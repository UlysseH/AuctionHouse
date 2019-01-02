package core.akka

import java.sql.Timestamp
import java.util.Date

import scala.collection.mutable.ListBuffer
import akka.actor.{Actor, ActorLogging, Props, Timers}

object AuctionActor {
  def props(auction: Auction): Props = Props(new AuctionActor(auction))

  //states
  sealed trait State
  case object Planned extends State
  case object Open extends State
  case object Closed extends State
}

class AuctionActor(auction: Auction) extends Actor with ActorLogging with Timers {
  import AuctionActor._

  override def preStart(): Unit = log.info("Auction started")
  override def postStop(): Unit = log.info("Auction stopped")

  var state: State = Planned
  var currentPrice: Float = -1
  var bidders = Set.empty[Bidder]
  var auctionHistory = new ListBuffer[SuccessfulBid]()

  // Auction parameters
  var floorPrice: Double = auction.floorPrice
  var incrementPolicy: Float = auction.incrementPolicy
  var startDate: Timestamp = auction.startDate
  var endDate: Timestamp = auction.endDate

  override def receive: Receive = {
    case UpdateAuction(a) =>
      val now = new Timestamp(new Date().getTime)
      val id = a.itemId
      if (now.before(startDate)) {
        floorPrice = a.floorPrice
        incrementPolicy = a.incrementPolicy
        startDate = a.startDate
        endDate = a.endDate
        sender() ! ActionPerformed(s"Updated auction $id.", true)
      }
      else {
        sender() ! ActionPerformed(s"Cannot update auction $id since it has already started.", false)
      }

    case GetAuctionHouseHistory(bidderId) =>
      val now = new Timestamp(new Date().getTime)

      val status =
        if (now.before(startDate)) "pending" else
        if (now.after(endDate)) "ended" else
        if (auctionHistory.reverse.head.bidderId == bidderId) "leading"
        else "not leading"

      sender() ! BidderAuctionHistory(
        BidderRelatedAuctionStatus(status),
        auction,
        auctionHistory.reverse
      )

    case GetAuctionHistory(_) =>
      sender() ! Some(AuctionHistory(auctionHistory.reverse))

    case NewBid(_, bidderId, price) =>
      val now = new Timestamp(new Date().getTime)
      val auctionId = auction.itemId
      state match {
      case Planned =>
        if (now.before(startDate)) {
          log.info("Too early to bid ! Auction starting at {}.", startDate)
          sender() ! ActionPerformed(
            s"Bid failed : too early too Bid ! Bid starting at $startDate.",
            false
          )
        }

        else if (now.after(endDate)) {
          log.info("Too late to bid ! Closing {}", auctionId)
          state = Closed
          sender() ! ActionPerformed(
            s"Bid failed : too late too Bid ! Bid closed at $endDate.",
            false
          )
        }

        else if (price >= floorPrice) {
          state = Open
          currentPrice = price
          auctionHistory += SuccessfulBid(bidderId, price, now)
          log.info(
            "First Bid registered for auction-{} by bidder-{} with a price of {}.",
            auctionId,
            bidderId,
            price
          )
          sender() ! ActionPerformed(
            s"Bid successful for Bidder $bidderId, Auction $auctionId and a price of $price.",
            true
          )
        }

        else if (price < floorPrice) {
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}.",
            auctionId,
            bidderId,
            floorPrice
          )
          sender() ! ActionPerformed(
            s"Bid failed : Insufficient price. Minimum is $floorPrice.",
            false
          )
        }

        else log.warning("Unpredicted behaviour for auction-{}.", auctionId)


      case Open =>
        if (now.after(endDate)) {
          log.info("Too late to bid ! Closing {}.", auctionId)
          state = Closed
          sender() ! ActionPerformed(
            s"Bid failed : too late too Bid ! Bid closed at $endDate.",
            false
          )
        }

        else if (price >= currentPrice + incrementPolicy) {
          currentPrice = price
          auctionHistory += SuccessfulBid(bidderId, price, now)
          log.info(
            "New Bid registered for auction-{} by bidder-{} with a price of {}",
            auctionId,
            bidderId,
            price
          )
          sender() ! ActionPerformed(
            s"Bid successful for Bidder $bidderId, Auction $auctionId and a price of $price",
            true
          )
        }

        else if (price < currentPrice + incrementPolicy) {
          val minimumPrice = currentPrice + incrementPolicy
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}",
            auctionId,
            bidderId,
            minimumPrice
          )
          sender() ! ActionPerformed(
            s"Bid failed : Insufficient price. Minimum is $minimumPrice.",
            false
          )
        }
        else log.warning("Unpredicted behaviour for {}", auctionId)

      case Closed =>
        log.info("Too late to bid ! Closing {}", auctionId)
        sender() ! ActionPerformed(
          s"Bid failed : too late too Bid ! Bid closed at $endDate.",
          false
        )
    }
  }
}