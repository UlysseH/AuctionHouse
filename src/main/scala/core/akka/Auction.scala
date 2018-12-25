package core.akka

import java.sql.Timestamp
import java.util.Date

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, Props, Timers}
import core.akka.AuctionHouse.{NewAuction, RequestAuction}
//import core.akka.AuctionHouse.{AuctionHistory, UpdatedAuctionHistory}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

case class AuctionParams(
                          floorPrice: Double,
                          incrementPolicy: Float,
                          startDate: Timestamp,
                          endDate: Timestamp,
                        )

// Peut-être mieux en ajoutant une def actionSetup ?
object Auction {
  def props(auctionId: String): Props = Props(new Auction(auctionId))

  //received messages
  final case class NewBid(auctionId: String, bidderId: String, price: Float)
  final case class NewParams(auctionParams: AuctionParams)

  //sent messages
  final case class BidFailed(auctionId: String, bidderId: String)
  final case class BidSuccessful(auctionId: String, bidderId: String, price: Float, time: Long)

  //states
  sealed trait State

  case object Planned extends State
  case object Open extends State
  case object Closed extends State

  var state: State = Planned
  var params: Option[AuctionParams] = None
  //var paramsTimestamp:
  var currentPrice: Float = 0
  var auctionHistory = new ListBuffer[(String, Float, Long)]()
}

class Auction(auctionId: String) extends Actor with ActorLogging with Timers {
  import Auction._

  //timers.startSingleTimer()

  override def preStart(): Unit = log.info("Auction started")
  override def postStop(): Unit = log.info("Auction stopped")

  override def receive: Receive = {
    case RequestAuction(`auctionId`, auctionParams) =>
      params = Some(auctionParams)
      context.parent ! AuctionHouse.AuctionRegistered

    //case AuctionHouse.RequestAuction(`auctionId`) =>
    //  context.parent ! AuctionHouse.AuctioneerRegistered

    // TODO: clarify itemId <--> auctionId
    case RequestAuction(auctionId, _) =>
      log.warning(
        "Ignoring Auction request for {}. This actor is responsible for {}",
        auctionId, this.auctionId
      )

    //TODO : handle NewParams
    case NewParams(auctionParams) =>
      params = Some(auctionParams)

    case NewBid(`auctionId`, bidderId, price) =>
      val now = new Timestamp(new Date().getTime)
      state match {
      case Planned =>
        if (now.before(params.get.startDate)) {
          log.info("Too early to bid ! Auction starting at {}", params.get.startDate)
          context.parent ! BidFailed(auctionId, bidderId)
        }

        else if (now.after(params.get.endDate)) {
          log.info("Too late to bid ! Closing {}", auctionId)
          state = Closed
          context.parent ! BidFailed(auctionId, bidderId)
        }

        else if (price >= params.get.floorPrice) {
          state = Open
          currentPrice = price
          auctionHistory += Tuple3(bidderId, price, System.currentTimeMillis())
          log.info(
            "First Bid registered for auction-{} by bidder-{} with a price of {}",
            auctionId,
            bidderId,
            price
          )
          context.parent ! BidSuccessful(auctionId, bidderId, price, System.currentTimeMillis())
        }

        else if (price < params.get.floorPrice) {
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}",
            auctionId,
            bidderId,
            params.get.floorPrice
          )
        }

        else log.warning("Unpredicted behaviour for auction-{}", auctionId)


      case Open =>
        if (now.after(params.get.endDate)) {
          log.info("Too late to bid ! Closing {}", auctionId)
          state = Closed
          context.parent ! BidFailed(auctionId, bidderId)
        }

        //TODO: factoriser ?
        else if (price >= currentPrice + params.get.incrementPolicy) {
          currentPrice = price
          auctionHistory += Tuple3(bidderId, price, System.currentTimeMillis())
          log.info(
            "New Bid registered for auction-{} by bidder-{} with a price of {}",
            auctionId,
            bidderId,
            price
          )
          context.parent ! BidSuccessful(auctionId, bidderId, price, System.currentTimeMillis())
        }

        else if (price < currentPrice + params.get.incrementPolicy) {
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}",
            auctionId,
            bidderId,
            currentPrice + params.get.incrementPolicy
          )
        }
        else log.warning("Unpredicted behaviour for {}", auctionId)

      case Closed =>
        log.info("Too late to bid ! Closing {}", auctionId)
        context.parent ! BidFailed(auctionId, bidderId)
    }
  }
}