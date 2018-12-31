package com.routes

import java.sql.Timestamp
import java.util.Date

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MessageEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.akka.{Auction, AuctionHouse, Bidder, BidderManager}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import spray.json.JsString

class BidderRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with BidderRoutes {
  val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def dateWrite(obj: Timestamp) = JsString(dateFormat.format(obj.getTime))

  override val auctionHouseActor: ActorRef =
    system.actorOf(AuctionHouse.props, "auctionHouse")

  override val bidderManagerActor: ActorRef =
    system.actorOf(BidderManager.props, "bidderManager")

  lazy val routes = bidderRoutes

  "BidderRoutes" should {
    "return no bidders if no presetn (GET /bidders)" in {
      val request = HttpRequest(uri = "/bidders")

      request ~> routes ~> check {
        status should === (StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"bidders":[]}""")
      }
    }

    "be able to add bidders (POST /bidders)" in {
      val bidder = Bidder("Bob")
      val bidderEntity = Marshal(bidder).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request = Post("/bidders").withEntity(bidderEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        //entityAs[String] should ===("""{"description":"Bidder Bob created."}""")
      }
    }

    "be able to provide a consistent bidding system (POST /users/$id/bid)" in {
      val auctionStart = new Timestamp(new Date().getTime + 100000)
      val auctionEnd = new Timestamp(new Date().getTime + 200000)
      val auction = Auction(
        "1",
        100,
        1,
        auctionStart,
        auctionEnd
      )

      val bidderBob = Bidder("Bob")
      val bidderBobby = Bidder("Bobby")
    }
  }
}
