package core.akka

import akka.actor.{ActorLogging, Actor, Props}

//itemId -> Item case class ?
object Auctioneer {
  def props(auctioneerId: String): Props = Props(new Auctioneer(auctioneerId))

}

class Auctioneer(auctioneerId: String) extends Actor with ActorLogging {
  import Auctioneer._

  override def preStart(): Unit = log.info("Auctioneer started")
  override def postStop(): Unit = log.info("Auctioneer stopped")

  override def receive: Receive = {
    case AuctionHouse.RequestAuctioneer(`auctioneerId`) =>
      val s = sender()
      println(s)
      s ! AuctionHouse.AuctioneerRegistered

    case AuctionHouse.RequestAuctioneer(auctioneerId) =>
      log.warning(
        "Ignoring Auctioneer request for {}. This actor is responsible for {}",
        auctioneerId, this.auctioneerId
      )
  }
}
