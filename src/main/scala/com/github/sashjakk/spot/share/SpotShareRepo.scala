package com.github.sashjakk.spot.share

import cats.effect.Sync
import cats.implicits.catsSyntaxEq

import java.util.UUID
import scala.collection.mutable

trait SpotShareRepo[F[_]] {
  def create(share: ShareCreate): F[Share]
  def findById(id: UUID): F[Option[Share]]
  def findBySpotId(id: UUID): F[Option[Share]]
  def all(): F[List[Share]]
}

object SpotShareRepo {
  def inMemory[F[_]: Sync](memory: mutable.Map[UUID, Share] = mutable.Map.empty): SpotShareRepo[F] =
    new SpotShareRepo[F] {
      override def create(share: ShareCreate): F[Share] =
        Sync[F].delay {
          val uuid = UUID.randomUUID()
          val entity = Share(uuid, share.spotId, share.from, share.to)

          memory += uuid -> entity

          entity
        }

      override def findById(id: UUID): F[Option[Share]] =
        Sync[F].delay(memory.get(id))

      override def findBySpotId(id: UUID): F[Option[Share]] =
        Sync[F].delay(memory.values.find(_.spotId === id))

      override def all(): F[List[Share]] = Sync[F].delay(memory.values.toList)
    }
}
