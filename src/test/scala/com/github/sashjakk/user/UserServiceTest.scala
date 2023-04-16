package com.github.sashjakk.user

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class UserServiceTest extends AsyncFlatSpec with AsyncIOSpec with EitherValues with Matchers {
  "UserService" should "create user" in {
    val repo = UserRepo.inMemory[IO]()
    val service = UserService.make[IO](repo)

    val request = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    service.create(request).asserting { result =>
      result.isRight shouldBe true
      result.value.name shouldBe "John"
      result.value.surname shouldBe "Smith"
      result.value.phone shouldBe "+37127231766"
    }
  }

  it should "NOT create user with existing phone number" in {
    val repo = UserRepo.inMemory[IO]()
    val service = UserService.make[IO](repo)

    val existing = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    val failed = UserCreate(name = "Jenny", surname = "Smith", phone = "+37127231766")

    for {
      _ <- service
        .create(existing)
        .asserting(_.isRight shouldBe true)

      _ <- service
        .create(failed)
        .asserting(_.left.value shouldBe a[DuplicatePhoneNumberError.type])
    } yield ()
  }
}
