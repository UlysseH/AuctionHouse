package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import java.sql.Timestamp
import java.util.Date

import akka.util.Timeout
import core.akka.AuctionActor.{BidSuccessful, NewBid, SetValues}
import core.akka.BidderManager.{AuctionJoined, JoinAuction}

// Get the implicit ExecutionContext from this import
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._


import scala.Tuple3
//utiliser collection.parallel ?
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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

  //TODO: Caution not to create two or more Auction for the same item -> auctionId === itemId
  final case class GetAuctionHistory(auctionId: String)

  //final case class RequestAuction(auctionId: String, auctionParams: AuctionParams)

  final case class CreateAuction(auction: Auction)

  final case class UpdateAuction(auction: Auction)

  final case class ActionPerformed(description: String)

  final case class Bid(auctionId: String, price: Float)

  //TODO: move to Auction Actor
  final case class SuccessfulBid(bidderId: String, price: Float, time: Timestamp)

  final case object GetAuctions

  final case class GetAuction(auctionId: String)

  private implicit lazy val timeout: Timeout = Timeout(5.seconds)

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

  override def preStart(): Unit = log.info("Auction House started")
  override def postStop(): Unit = log.info("Auction House stopped")

  override def receive: Receive = {
    //case NewParams

    case GetAuction(auctionId) =>
      auctionIdToActor.get(auctionId) match {
        case Some(ref) => sender() ! ref
        case None =>
          println(auctionId)
          sender() ! ActionPerformed(s"Auction $auctionId does not exists")
      }

    case GetAuctions =>
      sender() ! Auctions(auctions.toSeq)

    case UpdateAuction(auction) =>
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
                (ref ? SetValues(
                  auction.floorPrice,
                  auction.incrementPolicy,
                  auction.startDate,
                  auction.endDate
                )) pipeTo sender()
              }
              else {
                sender() ! ActionPerformed("Auction already started. Too late to change parameters !")
                log.warning("Auction already started. Too late to change parameters !")
              }
          }

        case None =>
          log.info("Creating new auction actor for item {}", auctionId)
          val auctionActor = context.actorOf(AuctionActor.props(auction), "auction-" + auctionId)
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

    case CreateAuction(auction) =>
      val auctionId = auction.itemId
      auctionIdToActor.get(auctionId) match {
        case Some(_) =>
          sender() ! ActionPerformed(s"Auction already exists !")
        case None =>
          log.info("Creating new auction actor for item {}", auctionId)
          val auctionActor = context.actorOf(AuctionActor.props(auction), "auction-" + auctionId)
          auctions += auction
          auctionIdToActor += auctionId -> auctionActor
          actorToAuction += auctionActor -> auctionId
          //(auctionActor ? SetValues(
          //  auction.floorPrice,
          //  auction.incrementPolicy,
          //  auction.startDate,
          //  auction.endDate
          //)) pipeTo sender()
          sender() ! ActionPerformed("Auction created")
      }

    case msg @ GetAuctionHistory(auctionId) => {
      auctionIdToActor.get(auctionId) match {
        case Some(ref) =>
          (ref ? msg) pipeTo sender()
        case None =>
          log.warning("Auction [{}] does not exist", auctionId)
          sender() ! None
      }
    }

    //TODO: Handle BidSuccessful
    //case BidSuccessful(auctionId, bidderId, price, time) => history.get(auctionId) match {
    //  case None => history += auctionId -> new ListBuffer[Bid](). += (Bid(bidderId, price, time))
    //  case Some(l) => history(auctionId) -> l. +=(Bid(bidderId, price, time))

    //}
    //case UpdatedAuctionHistory(auctionId, auctionHistory) => history.get(auctionId) match {
    //  case None
    //}
    //  history auctionHistory
    //  println(auctionHistory)

    case trackMsg @ NewBid(auctionId, _, _) =>
      auctionIdToActor(auctionId) forward trackMsg
  }
}