package com.github.sashjakk.user

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserServiceTest extends AnyFlatSpec with EitherValues with Matchers {
  "UserService" should "create user" in {
    val repo = UserRepo.inMemory()
    val service = UserService.default(repo)

    val request = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    val result = service.create(request)

    result.isRight shouldBe true
    result.value.name shouldBe "John"
    result.value.surname shouldBe "Smith"
    result.value.phone shouldBe "+37127231766"
  }

  it should "NOT create user with existing phone number" in {
    val repo = UserRepo.inMemory()
    val service = UserService.default(repo)

    val existing = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    service.create(existing).isRight shouldBe true

    val failed = UserCreate(name = "Jenny", surname = "Smith", phone = "+37127231766")
    service.create(failed).left.value shouldBe a[DuplicatePhoneNumberError.type]
  }
}
