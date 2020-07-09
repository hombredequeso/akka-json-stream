//#full-example
package com.hdq

import akka.actor.ActorSystem
import akka.NotUsed
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{FileIO, Flow, JsonFraming, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import io.circe._
import io.circe.parser._
import io.circe.optics.JsonPath._

import scala.concurrent.{Await, Future}

object Main extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher

  val sourcebucket = args(0)
  val sourceBucketKey = args(1)
  val destBucket = args(2)
  val destBucketKey = args(3)

  val source: Either[Throwable, Source[ByteString, NotUsed]] =
    Transforms.getS3Source(sourcebucket, sourceBucketKey)
  val sink: Sink[ByteString, Future[MultipartUploadResult]] =
    S3.multipartUpload(destBucket, destBucketKey)

  val frameJson: Flow[ByteString, ByteString, NotUsed] = JsonFraming.objectScanner(1000)

  val redactName = root.details.name.string.modify(_ => "REDACTED")
  val redactAddress = root.address.string.modify(_ => "REDACTED")
  val anonymize: Json => Json = redactName.andThen(redactAddress)

  val flow: Flow[ByteString, ByteString, NotUsed] = frameJson
    .map(bs => parse(bs.utf8String).getOrElse(Json.Null))
    .map(anonymize)
    .map(json => ByteString(json.noSpaces + "/n"))

  val rr: Future[Any] = source.fold(t => Future(), s => s.via(flow).runWith(sink))

  rr.onComplete(_ => system.terminate())
}
