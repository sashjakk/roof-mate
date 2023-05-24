package com.github.sashjakk

import cats.implicits.catsSyntaxEitherId
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import java.time.Instant

package object interval {
  implicit val intervalEncoder: Encoder[Interval] = interval =>
    Json.obj(("from", interval.from.asJson), ("to", interval.to.asJson))

  implicit val intervalDecoder: Decoder[Interval] = cursor =>
    for {
      from <- cursor.downField("from").as[Instant]
      to <- cursor.downField("to").as[Instant]
      interval <- {
        type ErrOr[A] = Either[Throwable, A]

        // TODO: looks like extra mapping, need a way to convert to DecodingFailure directly
        Interval.from[ErrOr](from, to) match {
          case Right(value) => value.asRight[DecodingFailure]
          case Left(value)  => DecodingFailure.fromThrowable(value, List.empty).asLeft[Interval]
        }
      }
    } yield interval
}
