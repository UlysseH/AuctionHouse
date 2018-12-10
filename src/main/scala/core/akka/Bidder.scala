package core.akka

import akka.actor.{ActorLogging, Actor, Props}

object Bidder {
  def props(): Props = Props(new Bidder)
}

class Bidder extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Bidder started")
  override def postStop(): Unit = log.info("Bidder stoped")

  // No need to handle any messages
  override def receive: Receive = Actor.emptyBehavior
}
