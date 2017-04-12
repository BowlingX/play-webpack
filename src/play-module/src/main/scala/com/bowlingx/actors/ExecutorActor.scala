package com.bowlingx.actors

import akka.actor.Actor
import jdk.nashorn.api.scripting.JSObject

import scala.concurrent.Promise

/**
  * Actor that handles our schedules (setTimeout) messages
  */
class ExecutorActor extends Actor {
  override def receive: Receive = {
    case ((function: JSObject, promise: Promise[_])) =>
      function.call(null) // scalastyle:ignore
      promise.asInstanceOf[Promise[Boolean]].success(true)
      ()
  }
}
