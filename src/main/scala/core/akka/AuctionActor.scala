package core.akka

import java.sql.Timestamp
import java.util.Date

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, Props, Timers}
import core.akka.AuctionHouse.{ActionPerformed, CreateAuction, GetAuctionHistory, SuccessfulBid}
import core.akka.BidderManager.{BidderAuctionHistory, BidderRelatedAuctionStatus, GetAuctionHouseHistory}

import scala.collection.mutable
//import core.akka.AuctionHouse.{AuctionHistory, UpdatedAuctionHistory}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._



//case class AuctionParams(
//                          floorPrice: Double,
//                          incrementPolicy: Float,
//                          startDate: Timestamp,
//                          endDate: Timestamp,
//                        )

// Peut-être mieux en ajoutant une def actionSetup ?
object AuctionActor {
  def props(auction: Auction): Props = Props(new AuctionActor(auction))

  //received messages
  final case class NewBid(auctionId: String, bidderId: String, price: Float)
  final case class SetValues(floorPrice: Double, incrementPolicy: Float, startDate: Timestamp, endDate: Timestamp)

  //sent messages
  final case class BidFailed(auctionId: String, bidderId: String)
  final case class BidSuccessful(auctionId: String, bidderId: String, price: Float, time: Long)

  final case class AuctionHistory(history: Seq[SuccessfulBid])


  //states
  sealed trait State

  case object Planned extends State
  case object Open extends State
  case object Closed extends State
}

class AuctionActor(auction: Auction) extends Actor with ActorLogging with Timers {
  import AuctionActor._

  //timers.startSingleTimer()

  override def preStart(): Unit = log.info("Auction started")
  override def postStop(): Unit = log.info("Auction stopped")

  var state: State = Planned
  var currentPrice: Float = -1


  var bidders = Set.empty[Bidder]

  var auctionHistory = new ListBuffer[SuccessfulBid]()

  var floorPrice: Double = auction.floorPrice
  var incrementPolicy: Float = auction.incrementPolicy
  var startDate: Timestamp = auction.startDate
  var endDate: Timestamp = auction.endDate

  override def receive: Receive = {
    case GetAuctionHouseHistory(bidderId) =>
      val now = new Timestamp(new Date().getTime)

      val status =
        if (now.before(startDate)) "pending" else
        if (now.after(endDate)) "ended" else
        if (auctionHistory.reverse.head.bidderId == bidderId) "leading"
        else "not leading"

      sender() ! BidderAuctionHistory(
        BidderRelatedAuctionStatus(status),A
        auction,
        auctionHistory.reverse
      )

    case SetValues(price: Double, increment: Float, start: Timestamp, end: Timestamp) =>
      floorPrice = price
      incrementPolicy = increment
      startDate = start
      endDate = end
      val auctionId = auction.itemId
      sender() ! ActionPerformed(s"Auction $auctionId updated.")
    
    //case RequestAuction(`auctionId`, auctionParams) =>
    //  println(sender())
    //  params match {
    //    case None => log.info("Auction for {} initialized.", auctionId)
    //    case _ => log.info("Auction for {} updated.", auctionId)
    //  }
    //  params = Some(auctionParams)

    //case AuctionHouse.RequestAuction(`auctionId`) =>
    //  context.parent ! AuctionHouse.AuctioneerRegistered

    //case RequestAuction(auctionId, _) =>
    //  log.warning(
    //    "Ignoring Auction request for {}. This actor is responsible for {}",
    //    auctionId, this.auctionId
    //  )

    case GetAuctionHistory(_) =>
      sender() ! Some(AuctionHistory(auctionHistory))

    case NewBid(_, bidderId, price) =>
      val now = new Timestamp(new Date().getTime)
      val auctionId = auction.itemId
      state match {
      case Planned =>
        if (now.before(startDate)) {
          log.info("Too early to bid ! Auction starting at {}", startDate)
          //context.parent ! BidFailed(auctionId, bidderId)
          sender() ! ActionPerformed(s"Bid failed : too early too Bid ! Bid starting at [$startDate]")
        }

        else if (now.after(endDate)) {
          log.info("Too late to bid ! Closing {}", auctionId)
          state = Closed
          //context.parent ! BidFailed(auctionId, bidderId)
          sender() ! ActionPerformed(s"Bid failed : too late too Bid ! Bid closed at [$endDate")
        }

        else if (price >= floorPrice) {
          state = Open
          currentPrice = price
          auctionHistory += SuccessfulBid(bidderId, price, now)
          log.info(
            "First Bid registered for auction-{} by bidder-{} with a price of {}",
            auctionId,
            bidderId,
            price
          )
          sender() ! ActionPerformed(s"Bid successful for Bidder $bidderId, Auction $auctionId and a price of $price")
        }

        else if (price < floorPrice) {
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}",
            auctionId,
            bidderId,
            floorPrice
          )
          sender() ! ActionPerformed(s"Bid failed : Insufficient price. Minimum is [$floorPrice]")
        }

        else log.warning("Unpredicted behaviour for auction-{}", auctionId)


      case Open =>
        if (now.after(endDate)) {
          log.info("Too late to bid ! Closing {}", auctionId)
          state = Closed
          //context.parent ! BidFailed(auctionId, bidderId)
          sender() ! ActionPerformed(s"Bid failed : too late too Bid ! Bid closed at [$endDate")
        }

        //TODO: factoriser ?
        else if (price >= currentPrice + incrementPolicy) {
          currentPrice = price
          auctionHistory += SuccessfulBid(bidderId, price, now)
          log.info(
            "New Bid registered for auction-{} by bidder-{} with a price of {}",
            auctionId,
            bidderId,
            price
          )
          sender() ! ActionPerformed(s"Bid successful for Bidder $bidderId, Auction $auctionId and a price of $price")
          //context.parent ! BidSuccessful(auctionId, bidderId, price, System.currentTimeMillis())
        }

        else if (price < currentPrice + incrementPolicy) {
          val minimumPrice = currentPrice + incrementPolicy
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}",
            auctionId,
            bidderId,
            minimumPrice
          )
          sender() ! ActionPerformed(s"Bid failed : Insufficient price. Minimum is [$minimumPrice]")
        }
        else log.warning("Unpredicted behaviour for {}", auctionId)

      case Closed =>
        log.info("Too late to bid ! Closing {}", auctionId)
        //context.parent ! BidFailed(auctionId, bidderId)
        sender() ! ActionPerformed(s"Bid failed : too late too Bid ! Bid closed at [$endDate")
    }
  }
}