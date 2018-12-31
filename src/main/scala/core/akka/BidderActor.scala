package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._
// Get the implicit ExecutionContext from this import
import scala.concurrent.ExecutionContext.Implicits.global

object BidderActor {
  def props(bidderId: String): Props = Props(new BidderActor(bidderId))

  private implicit lazy val timeout: Timeout = Timeout(5.seconds)
}

class BidderActor(bidderId: String) extends Actor with ActorLogging {
  import BidderActor._

  override def preStart(): Unit = log.info("Bidder started")
  override def postStop(): Unit = log.info("Bidder stopped")

  var auctionsJoined = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case JoinAuction(`bidderId`, id, ref) =>
      auctionsJoined += id -> ref
      sender() ! ActionPerformed(s"Bidder $bidderId joined auction $id.", true)

    case msg @ NewBid(auctionId, `bidderId`, _) =>
      auctionsJoined.get(auctionId) match {
        case None => sender() ! ActionPerformed(s"Bidder $bidderId has yet to join auction $auctionId.", false)
        case Some(ref) => (ref ? msg) pipeTo sender()
      }

    case msg @ GetAuctionHouseHistory(`bidderId`) =>
      val history = auctionsJoined.map( auction => auction._2 ? msg)
      sender() ! history
  }
}
