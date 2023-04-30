package com.github.sashjakk.user

import cats.MonadThrow
import cats.implicits.toFlatMapOps

trait UserService[F[_]] {
  def create(user: UserCreate): F[User]
}

object UserService {
  def make[F[_]: MonadThrow](repo: UserRepo[F]): UserService[F] = new UserService[F] {
    override def create(user: UserCreate): F[User] = {
      repo.findByPhone(user.phone).flatMap {
        case Some(_) => MonadThrow[F].raiseError(new Error("Unable to create user - duplicate phone number"))
        case None    => repo.create(user)
      }
    }
  }
}
