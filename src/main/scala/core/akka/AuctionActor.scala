package core.akka

import java.sql.Timestamp
import java.util.Date

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, Props, Timers}
import core.akka.AuctionHouse.{ActionPerformed, CreateAuction, NewAuction, RequestAuction}
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
object AuctionActor {
  def props(auctionId: String): Props = Props(new AuctionActor(auctionId))

  //received messages
  final case class NewBid(auctionId: String, bidderId: String, price: Float)
  final case class SetValues(floorPrice: Double, incrementPolicy: Float, startDate: Timestamp, endDate: Timestamp)

  //sent messages
  final case class BidFailed(auctionId: String, bidderId: String)
  final case class BidSuccessful(auctionId: String, bidderId: String, price: Float, time: Long)


  //states
  sealed trait State

  case object Planned extends State
  case object Open extends State
  case object Closed extends State

  var state: State = Planned
  var currentPrice: Float = -1
  
  var floorPrice: Double = -1
  var incrementPolicy: Float = -1
  var startDate: Timestamp = new Timestamp(0)
  var endDate: Timestamp = new Timestamp(0)
  
  var auctionHistory = new ListBuffer[(String, Float, Long)]()
}

class AuctionActor(auctionId: String) extends Actor with ActorLogging with Timers {
  import AuctionActor._

  //timers.startSingleTimer()

  override def preStart(): Unit = log.info("Auction started")
  override def postStop(): Unit = log.info("Auction stopped")

  override def receive: Receive = {
    case SetValues(price: Double, increment: Float, start: Timestamp, end: Timestamp) =>
      floorPrice = price
      incrementPolicy = increment
      startDate = start
      endDate = end
    
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


    case NewBid(`auctionId`, bidderId, price) =>
      val now = new Timestamp(new Date().getTime)
      state match {
      case Planned =>
        if (now.before(startDate)) {
          log.info("Too early to bid ! Auction starting at {}", startDate)
          context.parent ! BidFailed(auctionId, bidderId)
        }

        else if (now.after(endDate)) {
          log.info("Too late to bid ! Closing {}", auctionId)
          state = Closed
          context.parent ! BidFailed(auctionId, bidderId)
        }

        else if (price >= floorPrice) {
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

        else if (price < floorPrice) {
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}",
            auctionId,
            bidderId,
            floorPrice
          )
        }

        else log.warning("Unpredicted behaviour for auction-{}", auctionId)


      case Open =>
        if (now.after(endDate)) {
          log.info("Too late to bid ! Closing {}", auctionId)
          state = Closed
          context.parent ! BidFailed(auctionId, bidderId)
        }

        //TODO: factoriser ?
        else if (price >= currentPrice + incrementPolicy) {
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

        else if (price < currentPrice + incrementPolicy) {
          log.info(
            "Insufficient price for auction-{} by bidder-{}. Minimum is {}",
            auctionId,
            bidderId,
            currentPrice + incrementPolicy
          )
        }
        else log.warning("Unpredicted behaviour for {}", auctionId)

      case Closed =>
        log.info("Too late to bid ! Closing {}", auctionId)
        context.parent ! BidFailed(auctionId, bidderId)
    }
  }
}