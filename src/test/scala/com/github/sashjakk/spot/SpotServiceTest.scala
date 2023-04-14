package com.github.sashjakk.spot

import com.github.sashjakk.interval.IntervalSyntax._
import com.github.sashjakk.spot.book.{BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{Share, ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.{User, UserRepo}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.collection.mutable

class SpotServiceTest extends AnyFlatSpec with EitherValues with Matchers {
  private def fixture =
    new {
      val john = User(id = UUID.randomUUID(), name = "John", surname = "Smith", phone = "+37127231766")
      val jenny = User(id = UUID.randomUUID(), name = "Jenny", surname = "Penny", phone = "+37166713273")
      val alma = User(id = UUID.randomUUID(), name = "Alma", surname = "Clark", phone = "+3712678987")

      val users = UserRepo.inMemory(mutable.Map(john.id -> john, jenny.id -> jenny))

      val jennysSpot = Spot(id = UUID.randomUUID(), identifier = "456", userId = jenny.id)
      val almasSpot = Spot(id = UUID.randomUUID(), identifier = "567", userId = alma.id)
      val spots = SpotRepo.inMemory(mutable.Map(jennysSpot.id -> jennysSpot, almasSpot.id -> almasSpot))

      val almasSharing = Share(id = UUID.randomUUID(), spotId = almasSpot.id, from = 0.h, to = 10.h)
      val sharing = SpotShareRepo.inMemory(mutable.Map(almasSharing.id -> almasSharing))
      val booking = BookingRepo.inMemory()

      val service = SpotService.default(users, spots, sharing, booking)
    }

  "SpotService" should "create spot" in {
    val ctx = fixture

    val request = SpotCreate(identifier = "46", userId = ctx.john.id)
    val response = ctx.service.create(request)

    response.isRight shouldBe true
    response.value should have(Symbol("identifier")("46"), Symbol("userId")(ctx.john.id))
  }

  it should "NOT create spot with non existing user" in {
    val ctx = fixture

    val request = SpotCreate(identifier = "456", userId = UUID.randomUUID())
    val response = ctx.service.create(request)

    response.isLeft shouldBe true
    response.left.value.getMessage shouldBe "User does not exist"
  }

  it should "NOT create spot with same identifier" in {
    val ctx = fixture

    val request = SpotCreate(identifier = "456", userId = ctx.john.id)
    val response = ctx.service.create(request)

    response.isLeft shouldBe true
    response.left.value.getMessage shouldBe "Spot already exists"
  }

  it should "share spot" in {
    val ctx = fixture

    val request = ShareCreate(spotId = ctx.jennysSpot.id, from = 3.h, to = 5.h)
    val response = ctx.service.share(request)
    response.isRight shouldBe true

    val free = ctx.service.sharedSpots()
    free should contain(FreeSpot(ctx.jennysSpot.id, Set(3.h -> 5.h)))
  }

  it should "NOT share non existing spot" in {
    val ctx = fixture

    val request = ShareCreate(spotId = UUID.randomUUID(), from = 1.h, to = 2.h)
    val response = ctx.service.share(request)

    response.isLeft shouldBe true
    response.left.value.getMessage shouldBe "Unable to share - spot not registered"
  }

  it should "NOT share already shared spot" in {
    val ctx = fixture

    val request = ShareCreate(spotId = ctx.almasSpot.id, from = 0.h, to = 1.h)
    val response = ctx.service.share(request)

    response.isLeft shouldBe true
    response.left.value.getMessage shouldBe "Unable to share spot - already shared"
  }

  it should "book shared spot" in {
    val ctx = fixture

    val request = BookingCreate(shareId = ctx.almasSharing.id, userId = ctx.john.id, from = 2.h, to = 4.h)
    val response = ctx.service.book(request)
    response.isRight shouldBe true
    response.value should have(
      Symbol("userId")(ctx.john.id),
      Symbol("shareId")(ctx.almasSharing.id),
      Symbol("from")(2.h),
      Symbol("to")(4.h)
    )

    val free = ctx.service.sharedSpots()
    free should contain(FreeSpot(spotId = ctx.almasSpot.id, timeSlots = Set(0.h -> 2.h, 4.h -> 10.h)))
  }
//
//  it should "NOT book busy spot" in {}

}
