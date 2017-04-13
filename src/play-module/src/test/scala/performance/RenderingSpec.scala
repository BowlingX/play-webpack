package performance

import akka.util.Timeout
import com.bowlingx.Engine
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by bowlingx on 13.04.17.
  */
class RenderingSpec extends PlaySpec with GuiceOneAppPerTest with FutureAwaits with DefaultAwaitTimeout {

  implicit override def defaultAwaitTimeout: Timeout = 5.minutes


  "Render a React application" should {
    "multiple times" in {

      val engine = app.injector.instanceOf[Engine]
      implicit val context = app.injector.instanceOf[ExecutionContext]
      val promises = Future.sequence(1 to 50 map { unit =>
        val start = System.nanoTime()
        engine.render("testPerformance") map { r =>
         System.nanoTime() - start) / 1000000
        }
      })

      val result = await(promises)
      result.size mustBe 50
    }
  }
}
