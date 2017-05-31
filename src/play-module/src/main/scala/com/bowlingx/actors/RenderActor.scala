package com.bowlingx.actors

import java.util.function.Consumer
import javax.script.{CompiledScript, Invocable, ScriptContext, SimpleScriptContext}

import akka.actor.{Actor, Cancellable, PoisonPill, Props}
import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}
import play.api.Logger

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

case class Render[T](method: String, args: List[T])
case class Answer(answer: Try[Option[AnyRef]])
case class UpdatedScript(compiledScript: CompiledScript)

class RenderActor(compiledScript: CompiledScript, timeout:FiniteDuration) extends Actor {

  implicit private val thisContext = context.system.dispatcher
  private var scriptContext : ScriptContext = createScriptContext(compiledScript)

  private val cancels = collection.mutable.ArrayBuffer[(Cancellable, Promise[Boolean], Future[Boolean])]()
  private val timeoutExecutor = context.actorOf(Props[ExecutorActor])

  private lazy val setTimeout = (function: JSObject, delay: Int) => {
    val promise = Promise[Boolean]()
    val cancelable = context.system.scheduler.scheduleOnce(delay.milliseconds) {
      timeoutExecutor ! (function -> promise)
      ()
    }
    (cancels += Tuple3(cancelable, promise, promise.future)).size
  }: Int

  private lazy val logger: Logger = Logger(this.getClass)

  private lazy val clearTimeout = (timer: Int) => {
    val (cancel, promise, _) = cancels(timer - 1)
    cancel.cancel()
    promise.success(false)
  }

  private def createScriptContext(compiledScript: CompiledScript) = {
    val context = new SimpleScriptContext()
    context.setBindings(compiledScript.getEngine.createBindings(), ScriptContext.ENGINE_SCOPE)
    context.setAttribute("__play_webpack_logger", logger.logger, ScriptContext.ENGINE_SCOPE)
    context.setAttribute("__play_webpack_setTimeout", setTimeout, ScriptContext.ENGINE_SCOPE)
    context.setAttribute("__play_webpack_clearTimeout", clearTimeout, ScriptContext.ENGINE_SCOPE)

    compiledScript.eval(context)

    context
  }

  override def receive: Receive = {
    case Render(method, args) =>
      val result = Try {
        val global = scriptContext.getAttribute("window")
        compiledScript.getEngine.asInstanceOf[Invocable]
          .invokeMethod(global, method, args.map(_.asInstanceOf[Object]): _*)
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

      val response = Future.sequence(cancels.map {
        case (_, _, future) => future
      }).flatMap(_ => result match {
        case Success(future) => future.map(r => Success(Option(r)))
        case Failure(any) => Future(Failure(any))
      })
      response onComplete { _ =>
        cancels.clear()
      }

      // the actor has to block for the result to prevent more executions
      // in the same context of the script engine
      val awaitedResult = Await.result(response, timeout)
      sender ! Answer(awaitedResult)


    case UpdatedScript(script) =>
      scriptContext = createScriptContext(script)

  }

  override def postStop(): Unit = {
    timeoutExecutor ! PoisonPill
  }
}
