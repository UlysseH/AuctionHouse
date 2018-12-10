package core.akka

import akka.actor.{ActorLogging, Actor, Props}

object Auction {
  def props(): Props = Props(new Auction)
}

class Auction extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Auction started")
  override def postStop(): Unit = log.info("Auction stoped")

  // No need to handle any messages
  override def receive: Receive = Actor.emptyBehavior
}