package akka.book.avionics.supervisors

import scala.concurrent.duration.Duration
import akka.actor.SupervisorStrategy._
import akka.actor.ActorInitializationException
import akka.actor.ActorKilledException

abstract class IsolatedStopSupervisor(maxNrRetries: Int = -1,
  withinTimeRange: Duration = Duration.Inf) extends IsolatedLifeCycleSupervisor {

  this: SupervisionStrategyFactory =>
    
  override val supervisorStrategy = makeStrategy(
    maxNrRetries, withinTimeRange) {
      // usage of case syntax for partial function
      // example: val fizz: PartialFunction[Int, String] = { case a if a % 3 == 0 => "Fizz" }
      // type Decider = PartialFunction[Throwable, Directive]
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case _: Exception => Stop
      case _ => Escalate
    }
}