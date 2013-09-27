package akka.book.avionics

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala
import EventSource._

object Plane {
  // Returns the control surface to the Actor that asks for them
  case object GiveMeControl
}

// We want the Plane to own the Altimeter and we're going to do that
// by passing in a specific factory we can use to build the Altimeter
class Plane extends Actor with ActorLogging {
  
	import Altimeter._
	import Plane._
	
	val altimeter = context.actorOf(Props(Altimeter()), "Altimeter")
	val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")
	
	override def preStart() {
	  altimeter ! RegisterListener(self)
	}
	
	def receive = {
	  case GiveMeControl =>
	  	log.info("Plane giving control.")
	  	sender ! controls
	  case AltitudeUpdate(altitude) =>
	    log.info(s"Altitude is now: $altitude")
	}
}