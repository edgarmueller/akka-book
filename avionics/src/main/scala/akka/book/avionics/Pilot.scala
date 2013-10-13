package akka.book.avionics

import akka.actor.Actor
import akka.actor.ActorRef
import akka.book.avionics.behaviours.FlyingProvider
import akka.book.avionics.behaviours.DrinkingProvider
import akka.actor.Props

object Pilots {
  case object ReadyToGo
  case object RelinquishControls
}

object Pilot {

  import akka.book.avionics.behaviours.FlyingBehaviour._
  import ControlSurfaces._

  // Calculates the elevator changes when we're a bit tipsy
  val tipsyCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickForward(amt * 1.03f)
      case StickBack(amt) => StickBack(amt * 1.03f)
      case m => m
    }
  }

  // Calculates the aileron changes when we're a bit tipsy
  val tipsyCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickLeft(amt * 1.03f)
      case StickRight(amt) => StickRight(amt * 1.03f)
      case m => m
    }
  }

  // Calculates the elevator changes when we're totally out of it
  val zaphodCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickBack(1f)
      case StickBack(amt) => StickForward(1f)
      case m => m
    }
  }

  // Calculates the aileron changes when we're totally out of it
  val zaphodCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickRight(1f)
      case StickRight(amt) => StickLeft(1f)
      case m => m
    }
  }
}

class Pilot(plane: ActorRef,
  autopilot: ActorRef,
  heading: ActorRef,
  altimeter: ActorRef) extends Actor {

  this: DrinkingProvider with FlyingProvider =>

  import Pilots._
  import Pilot._
  import Plane._
  import Altimeter._
  import ControlSurfaces._
  import akka.book.avionics.behaviours.DrinkingBehaviour._
  import akka.book.avionics.behaviours.FlyingBehaviour._
  import akka.actor.FSM._

  var copilotName = context.system.settings.config.getString(
    "akka.book.avionics.flightcrew.copilotName")

  def setCourse(flyer: ActorRef) {
    flyer ! Fly(CourseTarget(20000, 250,
      System.currentTimeMillis + 30000))
  }

  override def preStart() {
    // Create our children
    context.actorOf(Props(newDrinkingBehaviour(self)),
      "DrinkingBehaviour")
    val a = context.actorOf(Props(newFlyingBehaviour(plane, heading,
      altimeter)),
      "FlyingBehaviour")
  }

  // We've pulled the bootstrapping code out into a separate
  // receive method. We'll only ever be in this state once,
  // so there's no point in having it around for long
  def bootstrap: Receive = {
    case ReadyToGo =>
      println("ready to go")
      val copilot = context.actorFor("../" + copilotName)
      val flyer = context.actorFor("FlyingBehaviour")
      flyer ! SubscribeTransitionCallBack(self)
      setCourse(flyer)
      context.become(sober(copilot, flyer))
  }

  // The 'sober' behaviour
  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => // We're already sober
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  // The 'tipsy' behaviour
  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => // We're already tipsy
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  // The 'zaphod' behaviour
  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => // We're already Zaphod
  }

  // The 'idle' state is merely the state where the Pilot
  // does nothing at all
  def idle: Receive = {
    case _ =>
  }

  // Updates the FlyingBehaviour with sober calculations and
  // then becomes the sober behaviour
  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }

  // Updates the FlyingBehaviour with tipsy calculations and
  // then becomes the tipsy behaviour
  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }

  // Updates the FlyingBehaviour with zaphod calculations
  // and then becomes the zaphod behaviour
  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    println("zaphod!!")
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

  // Idle state, which means that our behavioural changes
  // don't matter any more
  override def unhandled(msg: Any): Unit = {
    msg match {
      case Transition(_, _, Flying) =>
        setCourse(sender)
      case Transition(_, _, Idle) =>
        context.become(idle)
      // Ignore these two messages from the FSM rather than
      // have them go to the log
      case Transition(_, _, _) =>
      case CurrentState(_, _) =>
      case m => super.unhandled(m)
    }
  }
  // Initially we start in the bootstrap state
  def receive = bootstrap
}