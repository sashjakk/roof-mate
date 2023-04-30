package com.github.sashjakk.spot

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.github.sashjakk.interval.IntervalSyntax._
import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{Share, ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.{User, UserRepo}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.collection.mutable

class SpotServiceTest extends AsyncFlatSpec with AsyncIOSpec with Matchers {
  private def fixture =
    new {
      val john = User(id = UUID.randomUUID(), name = "John", surname = "Smith", phone = "+37127231766")
      val jenny = User(id = UUID.randomUUID(), name = "Jenny", surname = "Penny", phone = "+37166713273")
      val alma = User(id = UUID.randomUUID(), name = "Alma", surname = "Clark", phone = "+3712678987")

      val users = UserRepo.inMemory[IO](mutable.Map(john.id -> john, jenny.id -> jenny))

      val jennysSpot = Spot(id = UUID.randomUUID(), identifier = "456", userId = jenny.id)
      val almasSpot = Spot(id = UUID.randomUUID(), identifier = "567", userId = alma.id)
      val spots = SpotRepo.inMemory[IO](mutable.Map(jennysSpot.id -> jennysSpot, almasSpot.id -> almasSpot))

      val almasSharing = Share(id = UUID.randomUUID(), spotId = almasSpot.id, from = 0.h, to = 10.h)
      val sharing = SpotShareRepo.inMemory[IO](mutable.Map(almasSharing.id -> almasSharing))
      val booking = BookingRepo.inMemory[IO]()

      val service = SpotService.make(users, spots, sharing, booking)
    }

  "SpotService" should "create spot" in {
    val ctx = fixture

    val request = SpotCreate(identifier = "46", userId = ctx.john.id)
    ctx.service.create(request).asserting { response =>
      response should have(Symbol("identifier")("46"), Symbol("userId")(ctx.john.id))
    }
  }

  it should "NOT create spot with non existing user" in {
    val ctx = fixture

    val request = SpotCreate(identifier = "456", userId = UUID.randomUUID())
    ctx.service.create(request).assertThrowsWithMessage[Error]("User does not exist")
  }

  it should "NOT create spot with same identifier" in {
    val ctx = fixture

    val request = SpotCreate(identifier = "456", userId = ctx.john.id)
    ctx.service.create(request).assertThrowsWithMessage[Error]("Spot already exists")
  }

  it should "share spot" in {
    val ctx = fixture

    val request = ShareCreate(spotId = ctx.jennysSpot.id, from = 3.h, to = 5.h)

    for {
      _ <- ctx.service.share(request).assertNoException

      _ <- ctx.service.sharedSpots().asserting { free =>
        free should contain(FreeSpot(ctx.jennysSpot.id, Set(3.h -> 5.h)))
      }
    } yield ()
  }

  it should "NOT share non existing spot" in {
    val ctx = fixture

    val request = ShareCreate(spotId = UUID.randomUUID(), from = 1.h, to = 2.h)
    ctx.service.share(request).assertThrowsWithMessage[Error]("Unable to share - spot not registered")
  }

  it should "NOT share already shared spot" in {
    val ctx = fixture

    val request = ShareCreate(spotId = ctx.almasSpot.id, from = 0.h, to = 1.h)
    ctx.service.share(request).assertThrowsWithMessage[Error]("Unable to share - spot already shared")
  }

  it should "book shared spot" in {
    val ctx = fixture

    val request = BookingCreate(shareId = ctx.almasSharing.id, userId = ctx.john.id, from = 2.h, to = 4.h)

    for {
      _ <- ctx.service.book(request).asserting { response =>
        response should have(
          Symbol("userId")(ctx.john.id),
          Symbol("shareId")(ctx.almasSharing.id),
          Symbol("from")(2.h),
          Symbol("to")(4.h)
        )
      }

      _ <- ctx.service.sharedSpots().asserting { free =>
        free should contain(FreeSpot(spotId = ctx.almasSpot.id, timeSlots = Set(0.h -> 2.h, 4.h -> 10.h)))
      }
    } yield ()
  }
//
//  it should "NOT book busy spot" in {}

}
