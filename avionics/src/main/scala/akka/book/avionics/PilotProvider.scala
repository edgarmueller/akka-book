package akka.book.avionics

import akka.actor.Actor

trait PilotProvider {
  def pilot: Actor = new Pilot
  def copilot: Actor = new Pilot
  def autopilot: Actor = new Pilot
}