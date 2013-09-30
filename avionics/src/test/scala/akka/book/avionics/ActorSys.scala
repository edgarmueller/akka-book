package akka.book.avionics

import java.util.concurrent.atomic.AtomicInteger
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.actor.ActorSystem

object ActorSys {
  val uniqueId = new AtomicInteger(0)
}

class ActorSys(name: String) extends TestKit(ActorSystem(name))
                             with ImplicitSender
                             with DelayedInit {

  def this() = this("TestSystem%05d".format(
      ActorSys.uniqueId.getAndIncrement()))
  
  def shutdown(): Unit = system.shutdown
  
  def delayedInit(f: => Unit): Unit = {
    try {
      f
    } finally {
      shutdown()
    }
  }
  
}