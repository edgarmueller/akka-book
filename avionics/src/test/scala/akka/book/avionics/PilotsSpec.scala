package akka.book.avionics

import akka.actor.{ ActorSystem, Actor, ActorRef, Props, PoisonPill }
import akka.pattern.ask
import akka.testkit.{ TestKit, ImplicitSender, TestProbe }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.book.avionics.supervisors.IsolatedStopSupervisor
import akka.book.avionics.supervisors.OneForOneStrategyFactory
import akka.book.avionics.supervisors.IsolatedLifeCycleSupervisor
import org.scalatest.BeforeAndAfterAll
import akka.book.avionics.supervisors.IsolatedResumeSupervisor
import org.scalatest.BeforeAndAfterEach
import akka.actor.Kill

class FakePilot extends Actor {
	override def receive = {
	case _ =>
	}
}

object PilotsSpec {
  val pilotName = "Mark"
  val coPilotName = "Mary"
  val configStr = s"""
    akka.book.avionics.flightcrew.copilotName = "$coPilotName"
    akka.book.avionics.flightcrew.pilotName = "$pilotName"
    """
}

class PilotsSpec extends TestKit(ActorSystem("PilotsSpec",
  ConfigFactory.parseString(PilotsSpec.configStr)))

  with ImplicitSender
  with WordSpec
  with MustMatchers {

  import PilotsSpec._
  import Plane._
  

  // We're going to care an "empty" Actor as a TestProbe
  // instance Calling it nilActor just makes it really clear
  // what we mean
  def nilActor: ActorRef = TestProbe().ref

  // These paths are going to prove useful
  val pilotPath = s"/user/TestPilots/$pilotName"
  val coPilotPath = s"/user/TestPilots/$coPilotName"

  def pilotsReadyToGo(): ActorRef = {
    // The 'ask' below needs a timeout value
    implicit val askTimeout = Timeout(4.seconds)

    // Much like the creation we're using in the Plane
    val a = system.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          context.actorOf(
            Props[FakePilot], pilotName)
          context.actorOf(
            Props(new CoPilot(testActor, nilActor,
              nilActor)), coPilotName)
        }
      }), "TestPilots")
    // Wait for the mailboxes to be up and running for the
    // children
    Await.result(
      a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)
    // Tell the CoPilot that it's ready to go
    system.actorFor(coPilotPath) ! Pilots.ReadyToGo
    a
  }

  "CoPilot" should {
    "take control when the Pilot dies" in {
      pilotsReadyToGo()
      // Kill the Pilot
      system.actorFor(pilotPath) ! PoisonPill
      // Since the test class is the "Plane" we can
      // expect to see this request
      expectMsg(GiveMeControl)
      // The girl who sent it had better be Mary
      lastSender must be(system.actorFor(coPilotPath))
    }
  }
}