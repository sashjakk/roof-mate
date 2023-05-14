package com.github.sashjakk.http

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

object API {
  case class APIError(error: String)

  // TODO: use tagless final
  def withAPIError(service: HttpRoutes[IO]): HttpRoutes[IO] = {
    import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

    implicit val encoder: Encoder[APIError] = deriveEncoder

    Kleisli { request =>
      service
        .run(request)
        .handleErrorWith(e => OptionT.liftF(BadRequest(APIError(e.getMessage))))
    }
  }
}
