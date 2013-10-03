package akka.book.avionics

import akka.actor.{Actor, ActorRef}

class CoPilot extends Actor {
  
  import Pilots._
  
  var controls:  ActorRef = context.system.deadLetters
  var pilot :    ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  var pilotName = context.system.settings.config.getString(
    "akka.book.avionics.flightcrew.pilotName")
    
  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      autopilot = context.actorFor("../AutoPilot")
  }      
          
}