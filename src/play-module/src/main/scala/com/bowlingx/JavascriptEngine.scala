package com.bowlingx

import java.io.{ByteArrayInputStream, InputStreamReader, SequenceInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.util
import java.util.function.Consumer
import javax.script._

import akka.actor.{ActorSystem, Cancellable}
import com.bowlingx.providers.ScriptResources
import jdk.nashorn.api.scripting.{JSObject, NashornScriptEngineFactory, ScriptObjectMirror}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait Engine {

  /**
    * Delegates a rendering of something to a `method`
    *
    * @param method    name of the method to call
    * @param arguments arguments
    * @tparam T any argument type
    * @return
    */
  def render[T <: Any](method: String, arguments: T*): Future[Try[Option[AnyRef]]]

}

/**
  * Renders JavaScript with the nashorn engine.
  *
  * @param context     implicit execution context
  * @param vendorFiles vendor files that should be precompiled and executed before the rendering
  */
class JavascriptEngine(
                        vendorFiles: ScriptResources,
                        actorSystem: ActorSystem,
                        lifecycle: ApplicationLifecycle,
                        watchFiles: Boolean
                      )(implicit context: ExecutionContext) extends Engine {

  val logger = Logger(this.getClass)

  private[this] val engine = new NashornScriptEngineFactory().getScriptEngine

  @volatile
  private[this] var compiledScript = createCompiledScripts()

  if (watchFiles) {
    this.initScheduling()
  }

  private[this] def initScheduling(): Unit = {
    val watch = FileSystems.getDefault.newWatchService()

    val uniqueFolders = vendorFiles.resources.map(r => Paths.get(r.toURI).getParent).distinct
    val fileNamesAllowedToTriggerChange = vendorFiles.resources.map(r => Paths.get(r.toURI).getFileName.toString)

    val keys = uniqueFolders.map(
      r => r.register(watch,
        StandardWatchEventKinds.ENTRY_MODIFY
      )
    )
    val scheduler = actorSystem.scheduler.schedule(0.second, 0.second) {
      // it's possible that the watcher has been closed already, so we ignore any exceptions
      val thisKeyOption = Try(Some(watch.poll())).toOption.flatten
      thisKeyOption.foreach(thisKey => {
        keys.find(_ == thisKey).foreach { k =>
          val events = k.pollEvents().asScala
          events.foreach(event => {
            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
              val file = event.context().asInstanceOf[Path].toString
              if (fileNamesAllowedToTriggerChange.contains(file)) {
                logger.info(s"Bundle source file `$file` changed, recompiling...")
                compiledScript = createCompiledScripts()
              }
            }
          })
          k.reset()
        }
      })
    }

    lifecycle.addStopHook(() => Future {
      watch.close()
      scheduler.cancel()
    })
  }

  def createCompiledScripts(): CompiledScript = {
    engine.asInstanceOf[Compilable].compile(new InputStreamReader(new SequenceInputStream(
      util.Collections.enumeration((bootstrap +: vendorFiles.resources.map(_.openStream())).asJava)
    )))
  }

  /**
    * Creates an event loop based on akka scheduler
    *
    * @param scriptContext context to register global methods
    * @param callback      executor that receives the promises
    * @param context       execution context
    * @return
    */
  private[this] def createEventLoop(
                                     scriptContext: SimpleScriptContext,
                                     callback: (collection.mutable.ArrayBuffer[Future[Boolean]]) => Future[Try[Option[AnyRef]]])
                                   (implicit context: ExecutionContext)
  : Future[Try[Option[AnyRef]]] = {
    val promises = collection.mutable.ArrayBuffer[Future[Boolean]]()
    val cancels = collection.mutable.ArrayBuffer[(Cancellable, Promise[Boolean])]()

    val setTimeout = (script: JSObject, delay: Int) => {
      val promise = Promise[Boolean]()
      val cancelable = actorSystem.scheduler.scheduleOnce(delay.milliseconds) {
        script.call(null) // scalastyle:ignore
        promise.success(true)
        ()
      }
      promises += promise.future
      (cancels += cancelable -> promise).size
    }: Int

    val clearTimeout = (timer: Int) => {
      val (cancel, promise) = cancels(timer - 1)
      cancel.cancel()
      promise.success(false)
    }

    scriptContext.setAttribute("__setTimeout", setTimeout, ScriptContext.ENGINE_SCOPE)
    scriptContext.setAttribute("__clearTimeout", clearTimeout, ScriptContext.ENGINE_SCOPE)

    callback(promises)
  }

  def render[T <: Any](method: String, arguments: T*): Future[Try[Option[AnyRef]]] = {
    // Make sure we work in a different context to prevent issues with other threads
    val scriptContext = new SimpleScriptContext()
    scriptContext.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE)
    compiledScript.eval(scriptContext)
    createEventLoop(scriptContext, promises => {
      val function = Option(scriptContext.getAttribute(method, ScriptContext.ENGINE_SCOPE).asInstanceOf[JSObject])
      function match {
        case Some(fn) =>
          val result = Try {
            Option(fn.call(null, arguments.map(_.asInstanceOf[Object]): _*)) map {  // scalastyle:ignore
              case result: ScriptObjectMirror if result.hasMember("then") =>
                val promise = Promise[AnyRef]()
                result.callMember("then", new Consumer[AnyRef] {
                  override def accept(t: AnyRef): Unit = {
                    promise.success(t)
                    ()
                  }
                }, new Consumer[AnyRef] {
                  override def accept(t: AnyRef): Unit = {
                    promise.failure(new RuntimeException(s"Promise failed with message: '${t.toString}'"))
                    ()
                  }
                })
                promise.future
              case anyResult => Future(anyResult)
            }
          }
          Future.sequence(promises).flatMap(_ => result match {
            case Success(Some(future)) => future.map(r => Success(Some(r)))
            case Success(None) => Future(Success(None))
            case Failure(any) => Future(Failure(any))
          })

        case _ => Future {
          Failure(new RuntimeException(s"Could not find method `$method` in current context."))
        }
      }
    })
  }

  /**
    * @return js to bootstrap the VM with
    */
  def bootstrap: ByteArrayInputStream = {
    val pre =
      """
        |var global = global || this, self = self || this, window = window || this;
        |var console = {};
        |console.debug = print;
        |console.warn = print;
        |console.error = print;
        |console.log = print;
        |console.trace = print;
        |
        |global.setTimeout = function(fn, delay) {
        |  return __setTimeout.apply(fn, delay);
        |};
        |
        |global.clearTimeout = function(timer) {
        |  return __clearTimeout.apply(timer);
        |};
        |
        |global.setImmediate = function(fn) {
        |  return __setTimeout.apply(fn, 0);
        |};
        |
        |global.clearImmediate = function(timer) {
        |  return __clearTimeout.apply(timer);
        |};
      """
        .stripMargin
    new ByteArrayInputStream(pre.getBytes(StandardCharsets.UTF_8))
  }
}