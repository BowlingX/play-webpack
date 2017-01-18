package com.bowlingx.playframework

import com.bowlingx.Engine
import play.api.mvc.{ActionBuilder, ActionTransformer, Request, WrappedRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ScriptRequest[A](val render: Any, request: Request[A]) extends WrappedRequest[A](request)

class ScriptActionBuilder(engine:Engine)(implicit context: ExecutionContext) {

  def call[T <: Any](method:String, args:T*) : ActionBuilder[ScriptRequest] = {
    object ScriptAction extends ActionBuilder[ScriptRequest] with ActionTransformer[Request, ScriptRequest] {

      def transform[A](request: Request[A]): Future[ScriptRequest[A]] = engine.render(method, args:_*).map {
          case Success(Some(result)) => new ScriptRequest(result, request)
          case Failure(ex) => throw new RuntimeException(ex)
          case Success(None) => new ScriptRequest("", request)
      }

    }
    ScriptAction
  }

}