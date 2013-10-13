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
import akka.testkit.TestFSMRef
import akka.book.avionics.behaviours.FlyingBehaviour
import akka.book.avionics.behaviours.FlyingBehaviour._
import akka.book.avionics.behaviours.DrinkingBehaviour._
import akka.book.avionics.HeadingIndicator._
import akka.book.avionics.Altimeter._
import akka.testkit.TestActorRef
import akka.book.avionics.behaviours.DrinkingBehaviour
import akka.book.avionics.behaviours.DrinkingProvider
import akka.book.avionics.behaviours.FlyingProvider
import akka.testkit.TestActor

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

  def fsm(plane: ActorRef = nilActor,
    heading: ActorRef = nilActor,
    altimeter: ActorRef = nilActor) = {
    TestFSMRef(new FlyingBehaviour(plane, heading, altimeter))
  }

  "FlyingBehaviour" should {
    "start in the Idle state and with Uninitialized data" in {
      val a = fsm()
      a.stateName must be(Idle)
      a.stateData must be(Uninitialized)
    }
  }

  //  "PreparingToFly state" should {
  //
  //    val target = new CourseTarget(1, 1.0f, 1000)
  //
  //    "stay in PreparingToFly state when only a " +
  //      "HeadingUpdate is received" in {
  //        val a = fsm()
  //        a ! Fly(target)
  //        a ! HeadingUpdate(20)
  //        a.stateName must be(PreparingToFly)
  //        val sd = a.stateData.asInstanceOf[FlightData]
  //        sd.status.altitude must be(-1)
  //        sd.status.heading must be(20)
  //      }
  //
  //    "move to Flying state when all parts are received" in {
  //      val a = fsm()
  //      a ! Fly(target)
  //      a ! HeadingUpdate(20)
  //      a ! AltitudeUpdate(20)
  //      a ! Controls(testActor)
  //      a.stateName must be(Flying)
  //      val sd = a.stateData.asInstanceOf[FlightData]
  //      sd.controls must be(testActor)
  //      sd.status.altitude must be(20)
  //      sd.status.heading must be(20)
  //    }
  //  }
  //
  //  "Transitioning to Flying state" should {
  //    "create the Adjustment timer" in {
  //      val a = fsm()
  //      a.setState(PreparingToFly)
  //      a.setState(Flying)
  //      a.timerActive_?("Adjustment") must be(true)
  //    }
  //  }

  "Pilot.becomeZaphod" should {

    import akka.book.avionics.Pilots._

    trait TestFlyingProvider extends FlyingProvider {
      override def newFlyingBehaviour(plane: ActorRef,
        heading: ActorRef,
        altimeter: ActorRef): Actor = {
        new Actor {
          override def receive = {
            case m: NewBankCalculator => testActor forward m
            case n: NewElevatorCalculator => testActor forward n
          }
        }
      }
    }

    def makePilot(): ActorRef = {
      val p = system.actorOf(Props(
        new Pilot(nilActor, nilActor, nilActor, nilActor) with DrinkingProvider with TestFlyingProvider))
      p ! ReadyToGo
      p
    }

    "send new zaphodCalcElevator and zaphodCalcAilerons " +
      "to FlyingBehaviour" in {
        //    	val a = fsm()
        //        a.setState(Flying)

        val p = makePilot()

        val flyer = system.actorFor(p.path + "/FlyingBehaviour")
        val target = new CourseTarget(1, 1.0f, 1000)
        flyer ! Fly(target)
        flyer ! HeadingUpdate(20)
        flyer ! AltitudeUpdate(20)
        flyer ! Controls(nilActor)

        p ! FeelingLikeZaphod
        expectMsgAllOf(
          NewElevatorCalculator(Pilot.zaphodCalcElevator),
          NewBankCalculator(Pilot.zaphodCalcAilerons))
      }

    "Pilot.becomeTipsy" should {
      "send new tipsyCalcElevator and tipsyCalcAilerons to " +
        "FlyingBehaviour" in {
          val ref = makePilot()

          val flyer = system.actorFor(ref.path + "/FlyingBehaviour")
          val target = new CourseTarget(1, 1.0f, 1000)
          flyer ! Fly(target)
          flyer ! HeadingUpdate(20)
          flyer ! AltitudeUpdate(20)
          flyer ! Controls(nilActor)

          ref ! FeelingTipsy
          expectMsgAllClassOf(classOf[NewElevatorCalculator],
            classOf[NewBankCalculator]) foreach { m =>
              m match {
                case NewElevatorCalculator(f) =>
                  f must be(Pilot.tipsyCalcElevator)
                case NewBankCalculator(f) =>
                  f must be(Pilot.tipsyCalcAilerons)
              }
            }
        }
    }
  }

}