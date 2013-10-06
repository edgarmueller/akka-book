package akka.book.avionics

import akka.actor.Actor
import akka.actor.ActorRef

trait PilotProvider {
  
  def newPilot(plane: ActorRef, 
               autopilot: ActorRef,
               controls: ActorRef,
               altimeter: ActorRef): Actor = new Pilot(plane, autopilot, controls, altimeter)
  
  def newCoPilot(plane: ActorRef, 
               autopilot: ActorRef,
               altimeter: ActorRef): Actor = new CoPilot(plane, autopilot, altimeter)
  
  def newAutoPilot(plane: ActorRef): Actor = new AutoPilot(plane)
}