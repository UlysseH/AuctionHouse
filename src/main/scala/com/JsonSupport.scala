package com

import core.akka.{Auction, Auctions, Bidder, Bidders}
import core.akka.AuctionSystemSupervisor._
import spray.json.{DeserializationException, JsNumber, JsString, JsValue, JsonFormat}
import java.sql.Timestamp

import core.akka.AuctionHouse.ActionPerformed

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

  implicit val auctionJsonFormat = jsonFormat5(Auction)
  implicit val auctionsJsonFormat = jsonFormat1(Auctions)

  implicit val bidderJsonFormat = jsonFormat1(Bidder)
  implicit val biddersJsonFormat = jsonFormat1(Bidders)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)


}
