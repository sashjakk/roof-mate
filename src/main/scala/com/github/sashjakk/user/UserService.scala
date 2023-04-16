package com.github.sashjakk.user

import cats.MonadError
import cats.syntax.all._

sealed trait UserError
object DuplicatePhoneNumberError extends UserError
object PersistenceError extends UserError

trait UserService[F[_]] {
  def create(user: UserCreate): F[Either[UserError, User]]
}

object UserService {
  private type UserServiceError[F[_]] = MonadError[F, Throwable]
  private object UserServiceError {
    def apply[F[_]](implicit ev: UserServiceError[F]): UserServiceError[F] = ev
  }

  def make[F[_]: UserServiceError](repo: UserRepo[F]): UserService[F] = new UserService[F] {
    override def create(user: UserCreate): F[Either[UserError, User]] = {
      repo.findByPhone(user.phone).flatMap {
        case Some(_) => UserServiceError[F].pure(DuplicatePhoneNumberError.asLeft)
        case None =>
          repo
            .create(user)
            .attempt
            .map(_.leftMap(_ => PersistenceError))
      }
    }
  }
}
