package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import java.sql.Timestamp
import java.util.Date

import core.akka.AuctionActor.{BidSuccessful, NewBid, SetValues}

import scala.Tuple3
//utiliser collection.parallel ?
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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

  //TODO: Caution not to create two or more Auction for the same item -> auctionId === itemId
  final case class GetAuctionHistory(auctionId: String)

  final case class RequestAuctioneer(auctioneerId: String)
  final case class RequestAuction(auctionId: String, auctionParams: AuctionParams)

  final case class NewAuction(auctionId: String, auctionParams: AuctionParams)

  final case class CreateAuction(auction: Auction)

  final case class ActionPerformed(description: String)

  case object AuctioneerRegistered

  final case class Bid(bidderId: String, price: Double, timestamp: Long)

  final case class AuctionHistory(auctionId: String, history: ListBuffer[Bid])

  final case object GetAuctions

  //final case class UpdatedAuctionHistory(auctionId: String, history: Seq[(String, Float, Long)])

  //final case class JoinAuction(bidderId: String, auctionId: String)
}

class AuctionHouse extends Actor with ActorLogging {
  import AuctionHouse._

  var auctioneerIdToActor = Map.empty[String, ActorRef]
  var auctionIdToActor = Map.empty[String, ActorRef]
  var actorToAuctioneerId = Map.empty[ActorRef, String]
  var actorToAuction = Map.empty[ActorRef, String]

  var auctions = Set.empty[Auction]

  var history = mutable.Map.empty[String, ListBuffer[Bid]]

  override def preStart(): Unit = log.info("Auction House started")
  override def postStop(): Unit = log.info("Auction House stopped")

  override def receive: Receive = {
    //case NewParams

    case GetAuctions =>
      sender() ! Auctions(auctions.toSeq)

    case CreateAuction(auction) =>
      val auctionId = auction.itemId
      val now = new Timestamp(new Date().getTime)
      auctionIdToActor.get(auctionId) match {
        case Some(ref) =>
          auctions
            .map(a => a.itemId -> a.startDate)
            .toMap.get(auctionId)
          match {
            case None => log.warning("Critical issue with auction[{}]", auctionId)
            case Some(timestamp) =>
              if (now.before(timestamp)) {
                // Replacing former auction with the updated version
                auctions -= auctions.filter(a => a.itemId == auctionId).head
                auctions += auction
                ref ! SetValues(
                  auction.floorPrice,
                  auction.incrementPolicy,
                  auction.startDate,
                  auction.endDate
                )
                sender() ! ActionPerformed(s"Auction $auctionId updated.")
              }
              else {
                sender() ! ActionPerformed("Auction already started. Too late to change parameters !")
                log.warning("Auction already started. Too late to change parameters !")
              }
          }

          case None =>
            log.info("Creating new auction actor for item {}", auctionId)
            val auctionActor = context.actorOf(AuctionActor.props(auctionId), "auction-" + auctionId)
            auctions += auction
            auctionIdToActor += auctionId -> auctionActor
            actorToAuction += auctionActor -> auctionId
            auctionActor ! SetValues(
              auction.floorPrice,
              auction.incrementPolicy,
              auction.startDate,
              auction.endDate
            )
            sender() ! ActionPerformed(s"Auction $auctionId created.")
      }

    case GetAuctionHistory(auctionId) => {
      println(history.get(auctionId))
      history.get(auctionId)
    }

    //TODO: Handle BidSuccessful
    case BidSuccessful(auctionId, bidderId, price, time) => history.get(auctionId) match {
      case None => history += auctionId -> new ListBuffer[Bid](). += (Bid(bidderId, price, time))
      case Some(l) => history(auctionId) -> l. +=(Bid(bidderId, price, time))

    }
    //case UpdatedAuctionHistory(auctionId, auctionHistory) => history.get(auctionId) match {
    //  case None
    //}
    //  history auctionHistory
    //  println(auctionHistory)

    case trackMsg @ NewBid(auctionId, _, _) =>
      auctionIdToActor(auctionId) forward trackMsg
  }
}