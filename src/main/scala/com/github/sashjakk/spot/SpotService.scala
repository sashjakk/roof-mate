package com.github.sashjakk.spot

import cats.syntax.all._
import com.github.sashjakk.interval.Interval
import com.github.sashjakk.spot.book.{Booking, BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{Share, ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.UserRepo

trait SpotService {
  def create(spot: SpotCreate): Either[Throwable, Spot]
  def sharedSpots(): List[FreeSpot]
  def share(share: ShareCreate): Either[Throwable, Share]
  def book(booking: BookingCreate): Either[Throwable, Booking]
}

object SpotService {
  def default(
    userRepo: UserRepo,
    spotRepo: SpotRepo,
    sharingRepo: SpotShareRepo,
    bookingRepo: BookingRepo
  ): SpotService =
    new SpotService {
      override def create(spot: SpotCreate): Either[Throwable, Spot] = {
        for {
          _ <- userRepo
            .findById(spot.userId)
            .toRight(new Error("User does not exist"))

          _ <- {
            val existing = spotRepo.findByIdentifier(spot.identifier)
            Either.cond(existing.isEmpty, Right(()), new Error("Spot already exists"))
          }

          spot <- spotRepo.create(spot)

        } yield spot
      }

      override def sharedSpots(): List[FreeSpot] = {
        for {
          shared <- sharingRepo.all()

          busy = bookingRepo
            .findByShareId(shared.id)
            .collect { it =>
              Interval.from(it.from, it.to) match {
                case Right(value) => value
              }
            }

          timeSlots = Interval
            .from(shared.from, shared.to)
            .flatMap(_.cutAll(busy))
            .getOrElse(Set.empty)

        } yield FreeSpot(shared.spotId, timeSlots)
      }

      override def share(share: ShareCreate): Either[Throwable, Share] = {
        for {
          spot <- spotRepo
            .findById(share.spotId)
            .toRight(new Error("Unable to share - spot not registered"))

          newShare <- sharingRepo.findBySpotId(spot.id) match {
            case Some(_) => new Error("Unable to share spot - already shared").asLeft
            case None    => sharingRepo.create(share)
          }

        } yield newShare
      }

      override def book(booking: BookingCreate): Either[Throwable, Booking] = {
        for {
          interval <- Interval.from(booking.from, booking.to)

          share <- sharingRepo
            .findById(booking.shareId)
            .toRight(new Error("Unable to book spot - spot is not shared"))

          // TODO: user check

          _ <- {
            val conflicts = bookingRepo
              .findByShareId(share.id)
              .map { it => Interval.from(it.from, it.to) }
              .exists { it => it.map(_ encloses interval).isRight }

            Either.cond(!conflicts, (), new Error("Unable to book spot - spot is already in use"))
          }

          booking <- bookingRepo.create(booking)

        } yield booking
      }
    }
}
