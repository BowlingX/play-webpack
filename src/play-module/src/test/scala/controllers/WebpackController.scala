package controllers

import javax.inject._

import com.bowlingx.Engine
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

@Singleton
class WebpackController @Inject()
(engine: Engine, components: ControllerComponents)
(implicit context: ExecutionContext) extends AbstractController(components) {

  def index: Action[AnyContent] = Action.async {
    engine.render("render") map {
      case Success(Some(renderResult)) => Ok(renderResult.toString)
      case Failure(ex) => throw ex
      case _ => NotFound
    }
  }

  def asyncRenderedJs: Action[AnyContent] = Action.async {
    engine.render("renderPromise") map {
      case Success(Some(renderResult)) => Ok(renderResult.toString)
      case Failure(ex) => throw ex
      case _ => NotFound
    }
  }
}
