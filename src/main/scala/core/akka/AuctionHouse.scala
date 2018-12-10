package core.akka

import akka.actor.{ActorLogging, Actor, Props}


object AuctionHouse {
  def props(): Props = Props(new AuctionHouse)
}

class AuctionHouse extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Auction House started")
  override def postStop(): Unit = log.info("AuctionHouse stoped")

  // No need to handle any messages
  override def receive: Receive = Actor.emptyBehavior
}