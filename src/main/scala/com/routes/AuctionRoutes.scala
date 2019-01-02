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
import core.akka.{Auction, Auctions}
import akka.util.Timeout
import akka.pattern.ask
import core.akka._

import scala.concurrent.Future
import scala.concurrent.duration._

trait AuctionRoutes extends JsonSupport {
  implicit def system: ActorSystem

  private lazy val log = Logging(system, classOf[AuctionRoutes])

  def auctionHouseActor: ActorRef

  // Required by the `ask` (?) method below
  private implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val auctionRoutes: Route =
    pathPrefix("auctions") {
      concat(
        pathEnd {
          concat(
            get {
              val auctions: Future[Auctions] =
                (auctionHouseActor ? GetAuctions).mapTo[Auctions]
              complete(auctions)
            },
            post {
              entity(as[Auction]) { auction =>
                val auctionCreated: Future[ActionPerformed] =
                  (auctionHouseActor ? CreateAuction(auction)).mapTo[ActionPerformed]

                onSuccess(auctionCreated) { performed =>
                  if (performed.success) {
                    val id = auction.itemId
                    if (performed.description == s"Auction $id created.") {
                      log.info("Created auction [{}]: {}.", auction.itemId, performed.description)
                      complete((StatusCodes.Created, performed))
                    }
                    else if (performed.description == s"Updated auction $id.") {
                      log.info("Updated auction [{}]: {}.", auction.itemId, performed.description)
                      complete((StatusCodes.OK, performed))
                    }
                    else {
                      log.info(s"Unexpected issue with auction [{}]: {}.", auction.itemId, performed.description)
                      complete((StatusCodes.InternalServerError, performed))
                    }
                  }
                  else {
                    log.info(
                      "Couldn't create or update auction [{}]: {}.",
                      auction.itemId,
                      performed.description
                    )
                    complete((StatusCodes.BadRequest, performed))
                  }
                }
              }
            }
          )
        },
        path(Segment) { id =>
          get {
            val maybeAuction: Future[Option[Auction]] =
              (auctionHouseActor ? GetAuction(id)).mapTo[Option[Auction]]
            rejectEmptyResponse {
              complete(maybeAuction)
            }
          }
        },
        path(Segments(2)) { s => s match {
          case auctionId::"auction_history"::_ =>
            get {
              val maybeAuctionHistory: Future[Option[AuctionHistory]] =
                (auctionHouseActor ? GetAuctionHistory(auctionId)).mapTo[Option[AuctionHistory]]
              rejectEmptyResponse {
                complete(maybeAuctionHistory)
            }
          }
        }
        }
      )
    }
}
