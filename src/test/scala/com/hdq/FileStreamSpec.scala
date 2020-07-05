package com.hdq

import java.net.URL
import java.nio.file.{Path, Paths}

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, JsonFraming, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import org.scalatest.wordspec.AnyWordSpecLike
import io.circe._
import io.circe.parser._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class FileStreamSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Sample file stream" must {
    "output file as expected" in {
      val file: Path = Paths.get(getClass.getResource("/sample.json").getPath)
      // val fff: Path = Paths.get("sample.json")
      // fdsfds
      Console.println(file.toString)
      val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get(".data/test-output.txt"))

      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);

      val graph: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString]
        .via(frameJson)
        .map(bs => parse(bs.utf8String).getOrElse(Json.Null))
        .map(json => ByteString(json.toString()))

      val runnableGraph: RunnableGraph[Future[IOResult]] = fileSource.via(graph).toMat(fileSink)(Keep.right)
      val futureResult: Future[IOResult] = runnableGraph.run()
      val result: IOResult = Await.result(futureResult, 5.seconds)

      assert(result.getCount > 0)
    }
  }
}
