import controllers.WebpackController
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Helpers._
import play.api.test._

class ControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  "WebpackController GET" should {

    "render a javascript method" in {
      val controller = app.injector.instanceOf[WebpackController]
      val home = controller.index().apply(FakeRequest())

      status(home) mustBe OK
      contentAsString(home) must include ("This is rendered in JS")
    }

    "render an async javascript method" in {
      val controller = app.injector.instanceOf[WebpackController]
      val home = controller.asyncRenderedJs().apply(FakeRequest())
      status(home) mustBe OK
      contentAsString(home) must include ("This is an async resolved String")
    }

  }
}
