package performance

import akka.util.Timeout
import com.bowlingx.Engine
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
/**
  * Created by bowlingx on 13.04.17.
  */
class RenderingSpec extends PlaySpec with GuiceOneAppPerTest with FutureAwaits with DefaultAwaitTimeout {

  implicit override def defaultAwaitTimeout: Timeout = 1.minutes


  "Render a React application" should {
    "multiple times" in {

      val engine = app.injector.instanceOf[Engine]
      implicit val context = app.injector.instanceOf[ExecutionContext]
      val promises = Future.sequence(1 to 50 map { _ =>
        engine.render("testPerformance") map { result =>
          result mustBe a[Success[_]]
        }
      })

      val result = await(promises)
      result.size mustBe 50
    }
  }
}
