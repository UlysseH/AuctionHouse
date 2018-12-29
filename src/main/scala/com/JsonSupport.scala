package com

import core.akka._
import core.akka.AuctionSystemSupervisor._
import spray.json.{DeserializationException, JsNumber, JsString, JsValue, JsonFormat}
import java.sql.Timestamp

import core.akka.AuctionActor.AuctionHistory
import core.akka.AuctionHouse.{ActionPerformed, Bid, SuccessfulBid}
import core.akka.BidderManager.{BidderAuctionHistory, BidderAuctionHouseHistory, BidderRelatedAuctionStatus}

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol



trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    //TODO: revoir avec la norme ISO 8601
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def write(obj: Timestamp) = JsString(format.format(obj.getTime))

    def read(json: JsValue) = json match {
      case JsString(s) => new Timestamp(format.parse(s).getTime)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val auctionIdJsonFormat = jsonFormat1(AuctionId)
  implicit val auctionJsonFormat = jsonFormat5(Auction)
  implicit val auctionsJsonFormat = jsonFormat1(Auctions)

  implicit val successfulBidJsonFormat = jsonFormat3(SuccessfulBid)
  implicit val auctionHistoryJsonFormat = jsonFormat1(AuctionHistory)

  implicit val bidderRelatedAuctionStatusJsonFormat = jsonFormat1(BidderRelatedAuctionStatus)
  implicit val bidderRelatedAuctionHistoryJsonFormat = jsonFormat3(BidderAuctionHistory)
  implicit val bidderRelatedAuctionHouseHistoryJsonFormat = jsonFormat1(BidderAuctionHouseHistory)

  implicit val bidderJsonFormat = jsonFormat1(Bidder)
  implicit val biddersJsonFormat = jsonFormat1(Bidders)

  implicit val bidJsonFormat = jsonFormat2(Bid)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
