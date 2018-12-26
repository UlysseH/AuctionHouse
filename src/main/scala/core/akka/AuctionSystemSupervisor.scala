package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.akka.BidderManager.RequestBidder

object AuctionSystemSupervisor {
  def props(): Props = Props(new AuctionSystemSupervisor)
  var auctionHouse: Option[ActorRef] = None
  var bidderManager: Option[ActorRef] = None
}

class AuctionSystemSupervisor extends Actor with ActorLogging {
  import AuctionSystemSupervisor._

  override def preStart(): Unit = {
    log.info("Auction System Application started")

    auctionHouse =
      Some(
        context
          .actorOf(AuctionHouse.props(), "auction-house"))

    bidderManager =
      Some(
        context
          .actorOf(BidderManager.props(), "bidder-manager")
      )
  }
  override def postStop(): Unit = log.info("Auction System Application  stopped")

  // No need to handle any messages
  override def receive= {
    case trackMsg @ AuctionHouse.RequestAuctioneer(_) =>  auctionHouse.get forward trackMsg
    case trackMsg @ AuctionHouse.RequestAuction(_, _) =>  auctionHouse.get forward trackMsg
    case trackMsg @ BidderManager.RequestBidder(_) => bidderManager.get forward trackMsg
    case trackMsg @ AuctionHouse.NewAuction(_, _) => auctionHouse.get forward trackMsg
    case trackMsg @ AuctionActor.NewBid(_, _, _) => auctionHouse.get forward trackMsg
    case trackMsg @ AuctionHouse.GetAuctionHistory(_) => auctionHouse.get forward trackMsg
  }

}