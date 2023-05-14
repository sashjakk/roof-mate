package com.github.sashjakk.http

import cats.implicits._
import com.github.sashjakk.interval.Interval
import com.github.sashjakk.spot.book.{Booking, BookingCreate}
import com.github.sashjakk.spot.share.{Share, ShareCreate}
import com.github.sashjakk.spot.{FreeSpot, Spot, SpotCreate}
import com.github.sashjakk.user.{User, UserCreate}
import io.circe._
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps

import java.time.Instant

object Codecs {
  implicit val userCreateCodec: Codec[UserCreate] = deriveCodec
  implicit val userCodec: Codec[User] = deriveCodec

  implicit val spotCreateDecoder: Codec[SpotCreate] = deriveCodec
  implicit val spotEncoder: Codec[Spot] = deriveCodec

  implicit val shareCreateCodec: Codec[ShareCreate] = deriveCodec
  implicit val shareEncoder: Codec[Share] = deriveCodec

  implicit val intervalEncoder: Encoder[Interval] =
    interval => Json.obj(("from", interval.from.asJson), ("to", interval.to.asJson))
  implicit val intervalDecoder: Decoder[Interval] =
    cursor =>
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

  implicit val freeSpotCodec: Codec[FreeSpot] = deriveCodec

  implicit val bookingCreateCodec: Codec[BookingCreate] = deriveCodec
  implicit val bookingCodec: Codec[Booking] = deriveCodec
}
