package akka.book.avionics

import akka.actor.Actor
import akka.actor.ActorRef

object Pilots {
  case object ReadyToGo
  case object RelinquishControls
}

class Pilot extends Actor {

  import Pilots._
  import Plane._
  
  var controls: ActorRef = context.system.deadLetters
  var copilot:  ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  var copilotName = context.system.settings.config.getString(
      "akka.book.avionics.flightcrew.copilotName")
      
  def receive = {
    case ReadyToGo =>
      context.parent ! Plane.GiveMeControl
      copilot = context.actorFor("../" + copilotName)
      autopilot = context.actorFor("../AutoPilot")
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }      
}