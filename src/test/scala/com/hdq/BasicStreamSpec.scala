package com.hdq

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class BasicStreamSpec  extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "ObjectScanner" must {
    "process each object" in {

      val sinkUnderTest: Sink[Int, Future[Int]] =
        Flow[Int].map(_ * 2).toMat(Sink.fold(0)(_ + _))(Keep.right)

      val future: Future[Int] = Source(1 to 4).runWith(sinkUnderTest)
      val result: Int = Await.result(future, 3.seconds)
      assert(result == 20)
    }
  }
}
