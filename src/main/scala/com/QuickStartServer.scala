package com

//#quick-start-server
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.routes.AuctionRoutes
import core.akka.AuctionHouse

//#main-class
object QuickStartServer extends App with AuctionRoutes {

  // set up ActorSystem and other dependencies here
  //#main-class
  //#server-bootstrapping
  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  //#server-bootstrapping

  val auctionHouseActor: ActorRef = system.actorOf(AuctionHouse.props, "auctionHouse")

  //#main-class
  // from the UserRoutes trait
  lazy val routes: Route = auctionRoutes
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
