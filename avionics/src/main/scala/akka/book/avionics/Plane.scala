package akka.book.avionics

import akka.actor.{ Actor, ActorRef }
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala
import EventSource._
import akka.book.avionics.supervisors.IsolatedResumeSupervisor
import akka.book.avionics.supervisors.OneForOneStrategyFactory
import akka.book.avionics.supervisors.IsolatedLifeCycleSupervisor._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import akka.book.avionics.supervisors.IsolatedStopSupervisor
import akka.actor.OneForOneStrategy

object Plane {
  def apply() = new Plane with AltimeterProvider with HeadingIndicatorProvider with PilotProvider with LeadFlightAttendantProvider
  // Returns the control surface to the Actor that asks for them
  case object GiveMeControl
  case object LostControl
  case class Controls(controlSurfaces: ActorRef)
  case object RequestCoPilot
  case class CoPilotReference(coPilot: ActorRef)
}

// We want the Plane to own the Altimeter and we're going to do that
// by passing in a specific factory we can use to build the Altimeter
class Plane extends Actor with ActorLogging {

  this: AltimeterProvider with HeadingIndicatorProvider with PilotProvider with LeadFlightAttendantProvider =>

  import Plane._    
  import Altimeter._

  val cfgstr = "akka.book.avionics.flightcrew"
  val config = context.system.settings.config
  
  var pilotName = context.system.settings.config.getString(s"$cfgstr.pilotName")
  var copilotName = context.system.settings.config.getString(s"$cfgstr.copilotName")
  var leadAttendantName = context.system.settings.config.getString(s"$cfgstr.leadAttendantName")

  implicit val askTimeout = Timeout(1.second)

  def startEquipment() {
    val plane = self
    val controls = context.actorOf(Props(
      new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          val altimeter = context.actorOf(Props(newAltimeter), 
              "Altimeter")
          val heading = context.actorOf(Props(newHeadingIndicator), 
              "HeadingIndicator")
          // These children get implicitly added to the hierarchy
          context.actorOf(Props(newAutoPilot(plane)), "AutoPilot")
          context.actorOf(Props(new ControlSurfaces(plane, altimeter, heading)), 
              "ControlSurfaces")
        }
      }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }

  // Helps us look up Actors within the "Equipment" Supervisor
  def actorForControls(name: String) =
    context.actorFor("Equipment/" + name)

  def actorForPilots(name: String) =
    context.actorFor("Pilots/" + name)

  def startPeople() {
    val plane = self
    // Note how we depend on the Actor structure beneath
    // us here by using actorFor(). This should be
    // resilient to change, since we'll probably be the
    // ones making the changes
    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("AutoPilot")
    val altimeter = actorForControls("Altimeter")

    val people = context.actorOf(Props(
      new IsolatedStopSupervisor with OneForOneStrategyFactory {
        def childStarter() {
          // These children get implicitly added to the hierarchy
          context.actorOf(Props(
            newCoPilot(plane, autopilot, altimeter)), copilotName)
          context.actorOf(Props(
            newPilot(plane, autopilot, controls, altimeter)), pilotName)
        }
      }), "Pilots")
    // Use the default strategy here, which restarts indefinitely
    context.actorOf(Props(newFlightAttendant), config.getString(s"$cfgstr.leadAttendantName"))
    Await.result(people ? WaitForStart, 1.second)
  }

  override def preStart() {
    import EventSource.RegisterListener
    import Pilots.ReadyToGo
    // Get our children going. Order is important here.
    startEquipment()
    startPeople()
    // Bootstrap the system
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
    actorForControls("AutoPilot") ! ReadyToGo
  }

  def receive = {
    case GiveMeControl =>
      sender ! Controls(actorForControls("ControlSurfaces"))
    case RequestCoPilot =>
      sender ! CoPilotReference(actorForPilots(copilotName))
    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }
}