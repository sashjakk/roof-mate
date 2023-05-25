package com.github.sashjakk.http

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.comcast.ip4s.IpLiteralSyntax
import com.github.sashjakk.Config.{AppConfig, DatabaseConfig}
import com.github.sashjakk.http.API._
import com.github.sashjakk.spot._
import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.{UserCreate, UserLogin, UserRepo, UserService}
import doobie.util.transactor.Transactor
import fly4s.core.Fly4s
import fly4s.core.data.{Fly4sConfig, Location}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.{Created, _}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.{FileService, fileService}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

import java.net.URI

object Server extends IOApp {
  private def users(userService: UserService[IO], spotService: SpotService[IO]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "users" / "create" =>
        request
          .as[UserCreate]
          .flatMap(userService.create)
          .flatMap(Created(_))

      case request @ POST -> Root / "users" / "login" =>
        request
          .as[UserLogin]
          .flatMap(userService.login)
          .flatMap(Ok(_))

      case GET -> Root / "users" / UUIDVar(user) / "spots" =>
        spotService
          .userSpots(user)
          .flatMap(Ok(_))
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

  private def api(config: AppConfig, transactor: Transactor[IO]): HttpRoutes[IO] = {
    val userRepo = UserRepo.postgres[IO](transactor)
    val userService = UserService.make[IO](userRepo)

    val spotRepo = SpotRepo.postgres[IO](transactor)
    val spotShareRepo = SpotShareRepo.postgres[IO](transactor)
    val bookSpotRepo = BookingRepo.postgres[IO](transactor)
    val spotService = SpotService.make[IO](userRepo, spotRepo, spotShareRepo, bookSpotRepo)

    withAPIError(users(userService, spotService) <+> spots(spotService))
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
      instance = api(config, xa)
      static = fileService[IO](FileService.Config(systemPath = "/var/www/public"))
      router = Router("/" -> static, "/api" -> instance).orNotFound

      server <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withHttpApp(router)
        .build
        .useForever

    } yield server
  }
}
