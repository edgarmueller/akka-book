package akka.book.avionics

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.WordSpec
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestProbe
import akka.actor.ActorRef
import akka.actor.Actor

class ActorsSpec  extends TestKit(ActorSystem("ActorsSpec"))
	              with WordSpec {

  class AnnoyingActor(snooper: ActorRef) extends Actor {
    
    override def preStart() {
      self ! 'send
    }
    
    def receive = {
      case 'send =>
        snooper ! "Hello!!!"
        self ! 'send
    }
  }
  
  class NiceActor(snooper: ActorRef) extends Actor {
    
    override def preStart() {
      snooper ! "Hi"
    }
    
    def receive = {
      case _ => 
    }
  }
  
  "The AnnoyingActor" should {
    "say Hello!!!" in {
      val p = TestProbe()
      val a = system.actorOf(Props(new AnnoyingActor(p.ref)))
      // We're expecting the message on the uniqe TestProbe, 
      // not the general testActor
      p.expectMsg("Hello!!!")
      system.stop(a)
    }
  }
  
  "The NiceActor" should {
    "say Hi" in {
      val a = system.actorOf(Props(new NiceActor(testActor)))
      expectMsg("Hi")
    }
  }
}