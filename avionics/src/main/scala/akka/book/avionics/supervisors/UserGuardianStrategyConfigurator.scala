package akka.book.avionics.supervisors
import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategy._
import akka.actor.SupervisorStrategyConfigurator
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy

class UserGuardianStrategyConfigurator extends SupervisorStrategyConfigurator {

  def create(): SupervisorStrategy = {
    OneForOneStrategy() {
      case _ => Resume
    }
  }
}