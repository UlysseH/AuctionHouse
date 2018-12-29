package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import akka.http.scaladsl.server.Directives.onSuccess
import akka.io.Tcp.Message
import core.akka.AuctionActor.NewBid
import core.akka.AuctionHouse.{ActionPerformed, GetAuction, GetAuctionHistory}
import core.akka.BidderManager.{AuctionJoined, BidderAuctionHistory, GetAuctionHouseHistory, JoinAuction}

import scala.concurrent.Future
import scala.concurrent.duration._

// Get the implicit ExecutionContext from this import
import scala.concurrent.ExecutionContext.Implicits.global

//itemId -> Item case class ?
//case class Bid(bidderId: String, itemId: String, value: Double, bidId: String)

object BidderActor {
  def props(bidderId: String): Props = Props(new BidderActor(bidderId))

  //final case class getActionHouseState(requestId: String)
  //final case class joinAuction(requestId: String)
  //final case class placeBid(requestId: String, bid: Bid)

  private implicit lazy val timeout: Timeout = Timeout(5.seconds)

}

class BidderActor(bidderId: String) extends Actor with ActorLogging {
  import BidderActor._

  override def preStart(): Unit = log.info("Bidder started")
  override def postStop(): Unit = log.info("Bidder stopped")

  var auctionsJoined = Map.empty[String, ActorRef]

  // No need to handle any messages
  override def receive: Receive = {
    case msg @ ActionPerformed(_) =>
      context.actorSelection("../..") forward msg

    case BidderManager.RequestBidder(`bidderId`) =>
      sender() ! BidderManager.BidderRegistered

    case BidderManager.RequestBidder(bidderId) =>
      log.warning(
        "Ignoring Bidder request for {}. This actor is responsible for {}",
        bidderId, this.bidderId
      )

    //case Bidv2(auctionId, price) =>

    case JoinAuction(`bidderId`, id, ref) =>
      auctionsJoined += id -> ref
      println(auctionsJoined)
      sender() ! ActionPerformed(s"Bidder $bidderId joined auction $id")

    case msg @ NewBid(auctionId, `bidderId`, _) =>
      auctionsJoined.get(auctionId) match {
        case None => sender() ! ActionPerformed(s"Bidder $bidderId has yet to join auction $auctionId")
        case Some(ref) => (ref ? msg) pipeTo sender()
      }

    case msg @ GetAuctionHouseHistory(`bidderId`) =>


      val history = auctionsJoined.map( x =>
        (x._2 ? msg))
      //val tmp = Future.sequence(history)
      sender() ! history

    //case msg @ GetAuctionHouseHistory(`bidderId`) =>
    //  auctionsJoined.values.foreach(ref =>

    //  )

  }
      //auctionsJoined += auctionId
      //println(auctionsJoined)
      //

    //case AuctionJoined(`bidderId`,  auctionId) =>
    //  auctionsJoined += auctionId
    //  context.actorSelection("../..") ! ActionPerformed(s"Bidder $bidderId joined auction $auctionId")
}
