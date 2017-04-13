package com.bowlingx

import java.util.function.Consumer
import javax.script._

import akka.actor.ActorSystem
import com.bowlingx.providers.ScriptResources
import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Renders JavaScript with the nashorn engine.
  *
  * @param context     implicit execution context
  * @param vendorFiles vendor files that should be precompiled and executed before the rendering
  */
class JavascriptEngine(
                        val vendorFiles: ScriptResources,
                        val actorSystem: ActorSystem,
                        val lifecycle: ApplicationLifecycle,
                        watchFiles: Boolean
                      )(implicit context: ExecutionContext) extends Engine with ScriptEventLoop with EngineWatcher {

  if (watchFiles) {
    this.initScheduling()
  }

  def render[T <: Any](method: String, arguments: T*): Future[Try[Option[AnyRef]]] = {
    // Make sure we work in a different context to prevent issues with other threads
    val scriptContext = new SimpleScriptContext()
    scriptContext.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE)
    createEventLoop(scriptContext, promises => {
      compiledScript.eval(scriptContext)
      val result = Try {
        val global = scriptContext.getAttribute("window")
        compiledScript.getEngine.asInstanceOf[Invocable].invokeMethod(global, method, arguments.map(_.asInstanceOf[Object]): _*)
      } map { // scalastyle:ignore
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
      Future.sequence(promises.map(_._3)).flatMap(_ => result match {
        case Success(future) => future.map(r => Success(Option(r)))
        case Failure(any) => Future(Failure(any))
      })

    })
  }
}