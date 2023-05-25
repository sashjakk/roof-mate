package com.github.sashjakk

import cats.effect.std.Random
import cats.effect.{Clock, ExitCode, IO, IOApp}
import com.github.sashjakk.interval.IntervalSyntax._
import com.github.sashjakk.spot.book.{Booking, BookingCreate}
import com.github.sashjakk.spot.share.{Share, ShareCreate}
import com.github.sashjakk.spot.{FreeSpot, Spot, SpotCreate}
import com.github.sashjakk.user.{User, UserCreate}
import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, _}

object Client extends IOApp {
  private val api = uri"http://localhost:8080/api"

  private def printStage(description: String, data: String): IO[Unit] =
    IO.println(s"\n--\n$description\n$data")

  override def run(args: List[String]): IO[ExitCode] =
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        for {
          randomizer <- Random.scalaUtilRandom[IO]

          phone1 <- randomizer.nextInt.map(it => s"+371$it")
          name1 <- randomizer.nextString(8)
          surname1 <- randomizer.nextString(10)
          user1 <- {
            val request = Request[IO](Method.POST, api / "users" / "create")
              .withEntity(UserCreate(name = name1, surname = surname1, phone = phone1))

            client
              .expect[User](request)
              .flatTap(it => printStage("Create user 1", it.asJson.spaces2))
          }

          user1SpotIdentifier <- randomizer.nextIntBounded(200)
          user1Spot <- {
            val request = Request[IO](Method.POST, api / "spots")
              .withEntity(SpotCreate(user1SpotIdentifier.toString, user1.id))

            client
              .expect[Spot](request)
              .flatTap(it => printStage("User 1 registers parking spot", it.asJson.spaces2))
          }

          now <- Clock[IO].realTimeInstant

          user1SpotShare <- {
            val request =
              Request[IO](Method.POST, api / "spots" / "share")
                .withEntity(ShareCreate(user1Spot.id, now, now + 6.h))

            client
              .expect[Share](request)
              .flatTap(it => printStage("User 1 shares spots for time period", it.asJson.spaces2))
          }

          _ <- client
            .expect[List[FreeSpot]](api / "spots")
            .flatTap(it => printStage("Available spots now", it.asJson.spaces2))

          phone2 <- randomizer.nextInt.map(it => s"+371$it")
          name2 <- randomizer.nextString(8)
          surname2 <- randomizer.nextString(10)
          user2 <- {
            val request = Request[IO](Method.POST, api / "users" / "create")
              .withEntity(UserCreate(name = name2, surname = surname2, phone = phone2))

            client
              .expect[User](request)
              .flatTap(it => printStage("Create user 2", it.asJson.spaces2))
          }

          _ <- {
            val request = Request[IO](Method.POST, api / "spots" / "book")
              .withEntity(BookingCreate(user1SpotShare.id, user2.id, now + 1.h, now + 4.h))

            client
              .expect[Booking](request)
              .flatTap(it => printStage("User 2 books spot", it.asJson.spaces2))
          }

          _ <- client
            .expect[List[FreeSpot]](api / "spots")
            .flatTap(it => printStage("Available spots now", it.asJson.spaces2))
        } yield ()
      }
      .as(ExitCode.Success)
}
