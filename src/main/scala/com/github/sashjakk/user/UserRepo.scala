package com.github.sashjakk.user

import cats.effect.Sync
import cats.implicits.catsSyntaxEq

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
}
