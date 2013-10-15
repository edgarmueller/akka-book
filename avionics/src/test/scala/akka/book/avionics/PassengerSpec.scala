package akka.book.avionics

import scala.concurrent.duration._
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

trait TestDrinkRequestProbability
  extends DrinkRequestProbability {

  override val askThreshold = 0f
  override val requestMin = 0.millis
  override val requestUpper = 2.millis

}

class PassengerSpec extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpec
  with MustMatchers {

  import akka.book.avionics.Passenger._
  import akka.event.Logging.Info
  import akka.testkit.TestProbe

  var seatNumber = 9

  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(new Passenger(testActor) with TestDrinkRequestProbability),
      s"Pat_Metheny-$seatNumber-B")
  }

  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF() {
        case Info(_, _, m) =>
          m.toString must include(" fastening seatbelt")
      }
    }
  }
}