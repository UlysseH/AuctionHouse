package core.akka

import java.sql.Timestamp
import java.util.Date

import scala.concurrent.duration._
// Get the implicit ExecutionContext from this import
import scala.concurrent.ExecutionContext.Implicits.global

import akka.util.Timeout
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}

final case class AuctionId(auctionId: String)
final case class Auction(
                          itemId: String,
                          floorPrice: Double,
                          incrementPolicy: Float,
                          startDate: Timestamp,
                          endDate: Timestamp,
                        )
final case class Auctions(auctions: Seq[Auction])

object AuctionHouse {
  def props(): Props = Props(new AuctionHouse)

  private implicit lazy val timeout: Timeout = Timeout(5.seconds)
}

class AuctionHouse extends Actor with ActorLogging {
  import AuctionHouse._

  var auctioneerIdToActor = Map.empty[String, ActorRef]
  var auctionIdToActor = Map.empty[String, ActorRef]
  var actorToAuctioneerId = Map.empty[ActorRef, String]
  var actorToAuction = Map.empty[ActorRef, String]

  var auctions = Set.empty[Auction]

  override def preStart(): Unit = log.info("Auction House started")
  override def postStop(): Unit = log.info("Auction House stopped")

  override def receive: Receive = {
    case GetAuction(auctionId) =>
      sender() ! auctions.find(_.itemId == auctionId)

    case GetAuctionActor(auctionId) =>
      sender() ! auctionIdToActor.get(auctionId)

    case GetAuctions =>
      sender() ! Auctions(auctions.toSeq)

    case CreateAuction(auction) =>
      val auctionId = auction.itemId
      auctionIdToActor.get(auctionId) match {
        case Some(ref) =>
          val now = new Timestamp(new Date().getTime)
          if (auction.startDate.before(now)) {
            sender() ! ActionPerformed("Illegal start date : an auction must start in the future.", false)
          }
          else if (auction.startDate.after(auction.endDate)) {
            sender() ! ActionPerformed("Cannot update auction : end date is anterior to start.", false)
          }
          else {
            (ref ? UpdateAuction(auction)) pipeTo sender()
            auctions -= auctions.find(_.itemId == auctionId).get
            auctions += auction
          }

        case None =>
          val now = new Timestamp(new Date().getTime)
          if (auction.startDate.before(now)) {
            sender() ! ActionPerformed("Illegal start date : an auction must start in the future.", false)
          }
          else if (auction.startDate.after(auction.endDate)) {
            sender() ! ActionPerformed("Cannot create auction : end date is anterior to start.", false)
          }
          else {
            log.info("Creating new auction actor for item {}", auctionId)
            val auctionActor = context.actorOf(AuctionActor.props(auction), "auction-" + auctionId)
            auctions += auction
            auctionIdToActor += auctionId -> auctionActor
            actorToAuction += auctionActor -> auctionId
            sender() ! ActionPerformed(s"Auction $auctionId created.", true)
          }
      }

    case msg @ GetAuctionHistory(auctionId) => {
      auctionIdToActor.get(auctionId) match {
        case Some(ref) =>
          (ref ? msg) pipeTo sender()
        case None =>
          log.warning("Auction {} does not exist.", auctionId)
          sender() ! None
      }
    }

    case trackMsg @ NewBid(auctionId, _, _) =>
      auctionIdToActor(auctionId) forward trackMsg
  }
}