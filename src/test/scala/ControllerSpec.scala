import controllers.WebpackController
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._


class ControllerSpec extends PlaySpec with OneAppPerTest {

  "WebpackController GET" should {

    "render the index page from the application" in {
      val controller = app.injector.instanceOf[WebpackController]
      val home = controller.index().apply(FakeRequest())

      status(home) mustBe OK
    }

  }
}
