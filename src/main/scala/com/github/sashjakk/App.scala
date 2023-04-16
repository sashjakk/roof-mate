package com.github.sashjakk

import cats.data.EitherT
import cats.effect.{IO, IOApp}
import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{ShareCreate, SpotShareRepo}
import com.github.sashjakk.spot.{SpotCreate, SpotRepo, SpotService}
import com.github.sashjakk.user.{DuplicatePhoneNumberError, PersistenceError, User, UserCreate, UserRepo, UserService}

import java.time.{LocalDateTime, ZoneOffset}

object App extends IOApp.Simple {
  val run: IO[Unit] = {
    val userRepo = UserRepo.inMemory[IO]()
    val userService = UserService.make(userRepo)

    val spotRepo = SpotRepo.inMemory[IO]()
    val spotShareRepo = SpotShareRepo.inMemory[IO]()
    val bookSpotRepo = BookingRepo.inMemory[IO]()
    val spotService = SpotService.make[IO](userRepo, spotRepo, spotShareRepo, bookSpotRepo)

    val user1 = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    val user2 = UserCreate(name = "Jenny", surname = "Penny", phone = "+37127231766")

    val now = LocalDateTime.now()

    for {
      user <- EitherT(userService.create(user1)).leftMap {
        case DuplicatePhoneNumberError => new Error("duplicate phone number")
        case PersistenceError          => new Error("failed to save")
      }.rethrowT

      spotDTO = SpotCreate("46", user.id)

      user2 <- EitherT(userService.create(user2)) .leftMap {
        case DuplicatePhoneNumberError => new Error("duplicate phone number")
        case PersistenceError          => new Error("failed to save")
      }.rethrowT

      spot <- spotService.create(spotDTO)

      share <- spotService.share(
        ShareCreate(spot.id, now.toInstant(ZoneOffset.UTC), now.plusHours(6).toInstant(ZoneOffset.UTC))
      )

      request = BookingCreate(
        share.id,
        user2.id,
        now.plusHours(1).toInstant(ZoneOffset.UTC),
        now.plusHours(4).toInstant(ZoneOffset.UTC)
      )

      result <- spotService.book(request)
      _ <- IO(println(s"result: $result"))
    } yield ()
  }
}
