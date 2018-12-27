package com.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.PathDirectives.{path, pathPrefix}
import com.JsonSupport
import akka.util.Timeout
import akka.pattern.ask
import core.akka.AuctionHouse.ActionPerformed
import core.akka.BidderManager.{CreateBidder, GetBidders}
import core.akka.{Bidder, Bidders}

import scala.concurrent.Future
import scala.concurrent.duration._

trait BidderRoutes extends JsonSupport {
  implicit def system: ActorSystem

  private lazy val log = Logging(system, classOf[BidderRoutes])
  
  def bidderManagerActor: ActorRef

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
        }
      )
    }

}
