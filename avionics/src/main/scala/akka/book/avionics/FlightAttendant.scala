package akka.book.avionics

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Cancellable

// This trait allows us to create different Flight Attendants
// with different levels of responsiveness.
trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}

object FlightAttendant {
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)
  case class Assist(passenger: ActorRef)
  case object Busy_?
  case object Yes
  case object No
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 30000
  }
}

class FlightAttendant extends Actor { this: AttendantResponsiveness =>

  import FlightAttendant._

  // bring the execution context into implicit scope for the
  // scheduler below
  implicit val ec = context.dispatcher

  // An internal message we can use to signal that drink
  // delivery can take place
  case class DeliverDrink(drink: Drink)

  // Stores our timer, which is an instance of 'Cancellable'
  var pendingDelivery: Option[Cancellable] = None

  def scheduleDelivery(drinkname: String): Cancellable = {
    context.system.scheduler.scheduleOnce(
      responseDuration, self, DeliverDrink(Drink(drinkname)))
  }

  // If we have an injured passenger, then we need to
  // immediately assist them, by giving them the 'secret'
  // Magic Healing Potion that's available on all flights in
  // and out of Xanadu
  def assistInjuredPassenger: Receive = {
    case Assist(passenger) =>
      // It's an emergency... stop what we're doing and
      // assist NOW
      pendingDelivery foreach { _.cancel() }
      pendingDelivery = None
      passenger ! Drink("Magic Healing Potion")
  }

  // This general handler is responsible for servicing drink
  // requests when we're not busy servicing an existing
  // request
  def handleDrinkRequests: Receive = {
    case GetDrink(drinkname) =>
      pendingDelivery = Some(scheduleDelivery(drinkname))
      // Become something new
      context.become(assistInjuredPassenger orElse handleSpecificPerson(sender))
    case Busy_? =>
      sender ! No
  }

  // When we are already busy getting a drink for someone
  // then we move to this state
  def handleSpecificPerson(person: ActorRef): Receive = {
    // If the person asking us for a drink is the same
    // person we're currently handling then we'll cancel
    // what we were doing and service their new request  	
    case GetDrink(drinkname) if sender == person =>
      pendingDelivery foreach { _.cancel() }
      pendingDelivery = Some(scheduleDelivery(drinkname))

    // The only time we can get the DeliverDrink message is
    // when we're in this state
    case DeliverDrink(drink) =>
      person ! drink
      pendingDelivery = None
      // Become something new
      context.become(assistInjuredPassenger orElse handleDrinkRequests)

    // If we get another drink request when we're already
    // handling one then we put that back to our parent
    // (the LeadFlightAttendant)
    case m: GetDrink =>
      context.parent forward m

    case Busy_? =>
      sender ! Yes
  }

  def receive = assistInjuredPassenger orElse handleDrinkRequests
}