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

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream.alpakka.s3.{MultipartUploadResult, ObjectMetadata}
import akka.stream.alpakka.s3.ObjectMetadata
import akka.stream.alpakka.s3.scaladsl.S3
import cats.syntax.either._
import io.circe._
import io.circe.parser._
import io.circe.optics.JsonPath._

import scala.reflect.ClassTag

object Transforms {
  def sequenceAOption[A, B](s: Source[Option[A], B])(implicit tag: ClassTag[A]): Option[Source[A, B]] = {
    class GotNoneException() extends Exception {
    }
    try {
      val a = s.map(x => x match {
        case Some(v: A) => v
        case None => throw new GotNoneException()
      })
      Some(a)
    } catch {
      case _: GotNoneException => None
    }
  }

  def noneToEither[A, B](s: Source[Option[A], B])(msg: String)(implicit tag: ClassTag[A]): Either[Throwable, Source[A, B]] = {
    class GotNoneException(msg: String) extends Exception {
    }
    try {
      val a = s.map(x => x match {
        case Some(v: A) => v
        case None => throw new GotNoneException(msg)
      })
      Right(a)
    } catch {
      case ex: GotNoneException => Left(ex)
    }
  }

  class S3LocationDoesNotExist(s3Location: String) extends Exception

  def getS3Source(bucket: String, key: String): Either[Throwable, Source[ByteString, NotUsed]] = {
    val s3Source: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] = S3.download(bucket, key)
    val streamData: Source[Option[Source[ByteString, NotUsed]], NotUsed] = s3Source.map(x => x.map(y => y._1))
    val s3b: Option[Source[Source[ByteString, NotUsed], NotUsed]] = sequenceAOption(streamData)
    val leftException: Either[Throwable, Source[Source[ByteString, NotUsed], NotUsed]] =
      Left(new S3LocationDoesNotExist(s"${bucket}/${key}"))
    val s4b: Either[Throwable, Source[Source[ByteString, NotUsed], NotUsed]] =
      s3b.fold(leftException)(Right(_))
    s4b.map(x => x.flatMapConcat(identity))
  }
}
class AnonS3JsonSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val sourcebucket = "temp-8cd36850276c"
  val sourceBucketKey = "akka-quickstart-scala/sample-person.json"

  val destBucket = "temp-8cd36850276c"
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
