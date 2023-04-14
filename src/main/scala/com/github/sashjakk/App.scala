package com.github.sashjakk

import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{ShareCreate, SpotShareRepo}
import com.github.sashjakk.spot.{SpotCreate, SpotRepo, SpotService}
import com.github.sashjakk.user.{UserCreate, UserRepo, UserService}

import java.time.{LocalDateTime, ZoneOffset}

object App {
  def main(args: Array[String]): Unit = {
    val userRepo = UserRepo.inMemory()
    val userService = UserService.default(userRepo)

    val spotRepo = SpotRepo.inMemory()
    val spotShareRepo = SpotShareRepo.inMemory()
    val bookSpotRepo = BookingRepo.inMemory()
    val spotService = SpotService.default(userRepo, spotRepo, spotShareRepo, bookSpotRepo)

    val user1 = UserCreate(name = "John", surname = "Smith", phone = "+37127231766")
    val user2 = UserCreate(name = "Jenny", surname = "Penny", phone = "+37166713273")

    val now = LocalDateTime.now()

    val result = for {
      user <- userService.create(user1)
      user2 <- userService.create(user2)
      spotDTO = SpotCreate("46", user.id)

      spot <- spotService.create(spotDTO)

      share <- spotService.share(
        ShareCreate(spot.id, now.toInstant(ZoneOffset.UTC), now.plusHours(6).toInstant(ZoneOffset.UTC))
      )

      booking = BookingCreate(
        share.id,
        user2.id,
        now.plusHours(1).toInstant(ZoneOffset.UTC),
        now.plusHours(4).toInstant(ZoneOffset.UTC)
      )

      x <- spotService.book(booking)
    } yield x

    println(s"result $result")
  }
}
