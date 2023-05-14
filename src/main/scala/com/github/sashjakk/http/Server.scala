package com.github.sashjakk.http

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.github.sashjakk.http.API._
import com.github.sashjakk.http.Codecs._
import com.github.sashjakk.spot._
import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.{UserCreate, UserRepo, UserService}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.{Created, _}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.ErrorHandling

object Server extends IOApp {
  private def users(service: UserService[IO]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] { case request @ POST -> Root / "users" =>
      request
        .as[UserCreate]
        .flatMap(service.create)
        .flatMap(Created(_))
    }
  }

  private def spots(service: SpotService[IO]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "spots" =>
        service
          .sharedSpots()
          .flatMap(Ok(_))

      case request @ POST -> Root / "spots" =>
        request
          .as[SpotCreate]
          .flatMap(service.create)
          .flatMap(Created(_))

      case request @ POST -> Root / "spots" / "share" =>
        request
          .as[ShareCreate]
          .flatMap(service.share)
          .flatMap(Created(_))

      case request @ POST -> Root / "spots" / "book" =>
        request
          .as[BookingCreate]
          .flatMap(service.book)
          .flatMap(Created(_))
    }
  }

  private val app = {
    val userRepo = UserRepo.inMemory[IO]()
    val userService = UserService.make[IO](userRepo)

    val spotRepo = SpotRepo.inMemory[IO]()
    val spotShareRepo = SpotShareRepo.inMemory[IO]()
    val bookSpotRepo = BookingRepo.inMemory[IO]()
    val spotService = SpotService.make[IO](userRepo, spotRepo, spotShareRepo, bookSpotRepo)

    ErrorHandling {
      withAPIError { users(userService) <+> spots(spotService) }
    }.orNotFound
  }

  override def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHttpApp(app)
      .build
      .useForever
}
