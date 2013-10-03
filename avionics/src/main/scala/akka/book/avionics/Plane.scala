package akka.book.avionics

import akka.actor.{ Actor, ActorRef }
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala
import EventSource._

object Plane {
  // Returns the control surface to the Actor that asks for them
  case object GiveMeControl
  case class Controls(controls: ActorRef)
}

// We want the Plane to own the Altimeter and we're going to do that
// by passing in a specific factory we can use to build the Altimeter
class Plane extends Actor with ActorLogging {

  import Altimeter._
  import Plane._

  val cfgstr = "akka.book.avionics.flightcrew"
  val altimeter = context.actorOf(Props(Altimeter()), "Altimeter")
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")
  val config = context.system.settings.config
  val pilot = context.actorOf(Props[Pilot], config.getString(s"$cfgstr.pilotName"))
  val copilot = context.actorOf(Props[CoPilot], config.getString(s"$cfgstr.copilotName"))
  val autopilot = context.actorOf(Props[AutoPilot], "AutoPilot")
  val flightAttendant = context.actorOf(Props(LeadFlightAttendant()), 
      config.getString(s"$cfgstr.leadAttendantName"))
  
  override def preStart() {
    altimeter ! RegisterListener(self)
    List(pilot, copilot) foreach { _ ! Pilots.ReadyToGo }
  }

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control.")
      sender ! controls
    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }
}