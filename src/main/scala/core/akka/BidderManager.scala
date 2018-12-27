package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.akka.AuctionHouse.ActionPerformed

final case class Bidder(bidderId: String)
final case class Bidders(bidders: Seq[Bidder])

object BidderManager {
  def props(): Props = Props(new BidderManager)

  final case class RequestBidder(bidderId: String)

  final case class CreateBidder(bidder: Bidder)
  final case object GetBidders

  case object BidderRegistered
}

class BidderManager extends Actor with ActorLogging {
  import BidderManager._

  var bidderIdToActor = Map.empty[String, ActorRef]
  var actorToBidderId = Map.empty[ActorRef, String]
  var bidders = Set.empty[Bidder]


  override def preStart(): Unit = log.info("Bidder started")
  override def postStop(): Unit = log.info("Bidder stoped")

  // No need to handle any messages
  override def receive: Receive = {
    case CreateBidder(bidder) =>
      val id = bidder.bidderId
      bidderIdToActor.get(id) match {
        case Some(ref) =>
          ref ! RequestBidder(id)
          sender() ! ActionPerformed("Bidder already exists !")
        case None =>
          log.info("Creating new bidder for {}", id)
          val bidderActor = context.actorOf(BidderActor.props(id), "bidder-" + id)
          bidders += bidder
          bidderIdToActor += id -> bidderActor
          actorToBidderId += bidderActor -> id
          sender() ! ActionPerformed(s"Auction $id created.")
      }

    case GetBidders =>
      sender() ! Bidders(bidders.toSeq)
  }
}
