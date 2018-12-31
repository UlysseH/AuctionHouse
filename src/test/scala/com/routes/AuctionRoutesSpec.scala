package com.routes

import java.sql.Timestamp
import java.util.Date

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.akka.{Auction, AuctionHouse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import spray.json.JsString

class AuctionRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with AuctionRoutes {
  val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def dateWrite(obj: Timestamp) = JsString(dateFormat.format(obj.getTime))

  override val auctionHouseActor: ActorRef =
    system.actorOf(AuctionHouse.props, "auctionHouse")

  lazy val routes = auctionRoutes

  "AuctionRoutes" should {
    "return no auctions if no present (GET /auctions)" in {
      val request = HttpRequest(uri = "/auctions")

      request ~> routes ~> check {
        status should === (StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"auctions":[]}""")
      }
    }

    "be able to add auctions (POST /auctions), with restrictions, and then read it (GET /auctions/$id)" in {
      val auction1start = new Timestamp(new Date().getTime + 100000)
      val auction1end = new Timestamp(new Date().getTime + 200000)
      val auction1startJson = dateWrite(auction1start)
      val auction1endJson = dateWrite(auction1end)
      val auction1 = Auction(
        "1",
        100,
        1,
        auction1start,
        auction1end
      )
      val auctionEntity = Marshal(auction1).to[MessageEntity].futureValue
      val request1 = Post("/auctions").withEntity(auctionEntity)

      val auction2 = Auction(
        "2",
        100,
        1,
        new Timestamp(new Date().getTime - 100000),
        new Timestamp(new Date().getTime + 200000)
      )
      val auction2Entity = Marshal(auction2).to[MessageEntity].futureValue
      val request2 = Post("/auctions").withEntity(auction2Entity)

      val auction3 = Auction(
        "3",
        100,
        1,
        new Timestamp(new Date().getTime + 300000),
        new Timestamp(new Date().getTime + 200000)
      )
      val auction3Entity = Marshal(auction3).to[MessageEntity].futureValue
      val request3 = Post("/auctions").withEntity(auction3Entity)

      val request4 = HttpRequest(uri = "/auctions/1")

      request1 ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        //entityAs[String] should ===("""{"description":"Auction 1 created."}""")
      }
      request2 ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`application/json`)
        //entityAs[String] should ===("""{"description":"Auction 1 created."}""")
      }
      request3 ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`application/json`)
        //entityAs[String] should ===("""{"description":"Auction 1 created."}""")
      }
      request4 ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(
          s"""{"endDate":$auction1endJson,"floorPrice":100.0,"incrementPolicy":1.0,"itemId":"1","startDate":$auction1startJson}"""
        )
      }

    }
  }

}
