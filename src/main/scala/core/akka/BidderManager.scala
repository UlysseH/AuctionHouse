package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.{ask, pipe}

import scala.concurrent.duration._

// Get the implicit ExecutionContext from this import
import scala.concurrent.ExecutionContext.Implicits.global

final case class Bidder(bidderId: String)
final case class Bidders(bidders: Seq[Bidder])

object BidderManager {
  def props(): Props = Props(new BidderManager)

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
          sender() ! ActionPerformed("Bidder already exists !", false)
        case None =>
          log.info("Creating new bidder for {}", id)
          val bidderActor = context.actorOf(BidderActor.props(id), "bidder-" + id)
          bidders += bidder
          bidderIdToActor += id -> bidderActor
          actorToBidderId += bidderActor -> id
          sender() ! ActionPerformed(s"Bidder $id created.", true)
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
        case None => sender() ! ActionPerformed(s"No action performed. Bidder $bidderId does not exists.", false)
      }

    case msg @ NewBid(_, bidderId, _) =>
      bidderIdToActor.get(bidderId) match {
        case Some(ref) =>
          (ref ? msg) pipeTo sender()
        case None => sender() ! ActionPerformed(s"No action performed. Bidder $bidderId does not exists.", false)
      }

    case msg @ GetAuctionHouseHistory(bidderId) =>
      bidderIdToActor.get(bidderId) match {
        case Some(ref) =>
          (ref ? msg) pipeTo sender()
        case None => log.warning("No action performed. Bidder {} does not exists.", bidderId)
      }
  }
}
