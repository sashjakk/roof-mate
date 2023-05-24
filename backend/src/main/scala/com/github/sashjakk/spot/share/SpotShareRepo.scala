package com.github.sashjakk.spot.share

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

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

  def postgres[F[_]: Async](transactor: Transactor[F]): SpotShareRepo[F] = {

    new SpotShareRepo[F] {
      val selectShare = fr"select id, spot_id, from_timestamp, to_timestamp from shares"

      override def create(share: ShareCreate): F[Share] = {
        sql"""
            insert into shares (spot_id, from_timestamp, to_timestamp) values
            (${share.spotId}, ${share.from}, ${share.to})
          """.update
          .withUniqueGeneratedKeys[UUID]("id")
          .transact(transactor)
          .map(id => Share(id, share.spotId, share.from, share.to))
      }

      override def findById(id: UUID): F[Option[Share]] = {
        (selectShare ++ fr"where id = $id")
          .query[Share]
          .option
          .transact(transactor)
      }

      override def findBySpotId(id: UUID): F[Option[Share]] = {
        (selectShare ++ fr"where spot_id = $id")
          .query[Share]
          .option
          .transact(transactor)
      }

      override def all(): F[List[Share]] = {
        selectShare
          .query[Share]
          .to[List]
          .transact(transactor)
      }
    }
  }
}
