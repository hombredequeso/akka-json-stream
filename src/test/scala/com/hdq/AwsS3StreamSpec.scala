package com.hdq

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.alpakka.s3.{MultipartUploadResult, ObjectMetadata}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

class AwsS3StreamSpec  extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  def getFromS3(bucket: String, bucketKey: String): Future[String] = {
    val Some((data: Source[ByteString, _], metadata)) =
      S3.download(bucket, bucketKey).runWith(Sink.head).futureValue
    val future: Future[String] =
      data.map(_.utf8String).runWith(Sink.head)
    future
  }

  "S3 write" must {

    "upload the entire file" in {
      val body = "abcdef"
      val file: Source[ByteString, NotUsed] =
        Source.single(ByteString(body))

      val bucket = "temp-8cd36850276c"
      val rnd: Random = new scala.util.Random()
      val bucketKey = s"akka-quickstart-scala/test-upload-${rnd.nextInt()}.txt"

      val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
        S3.multipartUpload(bucket, bucketKey)

      val future: Future[MultipartUploadResult] =
        file.runWith(s3Sink)

      val result: MultipartUploadResult = Await.result(future, 10.seconds)

      val retrievedFileContents = getFromS3(bucket, bucketKey)
      val retrievedResult: String = Await.result(retrievedFileContents, 10.seconds)
      assert(retrievedResult == body)
    }
  }



  "S3 retrieval" must {

    val bucket = "temp-8cd36850276c"
    val bucketKey = "akka-quickstart-scala/testfile.txt"

    "stream the entire file" in {
      val s3File: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] =
        S3.download(bucket, bucketKey)

      val Some((data: Source[ByteString, _], metadata)) =
        s3File.runWith(Sink.head).futureValue

      val future: Future[String] =
        data.map(_.utf8String).runWith(Sink.head)


      val result: String = Await.result(future, 10.seconds)
      assert(result == "1234567890")
    }
  }
}
