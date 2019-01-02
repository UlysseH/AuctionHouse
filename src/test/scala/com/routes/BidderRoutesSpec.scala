package com.routes

import java.sql.Timestamp
import java.util.Date

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MessageEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.akka._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import spray.json.JsString

class BidderRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with BidderRoutes with AuctionRoutes {
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
      val bidder = Bidder("0")
      val bidderEntity = Marshal(bidder).to[MessageEntity].futureValue
      val request = Post("/bidders").withEntity(bidderEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
      }
    }

    "be able to provide a consistent bidding system (POST /users/$id/bid)" in {
      val auctionStart = new Timestamp(new Date().getTime + 1000)
      val auctionEnd = new Timestamp(new Date().getTime + 200000)
      val auction = Auction(
        "1",
        100,
        1,
        auctionStart,
        auctionEnd
      )
      println(auction)
      val auctionEntity = Marshal(auction).to[MessageEntity].futureValue

      val auctionId = AuctionId(auction.itemId)
      val auctionIdEntity = Marshal(auctionId).to[MessageEntity].futureValue

      val bidder1 = Bidder("1")
      val bidder1Entity = Marshal(bidder1).to[MessageEntity].futureValue

      val bidder2 = Bidder("2")
      val bidder2Entity = Marshal(bidder2).to[MessageEntity].futureValue

      val bid1Entity = Marshal(Bid("1", 100)).to[MessageEntity].futureValue
      val bid2Entity = Marshal(Bid("1", 125)).to[MessageEntity].futureValue
      val bid3Entity = Marshal(Bid("1", 200)).to[MessageEntity].futureValue

      val createAuction = Post("/auctions").withEntity(auctionEntity)

      val createBidder1 = Post("/bidders").withEntity(bidder1Entity)
      val createBidder2 = Post("/bidders").withEntity(bidder2Entity)

      val bidder1JoinAuction = Post("/bidders/1/join_auction").withEntity(auctionIdEntity)
      val bidder2JoinAuction = Post("/bidders/2/join_auction").withEntity(auctionIdEntity)

      val bidder1Bid1 = Post("/bidders/1/bid").withEntity(bid1Entity)
      val bidder2Bid2 = Post("/bidders/2/bid").withEntity(bid2Entity)
      val bidder1Bid3 = Post("/bidders/1/bid").withEntity(bid3Entity)

      val bidder1History = Get("/bidders/1/auction_house_history")

      createAuction ~> auctionRoutes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
      }

      createBidder1  ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
      }

      createBidder2  ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
      }

      Thread.sleep(1000) //waiting to be sure auction start date is reached

      bidder1JoinAuction ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }

      bidder2JoinAuction ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }

      bidder1Bid1 ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }

      bidder2Bid2 ~> routes ~> check {
        println(entityAs[String])
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }

      bidder1Bid3 ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
      }

      bidder1History ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)

      }
    }
  }
}
