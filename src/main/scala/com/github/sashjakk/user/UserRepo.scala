package com.github.sashjakk.user

import cats.implicits.{catsSyntaxEitherId, catsSyntaxEq}

import java.util.UUID
import scala.collection.mutable

trait UserRepo {
  def create(user: UserCreate): Either[Throwable, User]
  def findById(id: UUID): Option[User]
  def findByPhone(phone: String): Option[User]
}

object UserRepo {
  def inMemory(memory: mutable.Map[UUID, User] = mutable.Map.empty): UserRepo =
    new UserRepo {
      override def create(user: UserCreate): Either[Throwable, User] = {
        val uuid = UUID.randomUUID()
        val entity = User(uuid, user.name, user.surname, user.phone)

        memory += uuid -> entity

        entity.asRight
      }

      override def findById(id: UUID): Option[User] = memory.get(id)

      override def findByPhone(phone: String): Option[User] = memory.values.find(_.phone === phone)
    }
}
