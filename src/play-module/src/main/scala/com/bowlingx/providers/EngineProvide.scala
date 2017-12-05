package com.bowlingx.providers

import java.net.URL
import javax.inject.{Inject, Provider}

import akka.actor.ActorSystem
import com.bowlingx.{Engine, JavascriptEngine}
import play.api.{Configuration, Environment}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

case class ScriptResources(resources: Seq[URL])

class EngineProvide(resources: ScriptResources) extends Provider[Engine] {

  @Inject() implicit var executionContext: ExecutionContext = _
  @Inject() var actorSystem: ActorSystem = _
  @Inject() var lifecycle: ApplicationLifecycle = _
  @Inject() var env: Environment = _
  @Inject() var config: Configuration = _

  override def get(): Engine = {
    val timeout = config.get[FiniteDuration]("webpack.rendering.timeout")
    val isWatchForcedDisabled = config.get[Boolean]("webpack.rendering.forceDisableWatch")
    val mode = env.mode match {
      case play.api.Mode.Dev => "dev"
      case play.api.Mode.Test => "test"
      case _ => "prod"
    }
    val renderInstances = config.get[Int](s"webpack.rendering.renderers.$mode")
    val watchMode = !isWatchForcedDisabled && env.mode == play.api.Mode.Dev
    new JavascriptEngine(
      resources, actorSystem, lifecycle, watchMode, timeout, renderInstances
    )
  }
}