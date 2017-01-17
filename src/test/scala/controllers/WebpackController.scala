package controllers

import javax.inject._

import com.bowlingx.playframework.ScriptActionBuilder
import play.api.mvc._

@Singleton
class WebpackController @Inject()(js: ScriptActionBuilder) extends Controller {

  def index: Action[AnyContent] = js.call("render") { request =>
    Ok(request.render.toString)
  }

}
