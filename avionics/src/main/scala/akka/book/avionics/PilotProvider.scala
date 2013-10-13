package akka.book.avionics

import akka.actor.Actor
import akka.actor.ActorRef
import akka.book.avionics.behaviours.DrinkingBehaviour
import akka.book.avionics.behaviours._

trait PilotProvider {
  
  def newPilot(plane: ActorRef, 
               autopilot: ActorRef,
               heading: ActorRef,
               altimeter: ActorRef): Actor = {
    new Pilot(plane, autopilot, heading, altimeter) with FlyingProvider with DrinkingProvider   
  }
  	
  
  def newCoPilot(plane: ActorRef, 
               autopilot: ActorRef,
               altimeter: ActorRef): Actor = new CoPilot(plane, autopilot, altimeter)
  
  def newAutoPilot(plane: ActorRef): Actor = new AutoPilot(plane)
}