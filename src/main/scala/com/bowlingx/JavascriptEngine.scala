package com.bowlingx

import java.io.{ByteArrayInputStream, InputStreamReader, SequenceInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.util
import javax.script._

import akka.actor.ActorSystem
import com.bowlingx.providers.ScriptResources
import jdk.nashorn.api.scripting.JSObject
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}
import collection.JavaConversions._
import scala.util.{Failure, Try}
import scala.concurrent.duration._

trait Engine {

  /**
    * Delegates a rendering of something to a `method`
    *
    * @param method name of the method to call
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

  private[this] val engine = new ScriptEngineManager(null).getEngineByName("nashorn") // scalastyle:ignore

  @volatile
  private[this] var compiledScript = createCompiledScripts()

  if (watchFiles) {
    this.initScheduling()
  }

  private[this] def initScheduling() {
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
          val events = k.pollEvents()
          events.foreach(event => {
            if(event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
              val file = event.context().asInstanceOf[Path].toString
              if(fileNamesAllowedToTriggerChange.contains(file)) {
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
      util.Collections.enumeration(bootstrap +: vendorFiles.resources.map(_.openStream()))
    )))
  }

  def render[T <: Any](method: String, arguments: T*): Future[Try[Option[AnyRef]]] = Future {
    // Make sure we work in a different context to prevent issues with other threads
    val context = new SimpleScriptContext()
    context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE)
    compiledScript.eval(context)

    val function = Option(context.getAttribute(method, ScriptContext.ENGINE_SCOPE).asInstanceOf[JSObject])

    function match {
      case Some(fn) => Try {
        Option(fn.call(null, arguments.map(_.asInstanceOf[Object]): _*)) // scalastyle:ignore
      }
      case _ => Failure(new RuntimeException(s"Could not find method `$method` in current context."))
    }
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
        |console.log = print;
      """
        .stripMargin
    new ByteArrayInputStream(pre.getBytes(StandardCharsets.UTF_8))
  }
}