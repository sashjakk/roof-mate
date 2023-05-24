package com.github.sashjakk

import cats.effect.{Clock, IO, IOApp}
import com.github.sashjakk.interval.IntervalSyntax.{InstantCreators, InstantOperations}
import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{ShareCreate, SpotShareRepo}
import com.github.sashjakk.spot.{SpotCreate, SpotRepo, SpotService}
import com.github.sashjakk.user.{UserCreate, UserRepo, UserService}

object App extends IOApp.Simple {
  val run: IO[Unit] = {
    val userRepo = UserRepo.inMemory[IO]()
    val userService = UserService.make[IO](userRepo)

    val spotRepo = SpotRepo.inMemory[IO]()
    val spotShareRepo = SpotShareRepo.inMemory[IO]()
    val bookSpotRepo = BookingRepo.inMemory[IO]()
    val spotService = SpotService.make[IO](userRepo, spotRepo, spotShareRepo, bookSpotRepo)

    val user1 = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    val user2 = UserCreate(name = "Jenny", surname = "Penny", phone = "+37166713273")

    for {
      user <- userService.create(user1)
      spotDTO = SpotCreate("46", user.id)

      user2 <- userService.create(user2)
      spot <- spotService.create(spotDTO)

      now <- Clock[IO].realTimeInstant

      share <- spotService.share(ShareCreate(spot.id, now, now + 6.h))
      request = BookingCreate(share.id, user2.id, now + 1.h, now + 4.h)

      result <- spotService.book(request)
      _ <- IO(println(s"result: $result"))
    } yield ()
  }
}
