package com.hdq

import akka.NotUsed
import akka.stream.alpakka.s3.ObjectMetadata
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import akka.util.ByteString

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
