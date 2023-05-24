package com.github.sashjakk

import cats.effect.{Clock, ExitCode, IO, IOApp}
import com.github.sashjakk.interval.IntervalSyntax._
import com.github.sashjakk.spot.book.{Booking, BookingCreate}
import com.github.sashjakk.spot.share.{Share, ShareCreate}
import com.github.sashjakk.spot.{FreeSpot, Spot, SpotCreate}
import com.github.sashjakk.user.{User, UserCreate}
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, _}

object Client extends IOApp {
  private val api = uri"http://localhost:8080"

  override def run(args: List[String]): IO[ExitCode] =
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        for {
          john <- {
            val request = Request[IO](Method.POST, api / "users")
              .withEntity(UserCreate(name = "John", surname = "Smith", phone = "+37127231766"))

            client
              .expect[User](request)
              .flatTap(IO.println)
          }

          johnsSpot <- {
            val request = Request[IO](Method.POST, api / "spots")
              .withEntity(SpotCreate("46", john.id))

            client
              .expect[Spot](request)
              .flatTap(IO.println)
          }

          now <- Clock[IO].realTimeInstant

          johnsSpotShare <- {
            val request =
              Request[IO](Method.POST, api / "spots" / "share")
                .withEntity(ShareCreate(johnsSpot.id, now, now + 6.h))

            client
              .expect[Share](request)
              .flatTap(IO.println)
          }

          _ <- client
            .expect[List[FreeSpot]](api / "spots")
            .flatTap(IO.println)

          jenny <- {
            val request = Request[IO](Method.POST, api / "users")
              .withEntity(UserCreate(name = "Jenny", surname = "Penny", phone = "+37166713273"))

            client
              .expect[User](request)
              .flatTap(IO.println)
          }

          _ <- {
            val request = Request[IO](Method.POST, api / "spots" / "book")
              .withEntity(BookingCreate(johnsSpotShare.id, jenny.id, now + 1.h, now + 4.h))

            client
              .expect[Booking](request)
              .flatTap(IO.println)
          }

          _ <- client
            .expect[List[FreeSpot]](api / "spots")
            .flatTap(IO.println)
        } yield ()
      }
      .as(ExitCode.Success)
}
