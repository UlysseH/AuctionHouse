import java.sql.Timestamp
import java.util.Date

import akka.actor._
import core.akka.Auction.NewBid
import core.akka.{AuctionParams, AuctionSystemSupervisor}
import core.akka.AuctionHouse.{GetAuctionHistory, NewAuction, RequestAuctioneer}
import core.akka.BidderManager.RequestBidder

import scala.io.StdIn

object sandbox {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("testSystem")

    try {
      // Create top level supervisor
      val supervisor = system.actorOf(AuctionSystemSupervisor.props(), "auctionSystem-supervisor")

      //supervisor ! RequestAuctioneer("1")
      supervisor ! RequestBidder("1")
      supervisor ! RequestBidder("2")
      supervisor ! RequestBidder("3")


      val now = new Timestamp(new Date().getTime)
      println(now)
      //TODO: add auctioneer to NewAuction
      supervisor ! NewAuction("1", AuctionParams(100, 1, now, new Timestamp(now.getTime + 60000)))
      supervisor ! NewAuction("2", AuctionParams(100, 1, now, new Timestamp(now.getTime + 60000)))

      supervisor ! NewBid("1", "1", 90)
      supervisor ! NewBid("1", "2", 100)
      //supervisor ! NewBid("1", "1", 102)
      supervisor ! NewBid("1", "3", 100)
      supervisor ! NewBid("1", "1", 102)

      Thread.sleep(100)

      supervisor ! NewBid("1", "2", 120)
      supervisor ! NewBid("1", "3", 200)
      supervisor ! NewBid("1", "1", 315)

      supervisor ! GetAuctionHistory("1")

      Thread.sleep(5000)
      supervisor ! GetAuctionHistory("1")

      // Exit the system after ENTER is pressed

      StdIn.readLine()
    } finally {
      system.terminate()
    }

    //val auctionHouse = supervisor

    //val request1 = RequestAuctioneer("1")
    //val request2 = RequestAuctioneer("1")
  }
}
