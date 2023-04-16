package com.github.sashjakk.spot

import cats.effect.Sync
import cats.implicits.catsSyntaxEq

import java.util.UUID
import scala.collection.mutable

trait SpotRepo[F[_]] {
  def create(spot: SpotCreate): F[Spot]
  def findById(id: UUID): F[Option[Spot]]
  def findByIdentifier(identifier: String): F[Option[Spot]]
}

object SpotRepo {
  def inMemory[F[_]: Sync](memory: mutable.Map[UUID, Spot] = mutable.Map.empty): SpotRepo[F] =
    new SpotRepo[F] {
      override def create(spot: SpotCreate): F[Spot] =
        Sync[F].delay {
          val uuid = UUID.randomUUID()
          val entity = Spot(uuid, spot.identifier, spot.userId)

          memory += uuid -> entity

          entity
        }

      override def findById(id: UUID): F[Option[Spot]] =
        Sync[F].delay(memory.get(id))

      override def findByIdentifier(identifier: String): F[Option[Spot]] =
        Sync[F].delay(memory.values.find(_.identifier === identifier))
    }
}
