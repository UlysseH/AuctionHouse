package com.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.PathDirectives.{path, pathPrefix}
import com.JsonSupport
import akka.util.Timeout
import akka.pattern.ask
import core.akka._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

// Get the implicit ExecutionContext from this import
import scala.concurrent.ExecutionContext.Implicits.global

trait BidderRoutes extends JsonSupport {
  implicit def system: ActorSystem

  private lazy val log = Logging(system, classOf[BidderRoutes])
  
  def bidderManagerActor: ActorRef
  def auctionHouseActor: ActorRef

  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val bidderRoutes: Route =
    pathPrefix("bidders") {
      concat(
        pathEnd {
          concat(
            get {
              val bidders: Future[Bidders] =
                (bidderManagerActor ? GetBidders).mapTo[Bidders]
              complete(bidders)
            },
            post {
              entity(as[Bidder]) { bidder =>
                val bidderCreated: Future[ActionPerformed] =
                  (bidderManagerActor ? CreateBidder(bidder)).mapTo[ActionPerformed]

                onSuccess(bidderCreated) { performed =>
                  log.info("Created bidder [{}]: {}", bidder.bidderId, performed.description)
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        path(Segments(2)) { _ match {
          case bidderId::"join_auction"::_ => post { entity(as[AuctionId]) {
            auction =>
              val auctionId = auction.auctionId
              val auctionActor: ActorRef = Await.result(
                (auctionHouseActor ? GetAuction(auctionId)).mapTo[ActorRef],
                5.seconds
              )

              val auctionJoined: Future[ActionPerformed] =
                (bidderManagerActor ? JoinAuction(bidderId, auctionId, auctionActor)).mapTo[ActionPerformed]


              onSuccess(auctionJoined) { performed =>
                log.info("Bidder {} joined auction {}: {}", bidderId, auctionId, performed.description)
                complete((StatusCodes.OK, performed))
              }

          }

        }
          case bidderId::"bid"::_ => post { entity(as[Bid]) {
            bid =>
              val bidOutcome: Future[ActionPerformed] =
                (bidderManagerActor ? NewBid(
                  bid.auctionId,
                  bidderId,
                  bid.price
                )).mapTo[ActionPerformed]

              onSuccess(bidOutcome) { performed =>
                log.info(performed.description)
                complete((StatusCodes.OK, performed))
              }

          }
          }
          case bidderId::"auction_house_history"::_ => get {
           val maybeBidderAuctionHistory = Future
              .sequence(
                Await.result(
                  (bidderManagerActor ? GetAuctionHouseHistory(bidderId)).mapTo[Seq[Future[BidderAuctionHistory]]],
                  5.seconds
                )
              )
              .flatMap(o => Future(BidderAuctionHouseHistory(o)))

            rejectEmptyResponse {
              complete(maybeBidderAuctionHistory)
            }
          }
        }
        }
      )
    }

}
