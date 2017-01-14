package com.bowlingx.providers

import java.net.URL
import javax.inject.{Inject, Provider}

import akka.actor.ActorSystem
import com.bowlingx.JavascriptEngine
import com.bowlingx.playframework.ScriptActionBuilder
import play.api.Environment
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext

case class ScriptResources(resources: Seq[URL])

class ScriptActionProvider(resources: ScriptResources) extends Provider[ScriptActionBuilder] {

  @Inject() implicit var executionContext: ExecutionContext = _
  @Inject() var actorSystem:ActorSystem = _
  @Inject() var lifecycle:ApplicationLifecycle = _
  @Inject() var env:Environment = _

  override def get(): ScriptActionBuilder = {
    new ScriptActionBuilder(new JavascriptEngine(resources, actorSystem, lifecycle, env.mode == play.api.Mode.Dev))
  }
}