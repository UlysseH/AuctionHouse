package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import core.akka.AuctionActor.NewBid

import scala.concurrent.duration._
import core.akka.AuctionHouse.{ActionPerformed, SuccessfulBid}

// Get the implicit ExecutionContext from this import
import scala.concurrent.ExecutionContext.Implicits.global

final case class Bidder(bidderId: String)
final case class Bidders(bidders: Seq[Bidder])

object BidderManager {
  def props(): Props = Props(new BidderManager)

  final case class RequestBidder(bidderId: String)

  final case class CreateBidder(bidder: Bidder)

  final case class AuctionJoined(auctionId: String, bidderId: String)

  final case object GetBidders

  final case class GetBidder(bidderId: String)

  final case class JoinAuction(bidderId: String, auctionId: String, auctionActor: ActorRef)

  case object BidderRegistered

  final case class BidderRelatedAuctionStatus(message: String)

  final case class BidderAuctionHistory(
                                                status: BidderRelatedAuctionStatus,
                                                auction: Auction,
                                                history: Seq[SuccessfulBid]
                                              )

  final case class BidderAuctionHouseHistory(history: Seq[BidderAuctionHistory])

  final case class GetAuctionHouseHistory(bidderId: String)

  private implicit lazy val timeout: Timeout = Timeout(5.seconds)
}

class BidderManager extends Actor with ActorLogging {
  import BidderManager._

  var bidderIdToActor = Map.empty[String, ActorRef]
  var actorToBidderId = Map.empty[ActorRef, String]
  var bidders = Set.empty[Bidder]


  override def preStart(): Unit = log.info("Bidder started")
  override def postStop(): Unit = log.info("Bidder stopped")

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
          sender() ! ActionPerformed(s"Bidder $id created.")
      }

    case GetBidders =>
      sender() ! Bidders(bidders.toSeq)

    case GetBidder(bidderId) =>
      bidderIdToActor.get(bidderId) match {
        case Some(ref) => sender() ! ref
        case None => log.warning(s"Bidder {} does not exists", bidderId)
      }

    case msg @ JoinAuction(bidderId, _, _) =>
      bidderIdToActor.get(bidderId) match {
        case Some(ref) =>
          (ref ? msg) pipeTo sender()
        case None => sender() ! ActionPerformed(s"No action performed. Bidder $bidderId does not exists")
      }

    case msg @ NewBid(_, bidderId, _) =>
      bidderIdToActor.get(bidderId) match {
        case Some(ref) =>
          (ref ? msg) pipeTo sender()
        case None => sender() ! ActionPerformed(s"No action performed. Bidder $bidderId does not exists")
      }

    case msg @ GetAuctionHouseHistory(bidderId) =>
      bidderIdToActor.get(bidderId) match {
        case Some(ref) =>
          (ref ? msg) pipeTo sender()
        case None => log.warning("No action performed. Bidder [{}] does not exists", bidderId)
      }
  }
}
