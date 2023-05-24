package sashjakk.user

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.github.sashjakk.user.{UserCreate, UserRepo, UserService}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class UserServiceTest extends AsyncFlatSpec with AsyncIOSpec with Matchers {
  "UserService" should "create user" in {
    val repo = UserRepo.inMemory[IO]()
    val service = UserService.make[IO](repo)

    val request = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    service.create(request).asserting { result =>
      result.name shouldBe "John"
      result.surname shouldBe "Smith"
      result.phone shouldBe "+37127231766"
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
        .assertNoException

      _ <- service
        .create(failed)
        .assertThrowsWithMessage[Error]("Unable to create user - duplicate phone number")
    } yield ()
  }
}
