package core.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}



object BidderManager {
  def props(): Props = Props(new BidderManager)

  final case class RequestBidder(bidderId: String)
  case object BidderRegistered
}

class BidderManager extends Actor with ActorLogging {
  import BidderManager._

  var bidderIdToActor = Map.empty[String, ActorRef]
  var actorToBidderId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("Bidder started")
  override def postStop(): Unit = log.info("Bidder stoped")

  // No need to handle any messages
  override def receive: Receive = {
    case trackMsg @ RequestBidder(bidderId) =>
      bidderIdToActor.get(bidderId) match {
        case Some(ref) =>
          ref forward trackMsg
        case None =>
          log.info("Creating new bidder for {}", bidderId)
          val bidderActor = context.actorOf(Bidder.props(bidderId), "bidder-" + bidderId)
          bidderIdToActor += bidderId -> bidderActor
          actorToBidderId += bidderActor -> bidderId
      }
  }
}
