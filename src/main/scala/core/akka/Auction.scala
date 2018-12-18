package core.akka

import akka.actor.{ActorLogging, Actor, Props}

// Peut-être mieux en ajoutant une def actionSetup ?
object Auction {
  def props(auctionParams: AuctionParams): Props = Props(new Auction(auctionParams))
}

class Auction(auctionParams: AuctionParams) extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Auction started")
  override def postStop(): Unit = log.info("Auction stoped")

  override def receive: Receive = {
    case AuctionHouse.RequestAuction(`auctionParams`) =>
      sender() ! AuctionHouse.AuctioneerRegistered


    // TODO: clarify itemId <--> auctionId
    case AuctionHouse.RequestAuction(auctionParams) =>
      log.warning(
        "Ignoring Auction request for {}. This actor is responsible for {}",
        auctionParams.itemId, this.auctionParams.itemId
      )
  }
}