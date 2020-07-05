//#full-example
package com.hdq

import akka.stream._
import akka.stream.scaladsl._

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import cats.syntax.either._
import io.circe._, io.circe.parser._
import io.circe.optics.JsonPath._

object Main extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher

  val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);

  val b:ByteString = ByteString("x")

  val intToJsonString: Int => String = (x: Int) => s"""{"a":$x}"""

  val clean1: Json => Json = root.a.int.modify(_ => 100)
  // val clean2 = root.a.arr.modify(_ => Vector[Json]())

  val done: Future[IOResult] =
    Source(1 to 100)
      .map(intToJsonString)
      .map(ByteString(_))
      .via(frameJson)
      .map(bs => parse(bs.utf8String).getOrElse(Json.Null))
      .map(clean1)
      .map(json => ByteString(json.toString()))
      .runWith(FileIO.toPath(Paths.get(".data/factorials.txt")))

  done.onComplete(_ => system.terminate())
}
