package com.bowlingx.actors

import akka.actor.Actor
import jdk.nashorn.api.scripting.JSObject

/**
  * Actor that handles our schedules (setTimeout) messages
  */
class ExecutorActor extends Actor {
  override def receive: Receive = {
    case function: JSObject =>
      sender ! function.call(null) // scalastyle:ignore
      ()
  }
}
