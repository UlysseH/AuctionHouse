package com

//#quick-start-server
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.routes.AuctionRoutes
import com.routes.BidderRoutes
import akka.http.scaladsl.server.Route

import akka.http.scaladsl.server.Directives._


import core.akka.{AuctionHouse, BidderManager}

//#main-class
object QuickStartServer extends App with AuctionRoutes with BidderRoutes {

  // set up ActorSystem and other dependencies here
  //#main-class
  //#server-bootstrapping
  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  //#server-bootstrapping

  val auctionHouseActor: ActorRef = system.actorOf(AuctionHouse.props, "auctionHouse")
  val bidderManagerActor: ActorRef = system.actorOf(BidderManager.props, "bidderManager")

  //#main-class
  // from the UserRoutes trait
  lazy val routes: Route = concat(auctionRoutes, bidderRoutes)
  //#main-class

  //#http-server
  Http().bindAndHandle(routes, "localhost", 5000)

  println(s"Server online at http://localhost:5000/")

  Await.result(system.whenTerminated, Duration.Inf)
  //#http-server
  //#main-class
}
//#main-class
//#quick-start-server
