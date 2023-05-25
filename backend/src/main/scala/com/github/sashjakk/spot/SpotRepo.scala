package com.github.sashjakk.spot

import cats.effect._
import cats.implicits.{catsSyntaxEq, toFunctorOps}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import java.util.UUID
import scala.collection.mutable

trait SpotRepo[F[_]] {
  def create(spot: SpotCreate): F[Spot]
  def findById(id: UUID): F[Option[Spot]]
  def findByIdentifier(identifier: String): F[Option[Spot]]
  def findByUser(user: UUID): F[List[Spot]]
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

      override def findByUser(user: UUID): F[List[Spot]] =
        Sync[F].delay(memory.values.filter(_.userId === user).toList)
    }

  def postgres[F[_]: Async](transactor: Transactor[F]): SpotRepo[F] = {
    new SpotRepo[F] {
      val selectSpot = fr"select id, identifier, user_id from spots"

      override def create(spot: SpotCreate): F[Spot] = {
        sql"insert into spots (identifier, user_id) values (${spot.identifier}, ${spot.userId})".update
          .withUniqueGeneratedKeys[UUID]("id")
          .transact(transactor)
          .map(id => Spot(id, spot.identifier, spot.userId))
      }

      override def findById(id: UUID): F[Option[Spot]] = {
        (selectSpot ++ fr"where id = $id")
          .query[Spot]
          .option
          .transact(transactor)
      }

      override def findByIdentifier(identifier: String): F[Option[Spot]] = {
        (selectSpot ++ fr"where identifier = $identifier")
          .query[Spot]
          .option
          .transact(transactor)
      }

      override def findByUser(user: UUID): F[List[Spot]] = {
        (selectSpot ++ fr"where user_id = $user")
          .query[Spot]
          .to[List]
          .transact(transactor)
      }
    }
  }
}
