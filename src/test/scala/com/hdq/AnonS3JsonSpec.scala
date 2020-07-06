package com.hdq

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.util.ByteString

import scala.concurrent.duration._

import akka.stream.alpakka.s3.{MultipartUploadResult, ObjectMetadata}
import akka.stream.alpakka.s3.scaladsl.S3
import io.circe._
import io.circe.parser._
import io.circe.optics.JsonPath._



class AnonS3JsonSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val sourcebucket = "TODO"
  val sourceBucketKey = "akka-quickstart-scala/sample-person.json"

  val destBucket = "TODO"
  val destBucketKey = "akka-quickstart-scala/sample-person-redacted.json"

  "Anonymization" must {
    "remove PII" in {
      val source: Either[Throwable, Source[ByteString, NotUsed]] =
        Transforms.getS3Source(sourcebucket, sourceBucketKey)
      val sink: Sink[ByteString, Future[MultipartUploadResult]] =
        S3.multipartUpload(destBucket, destBucketKey)

      val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000);

      val redactName = root.details.name.string.modify(_ => "REDACTED")
      val redactAddress = root.address.string.modify(_ => "REDACTED")
      val anonymize: Json => Json = redactName.andThen(redactAddress)

      val flow: Flow[ByteString, ByteString, NotUsed] = frameJson
          .map(bs => parse(bs.utf8String).getOrElse(Json.Null))
          .map(anonymize)
          .map(json => ByteString(json.toString()))

      val result: Either[Throwable, MultipartUploadResult] = source.map(s => {
        val uploadResultFuture: Future[MultipartUploadResult] = s.via(flow).runWith(sink)
        Await.result(uploadResultFuture, 10.seconds)
      })

      assert(result.isRight)
    }
  }

}
