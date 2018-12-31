package core.akka

import java.sql.Timestamp

import akka.actor.ActorRef

sealed trait Message

final case class GetAuctionHistory(auctionId: String) extends Message
final case class CreateAuction(auction: Auction) extends Message
final case class ActionPerformed(description: String, success: Boolean)
final case class Bid(auctionId: String, price: Float) extends Message
final case class SuccessfulBid(bidderId: String, price: Float, time: Timestamp) extends Message
final case object GetAuctions extends Message
final case class GetAuction(auctionId: String) extends Message
final case class CreateBidder(bidder: Bidder) extends Message
final case object GetBidders extends Message
final case class GetBidder(bidderId: String) extends Message
final case class GetAuctionHouseHistory(bidderId: String) extends Message
final case class JoinAuction(bidderId: String, auctionId: String, auctionActor: ActorRef) extends Message

final case class AuctionJoined(auctionId: String, bidderId: String) extends Message
final case class BidderRelatedAuctionStatus(message: String) extends Message
final case class BidderAuctionHistory(
                                       status: BidderRelatedAuctionStatus,
                                       auction: Auction,
                                       history: Seq[SuccessfulBid]
                                     ) extends Message
final case class BidderAuctionHouseHistory(history: Seq[BidderAuctionHistory]) extends Message

final case class NewBid(auctionId: String, bidderId: String, price: Float) extends Message
final case class UpdateAuction(auction: Auction) extends Message

//sent messages
final case class AuctionHistory(history: Seq[SuccessfulBid]) extends Message