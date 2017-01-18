import controllers.WebpackController
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._

class ControllerSpec extends PlaySpec with OneAppPerTest {

  "WebpackController GET" should {

    "render a javascript method" in {
      val controller = app.injector.instanceOf[WebpackController]
      val home = controller.index().apply(FakeRequest())

      status(home) mustBe OK
      contentAsString(home) must include ("This is rendered in JS")
    }

  }
}
