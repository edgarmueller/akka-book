package akka.book.avionics

import akka.util.Timeout
import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.actorRef2Scala

object Avionics extends App {
  
	// needed for '?' below
	import akka.pattern.ask
  
	implicit val timeout = Timeout(5.seconds)
	val system = ActorSystem("PlaneSimulation")
	val plane = system.actorOf(Props[Plane], "Plane")
 
	// Grab the controls
	val control = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef], 5.seconds)
		
	// Takeoff!
	system.scheduler.scheduleOnce(200.millis) {
		control ! ControlSurfaces.StickBack(1f)
	}

	// Level out
	system.scheduler.scheduleOnce(1.seconds) {
		control ! ControlSurfaces.StickBack(0f)
	}
	
	// Climb
	system.scheduler.scheduleOnce(3.seconds) {
		control ! ControlSurfaces.StickBack(0.5f)
	}
	
	// Level out
	system.scheduler.scheduleOnce(4.seconds) {
		control ! ControlSurfaces.StickBack(0f)
	}

	// Shut down
	system.scheduler.scheduleOnce(5.seconds) {
		system.shutdown()
	}
}