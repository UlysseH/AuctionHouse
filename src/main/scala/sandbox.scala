import akka.actor._
import core.akka.AuctionSystemSupervisor
import core.akka.AuctionHouse.RequestAuctioneer
import core.akka.BidderManager.RequestBidder

import scala.io.StdIn

object sandbox {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("testSystem")

    try {
      // Create top level supervisor
      val supervisor = system.actorOf(AuctionSystemSupervisor.props(), "auctionSystem-supervisor")

      supervisor ! RequestAuctioneer("1")
      supervisor ! RequestBidder("1")

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
