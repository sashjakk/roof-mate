package com.github.sashjakk.user

import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}

sealed trait UserError
object DuplicatePhoneNumberError extends UserError
object PersistenceError extends UserError

trait UserService {
  def create(user: UserCreate): Either[UserError, User]
}

object UserService {
  def default(repo: UserRepo): UserService = new UserService {
    override def create(user: UserCreate): Either[UserError, User] = {
      repo.findByPhone(user.phone) match {
        case Some(_) => DuplicatePhoneNumberError.asLeft
        case None    => repo.create(user).leftMap(_ => PersistenceError)
      }
    }
  }
}
