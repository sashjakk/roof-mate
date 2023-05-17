package com.github.sashjakk.http

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.comcast.ip4s.IpLiteralSyntax
import com.github.sashjakk.Config.{AppConfig, DatabaseConfig}
import com.github.sashjakk.http.API._
import com.github.sashjakk.http.Codecs._
import com.github.sashjakk.spot._
import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.{UserCreate, UserRepo, UserService}
import doobie.util.transactor.Transactor
import fly4s.core.Fly4s
import fly4s.core.data.{Fly4sConfig, Location}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.{Created, _}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.ErrorHandling
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

import java.net.URI

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

  private def transactor(database: DatabaseConfig): Transactor[IO] = {
    Transactor
      .fromDriverManager[IO]("org.postgresql.Driver", database.url, database.user, database.password)
  }

  private def app(config: AppConfig, transactor: Transactor[IO]): HttpApp[IO] = {
    val userRepo = UserRepo.postgres[IO](transactor)
    val userService = UserService.make[IO](userRepo)

    val spotRepo = SpotRepo.postgres[IO](transactor)
    val spotShareRepo = SpotShareRepo.postgres[IO](transactor)
    val bookSpotRepo = BookingRepo.postgres[IO](transactor)
    val spotService = SpotService.make[IO](userRepo, spotRepo, spotShareRepo, bookSpotRepo)

    ErrorHandling { withAPIError(users(userService) <+> spots(spotService)) }.orNotFound
  }

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- {
        ConfigSource.default
          .loadF[IO, AppConfig]()
          .map { it =>
            val raw = URI.create(it.database.url)

            val name = raw.getPath.substring(1)
            val url = s"jdbc:postgresql://${raw.getHost}:${raw.getPort}${raw.getPath}?${raw.getQuery}"
            val user = raw.getUserInfo.split(":")(0)
            val password = raw.getUserInfo.split(":")(1)

            it.copy(database = DatabaseConfig(url, name, user, password))
          }
          .flatTap(IO.println)
      }

      _ <- {
        Fly4s
          .make[IO](
            url = config.database.url,
            user = config.database.user.some,
            password = config.database.password.toCharArray.some,
            config = Fly4sConfig(locations = Location.of("db"))
          )
          .use(_.migrate)
          .ensureOr(_ => new Error("Database migration failure"))(_.success)
      }

      xa = transactor(config.database)
      instance = app(config, xa)

      server <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withHttpApp(instance)
        .build
        .useForever

    } yield server
  }
}
