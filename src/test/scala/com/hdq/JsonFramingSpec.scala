package com.hdq

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.{Flow, JsonFraming, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import org.scalatest.wordspec.AnyWordSpecLike

import io.circe._, io.circe.parser._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class JsonFramingSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val jsonStringInput =
    """ {"a":0}
      |{"a":1}
      |{"a": 2}
      |""".stripMargin

  "Sample stream" must {
    "without newlines returns expected result" in {
      val source: Source[ByteString, NotUsed] = Source.single(ByteString(jsonStringInput.replaceAll("\n", "")))
      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);
      val graph: RunnableGraph[Future[Int]] = source
        .via(frameJson)
        .map(bs => parse(bs.utf8String).getOrElse(throw new Exception()))
        .toMat(Sink.fold(0)((u, _)=> u + 1))(Keep.right)
      val future: Future[Int] = graph.run
      val result: Int = Await.result(future, 3.seconds)
      assert(result === 3)
    }

    "with newlines returns expected result" in {
      val source: Source[ByteString, NotUsed] = Source.single(ByteString(jsonStringInput))
      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);
      val graph: RunnableGraph[Future[Int]] = source
        .via(frameJson)
        .map(bs => parse(bs.utf8String).getOrElse(throw new Exception()))
        .toMat(Sink.fold(0)((u, _)=> u + 1))(Keep.right)
      val future: Future[Int] = graph.run
      val result: Int = Await.result(future, 3.seconds)
      assert(result === 3)
    }

    val jsonStringInputWithCommas =
      """ {"a":0},
        |{"a":1},
        |{"a": 2},
        |""".stripMargin
    "with newlines and commas returns expected result" in {
      val source: Source[ByteString, NotUsed] = Source.single(ByteString(jsonStringInputWithCommas))
      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);
      val graph: RunnableGraph[Future[Int]] = source
        .via(frameJson)
        .map(bs => parse(bs.utf8String).getOrElse(throw new Exception()))
        .toMat(Sink.fold(0)((u, _)=> u + 1))(Keep.right)
      val future: Future[Int] = graph.run
      val result: Int = Await.result(future, 3.seconds)
      assert(result === 3)
    }

    val appendNextLine = (s: String, n: Json) => s"$s\n${n.noSpaces}"

    "with newlines and commas returns expected result 2" in {
      val source: Source[ByteString, NotUsed] = Source.single(ByteString(jsonStringInputWithCommas))
      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);
      val graph: RunnableGraph[Future[String]] = source
        .via(frameJson)
        .map((bs) => parse(bs.utf8String).getOrElse(throw new Exception()))
        .toMat(Sink.fold("")(appendNextLine))(Keep.right)
      val future: Future[String] = graph.run
      val result: String = Await.result(future, 3.seconds)
      Console.println("THE RESULT:")
      Console.println(result);
      assert(result.length > 10)
    }

    val jsonStringInputArray =
      """ [{"a":0},
        |{"a":1},
        |{"a": 2}]
        |""".stripMargin
    "as array returns expected result" in {
      val source: Source[ByteString, NotUsed] = Source.single(ByteString(jsonStringInputArray))
      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);
      val graph: RunnableGraph[Future[Int]] = source
        .via(frameJson)
        .map(bs => parse(bs.utf8String).getOrElse(throw new Exception()))
        .toMat(Sink.fold(0)((u, _)=> u + 1))(Keep.right)
      val future: Future[Int] = graph.run
      val result: Int = Await.result(future, 3.seconds)
      assert(result === 3)
    }
    val jsonStringInputObject =
      """ {{"a":0},
        |{"a":1},
        |{"a": 2}}
        |""".stripMargin
    "object returns expected result" in {
      val source: Source[ByteString, NotUsed] = Source.single(ByteString(jsonStringInputObject))
      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);
      val graph: RunnableGraph[Future[Int]] = source
        .via(frameJson)
        .toMat(Sink.fold(0)((u, _)=> u + 1))(Keep.right)
      val future: Future[Int] = graph.run
      val result: Int = Await.result(future, 3.seconds)
      assert(result === 1)
    }
  }
}
