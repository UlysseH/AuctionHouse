package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import java.sql.Timestamp
import java.util.Date

case class AuctionParams(
                          itemId: String,
                          floorPrice: Double,
                          incrementPolicy: String,
                          startDate: Timestamp,
                          endDate: Timestamp,
                          auctionId: String
                        )

object AuctionHouse {
  def props(): Props = Props(new AuctionHouse)

  //TODO: Caution not to create two or more Auction for the same item -> auctionId === itemId
  final case class GetAuctionHistory(auctionId: String)

  final case class RequestAuctioneer(auctioneerId: String)
  final case class RequestAuction(auctionParams: AuctionParams)

  case object AuctioneerRegistered
  case object AuctionRegistered

  final case class JoinBid(bidderId: String, itemId: String)
  final case class MakeOffer(bidderId: String, itemId: String, price: String)
}

class AuctionHouse extends Actor with ActorLogging {
  import AuctionHouse._

  var auctioneerIdToActor = Map.empty[String, ActorRef]
  var auctionToActor = Map.empty[AuctionParams, ActorRef]
  var actorToAuctioneerId = Map.empty[ActorRef, String]
  var actorToAuction = Map.empty[ActorRef, AuctionParams]

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

    case trackMsg @ RequestAuction(auctionParams) =>
      auctionToActor.get(auctionParams) match {
        case Some(ref) => ref forward trackMsg
        case None =>
          log.info("Creating new auction actor for item {}", auctionParams.itemId)
          val auctioneerActor = context.actorOf(Auction.props(auctionParams), "auction-" + auctionParams.itemId)
          auctionToActor += auctionParams -> auctioneerActor
          actorToAuction += auctioneerActor -> auctionParams
      }
  }
}