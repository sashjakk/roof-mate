package com.github.sashjakk.user

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.util.UUID
import scala.collection.mutable

trait UserRepo[F[_]] {
  def create(user: UserCreate): F[User]
  def findById(id: UUID): F[Option[User]]
  def findByPhone(phone: String): F[Option[User]]
}

object UserRepo {
  def inMemory[F[_]: Sync](memory: mutable.Map[UUID, User] = mutable.Map.empty): UserRepo[F] =
    new UserRepo[F] {
      override def create(user: UserCreate): F[User] =
        Sync[F].delay {
          val uuid = UUID.randomUUID()
          val entity = User(uuid, user.name, user.surname, user.phone)

          memory += uuid -> entity

          entity
        }

      override def findById(id: UUID): F[Option[User]] =
        Sync[F].delay { memory.get(id) }

      override def findByPhone(phone: String): F[Option[User]] =
        Sync[F].delay(memory.values.find(_.phone === phone))
    }

  def postgres[F[_]: Async](transactor: Transactor[F]): UserRepo[F] = {

    new UserRepo[F] {
      val selectUser = fr"select id, name, surname, phone from users"

      override def create(user: UserCreate): F[User] = {
        sql"insert into users (name, surname, phone) values (${user.name}, ${user.surname}, ${user.phone})".update
          .withUniqueGeneratedKeys[UUID]("id")
          .transact(transactor)
          .map(id => User(id, user.name, user.surname, user.phone))
      }

      override def findById(id: UUID): F[Option[User]] = {
        (selectUser ++ fr"where id = $id")
          .query[User]
          .option
          .transact(transactor)
      }

      override def findByPhone(phone: String): F[Option[User]] = {
        (selectUser ++ fr"where phone = $phone")
          .query[User]
          .option
          .transact(transactor)
      }
    }
  }
}
