package akka.book.avionics

import akka.actor.Actor
import akka.actor.ActorRef

object Pilots {
  case object ReadyToGo
  case object RelinquishControls
}

class Pilot(plane: ActorRef, 
            autopilot: ActorRef,
            var controls: ActorRef,
            altimeter: ActorRef) extends Actor {

  import Pilots._
  
  var copilot: ActorRef = context.system.deadLetters
  var copilotName = context.system.settings.config.getString(
      "akka.book.avionics.flightcrew.copilotName")
      
  def receive = { 
    case ReadyToGo =>
      copilot = context.actorFor("../" + copilotName)
    case Plane.Controls(controlSurfaces) =>
      controls = controlSurfaces
  }      
}