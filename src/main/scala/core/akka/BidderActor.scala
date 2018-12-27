package core.akka

import akka.actor.{ActorLogging, Actor, Props}

//itemId -> Item case class ?
//case class Bid(bidderId: String, itemId: String, value: Double, bidId: String)

object BidderActor {
  def props(bidderId: String): Props = Props(new BidderActor(bidderId))

  final case class getActionHouseState(requestId: String)
  final case class joinAuction(requestId: String)
  //final case class placeBid(requestId: String, bid: Bid)
}

class BidderActor(bidderId: String) extends Actor with ActorLogging {
  import Bidder._

  override def preStart(): Unit = log.info("Bidder started")
  override def postStop(): Unit = log.info("Bidder stoped")

  // No need to handle any messages
  override def receive: Receive = {
    case BidderManager.RequestBidder(`bidderId`) =>
      sender() ! BidderManager.BidderRegistered

    case BidderManager.RequestBidder(bidderId) =>
      log.warning(
        "Ignoring Bidder request for {}. This actor is responsible for {}",
        bidderId, this.bidderId
      )




  }
}
