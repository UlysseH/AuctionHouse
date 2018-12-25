package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import java.sql.Timestamp
import java.util.Date

import core.akka.Auction.{BidSuccessful, NewBid, NewParams}

import scala.Tuple3
//utiliser collection.parallel ?
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AuctionHouse {
  def props(): Props = Props(new AuctionHouse)

  //TODO: Caution not to create two or more Auction for the same item -> auctionId === itemId
  final case class GetAuctionHistory(auctionId: String)

  final case class RequestAuctioneer(auctioneerId: String)
  final case class RequestAuction(auctionId: String, auctionParams: AuctionParams)

  final case class NewAuction(auctionId: String, auctionParams: AuctionParams)

  case object AuctioneerRegistered
  case object AuctionRegistered

  final case class Bid(bidderId: String, price: Double, timestamp: Long)

  final case class AuctionHistory(auctionId: String, history: ListBuffer[Bid])

  //final case class UpdatedAuctionHistory(auctionId: String, history: Seq[(String, Float, Long)])

  //final case class JoinAuction(bidderId: String, auctionId: String)
}

class AuctionHouse extends Actor with ActorLogging {
  import AuctionHouse._

  var auctioneerIdToActor = Map.empty[String, ActorRef]
  var auctionIdToActor = Map.empty[String, ActorRef]
  var actorToAuctioneerId = Map.empty[ActorRef, String]
  var actorToAuction = Map.empty[ActorRef, String]

  //var auction

  var history = mutable.Map.empty[String, ListBuffer[Bid]]




  override def preStart(): Unit = log.info("Auction House started")
  override def postStop(): Unit = log.info("Auction House stopped")

  override def receive: Receive = {
    case trackMsg @ RequestAuctioneer(auctioneerId) =>
      auctioneerIdToActor.get(auctioneerId) match {
        case Some(ref) =>
          ref forward trackMsg
        case None =>
          log.info("Creating new auctioneer actor for {}", auctioneerId)
          val auctioneerActor = context.actorOf(Auctioneer.props(auctioneerId), "auctioneer-" + auctioneerId)
          auctioneerIdToActor += auctioneerId -> auctioneerActor
          actorToAuctioneerId += auctioneerActor -> auctioneerId
      }

    //case NewParams

    case NewAuction(auctionId, auctionParams) =>
      auctionIdToActor.get(auctionId) match {
        case Some(ref) =>
          ref ! RequestAuction(auctionId, auctionParams)
          //ref ! NewParams(auctionParams)
        case None =>
          log.info("Creating new auction actor for item {}", auctionId)
          val auctioneerActor = context.actorOf(Auction.props(auctionId), "auction-" + auctionId)
          auctionIdToActor += auctionId -> auctioneerActor
          actorToAuction += auctioneerActor -> auctionId
          auctioneerActor ! RequestAuction(auctionId, auctionParams)
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