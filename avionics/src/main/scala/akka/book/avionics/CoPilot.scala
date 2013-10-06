package akka.book.avionics

import akka.actor.{ Actor, ActorRef }
import akka.actor.Terminated

class CoPilot(plane: ActorRef,
  autopilot: ActorRef,
  altimeter: ActorRef) extends Actor {

  import Pilots._

  var controls = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var pilotName = context.system.settings.config.getString(
    "akka.book.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      context.watch(pilot)
    case Terminated(_) =>
      // Pilot died
      plane ! Plane.GiveMeControl
    case Plane.Controls(controlSurfaces) =>
      controls = controlSurfaces
  }

}