package akka.book.avionics

import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import akka.testkit.ImplicitSender
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import akka.actor.ActorRef
import akka.util.Timeout
import akka.actor.Props
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.book.avionics.supervisors.IsolatedStopSupervisor
import akka.book.avionics.supervisors.OneForOneStrategyFactory
import akka.testkit.TestProbe
import akka.book.avionics.supervisors.IsolatedLifeCycleSupervisor
import akka.book.avionics.supervisors.IsolatedResumeSupervisor
import akka.actor.PoisonPill
import akka.actor.Actor

object AutoPilotSpec {
  val pilotName = "Mark"
  val coPilotName = "Mary"
  val configStr = s"""
    akka.book.avionics.flightcrew.copilotName = "$coPilotName"
    akka.book.avionics.flightcrew.pilotName = "$pilotName"
    """
}

class AutoPilotSpec extends TestKit(ActorSystem("AutoPilotSpec",
  ConfigFactory.parseString(AutoPilotSpec.configStr)))

  with ImplicitSender
  with WordSpec
  with MustMatchers {

  import AutoPilotSpec._
  import Plane._

  class FakeCoPilot extends Actor {
    override def receive = {
      case _ =>
    }
  }

  val coPilotPath = s"/user/TestPilots/$coPilotName"
  val autoPilotPath = s"/user/TestEquipment/AutoPilot"

  def nilActor: ActorRef = TestProbe().ref
  
  def pilotsReadyToGo(): ActorRef = {
    // The 'ask' below needs a timeout value
    implicit val askTimeout = Timeout(4.seconds)

    val a = system.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          context.actorOf(
            Props(new FakeCoPilot()), coPilotName)
        }
      }), "TestPilots")
    val b = system.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          context.actorOf(
            Props(new AutoPilot(testActor)), "AutoPilot")
        }
      }), "TestEquipment")
    // Wait for the mailboxes to be up and running for the
    // children
    Await.result(
      a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)
    Await.result(
      b ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)

    system.actorFor(autoPilotPath) ! Pilots.ReadyToGo
    expectMsg(RequestCoPilot)
    system.actorFor(autoPilotPath).tell(CoPilotReference(system.actorFor(coPilotName)), testActor)
    b
  }

  "AutoPilot" should {
    "take control when the CoPilot dies" in {
      pilotsReadyToGo()
      // Kill the Pilot
      system.actorFor(coPilotPath) ! PoisonPill
      // Since the test class is the "Plane" we can
      // expect to see this request
      expectMsg(GiveMeControl)
      // The girl who sent it had better be Mary
      lastSender must be(system.actorFor(autoPilotPath))
    }
  }
}