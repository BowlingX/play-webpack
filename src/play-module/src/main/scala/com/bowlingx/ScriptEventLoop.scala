package com.bowlingx

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.script.{ScriptContext, SimpleScriptContext}

import akka.actor.{ActorSystem, Cancellable, PoisonPill, Props}
import com.bowlingx.actors.ExecutorActor
import jdk.nashorn.api.scripting.JSObject

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

import scala.concurrent.duration._

/**
  * Provides an Event Loop based on akka scheduler
  */
trait ScriptEventLoop {

  val actorSystem: ActorSystem
  type FutureHolder = collection.mutable.ArrayBuffer[(Cancellable, Promise[Boolean], Future[Boolean])]

  /**
    * Creates an event loop based on akka scheduler
    *
    * @param scriptContext context to register global methods
    * @param callback      executor that receives the promises
    * @param context       execution context
    * @return
    */
  protected def createEventLoop(scriptContext: SimpleScriptContext,
                                callback: (FutureHolder) => Future[Try[Option[AnyRef]]])
                               (implicit context: ExecutionContext)
  : Future[Try[Option[AnyRef]]] = {
    val cancels = collection.mutable.ArrayBuffer[(Cancellable, Promise[Boolean], Future[Boolean])]()
    val timeoutExecutor = actorSystem.actorOf(Props[ExecutorActor])

    val setTimeout = (function: JSObject, delay: Int) => {
      val promise = Promise[Boolean]()
      val cancelable = actorSystem.scheduler.scheduleOnce(delay.milliseconds) {
        timeoutExecutor ! (function -> promise)
        ()
      }
      (cancels += Tuple3(cancelable, promise, promise.future)).size
    }: Int

    val clearTimeout = (timer: Int) => {
      val (cancel, promise, _) = cancels(timer - 1)
      cancel.cancel()
      promise.success(false)
    }

    scriptContext.setAttribute("__play_webpack_setTimeout", setTimeout, ScriptContext.ENGINE_SCOPE)
    scriptContext.setAttribute("__play_webpack_clearTimeout", clearTimeout, ScriptContext.ENGINE_SCOPE)

    val result = callback(cancels)

    result.onComplete(_ => {
      timeoutExecutor ! PoisonPill
    })

    result
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
}
