package akka.book.avionics

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated

class AutoPilot(plane: ActorRef) extends Actor {

  import Pilots._
  import Plane._

  var controls = context.system.deadLetters
  
  def receive = {
    case ReadyToGo =>
      plane ! RequestCoPilot
    case CoPilotReference(coPilot) =>
      context.watch(coPilot)
    case Terminated(_) =>
      plane ! GiveMeControl
    case Plane.Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}