package com.bowlingx

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import com.bowlingx.actors.{Answer, Render}
import com.bowlingx.providers.ScriptResources
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

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
                        watchFiles: Boolean,
                        val renderTimeout:FiniteDuration,
                        val renderInstances:Int
                      )(implicit context: ExecutionContext) extends Engine with EngineWatcher {

  if (watchFiles) {
    this.initScheduling()
  }

  /**
    * @return js to bootstrap the VM with
    */
  protected def bootstrap: ByteArrayInputStream = {
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
        |  return __play_webpack_setTimeout.apply(fn, delay || 0);
        |};
        |
        |global.clearTimeout = function(timer) {
        |  return __play_webpack_clearTimeout.apply(timer);
        |};
        |
        |global.setImmediate = function(fn) {
        |  return __play_webpack_setTimeout.apply(fn, 0);
        |};
        |
        |global.clearImmediate = function(timer) {
        |  return __play_webpack_clearTimeout.apply(timer);
        |};
      """
        .stripMargin
    new ByteArrayInputStream(pre.getBytes(StandardCharsets.UTF_8))
  }

  def render[T <: Any](method: String, arguments: T*): Future[Try[Option[AnyRef]]] = {
    implicit val timeout = Timeout(renderTimeout)

    renderer ? Render(method, arguments.toList) map {
      case Answer(response) => response
    }
  }
}