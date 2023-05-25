package com.github.sashjakk.spot

import cats.MonadThrow
import cats.data.{EitherT, OptionT}
import cats.syntax.all._
import com.github.sashjakk.interval.Interval
import com.github.sashjakk.spot.book.{Booking, BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{Share, ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.UserRepo

import java.util.UUID

trait SpotService[F[_]] {
  def create(spot: SpotCreate): F[Spot]
  def userSpots(user: UUID): F[List[Spot]]
  def sharedSpots(): F[List[FreeSpot]]
  def share(share: ShareCreate): F[Share]
  def book(booking: BookingCreate): F[Booking]
}

object SpotService {
  def make[F[_]: MonadThrow](
    userRepo: UserRepo[F],
    spotRepo: SpotRepo[F],
    sharingRepo: SpotShareRepo[F],
    bookingRepo: BookingRepo[F]
  ): SpotService[F] =
    new SpotService[F] {
      override def create(spot: SpotCreate): F[Spot] = {
        for {
          _ <- OptionT(userRepo.findById(spot.userId))
            .getOrRaise(new Error("User does not exist"))

          _ <- spotRepo
            .findByIdentifier(spot.identifier)
            .ensure(new Error("Spot already exists"))(_.isEmpty)

          spot <- spotRepo.create(spot)
        } yield spot
      }

      override def userSpots(user: UUID): F[List[Spot]] = {
        for {
          _ <- OptionT(userRepo.findById(user))
            .getOrRaise(new Error("User does not exist"))

          spots <- spotRepo.findByUser(user)
        } yield spots
      }

      override def sharedSpots(): F[List[FreeSpot]] = {
        def makeFreeSpot(shared: Share): F[FreeSpot] = {
          for {
            busy <- bookingRepo
              .findByShareId(shared.id)
              .flatMap(_.traverse(it => Interval.from(it.from, it.to)))

            timeSlots <- Interval
              .from(shared.from, shared.to)
              .flatMap(_.cutAll(busy))
          } yield FreeSpot(shared.spotId, timeSlots)
        }

        for {
          shares <- sharingRepo.all()
          spots <- shares.traverse(makeFreeSpot)
        } yield spots
      }

      override def share(share: ShareCreate): F[Share] = {
        for {
          spot <- OptionT(spotRepo.findById(share.spotId))
            .getOrRaise(new Error("Unable to share - spot not registered"))

          _ <- sharingRepo
            .findBySpotId(spot.id)
            .ensure(new Error("Unable to share - spot already shared"))(_.isEmpty)

          result <- sharingRepo.create(share)
        } yield result
      }

      override def book(booking: BookingCreate): F[Booking] = {
        for {
          share <- OptionT(sharingRepo.findById(booking.shareId))
            .getOrRaise(new Error("Unable to book spot - spot is not shared"))

          interval <- Interval.from(booking.from, booking.to)

          // TODO: user check

          busy <- bookingRepo
            .findByShareId(share.id)
            .flatMap(_.traverse(it => Interval.from(it.from, it.to)))

          conflicts = busy.exists(_ encloses interval)

          _ <- EitherT
            .cond(!conflicts, (), new Error("Unable to book spot - spot is already in use"))
            .rethrowT

          booking <- bookingRepo.create(booking)

        } yield booking
      }
    }
}
