package com.github.sashjakk.spot

import cats.data.{EitherT, OptionT}
import cats.syntax.all._
import cats.{Applicative, MonadThrow}
import com.github.sashjakk.interval.Interval
import com.github.sashjakk.spot.book.{Booking, BookingCreate, BookingRepo}
import com.github.sashjakk.spot.share.{Share, ShareCreate, SpotShareRepo}
import com.github.sashjakk.user.UserRepo

trait SpotService[F[_]] {
  def create(spot: SpotCreate): F[Spot]
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
            .flatMap {
              case Some(_) => MonadThrow[F].raiseError[Unit](new Error("Spot already exists"))
              case None    => Applicative[F].unit
            }

          spot <- spotRepo.create(spot)
        } yield spot
      }

      override def sharedSpots(): F[List[FreeSpot]] = {
        def makeFreeSpot(shared: Share): F[FreeSpot] = {
          for {
            busy <- bookingRepo
              .findByShareId(shared.id)
              .map {
                _.collect { it =>
                  Interval.from(it.from, it.to) match { case Right(value) => value }
                }
              }

            timeSlots = Interval
              .from(shared.from, shared.to)
              .flatMap(_.cutAll(busy))
              .getOrElse(Set.empty)

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
            .flatMap {
              case Some(_) => MonadThrow[F].raiseError[Unit](new Error("Unable to share - spot already shared"))
              case None    => Applicative[F].unit
            }

          result <- sharingRepo.create(share)
        } yield result
      }

      override def book(booking: BookingCreate): F[Booking] = {
        for {
          share <- OptionT(sharingRepo.findById(booking.shareId))
            .getOrRaise(new Error("Unable to book spot - spot is not shared"))

          interval <- EitherT
            .fromEither(Interval.from(booking.from, booking.to))
            .rethrowT

          // TODO: user check

          conflicts <- bookingRepo
            .findByShareId(share.id)
            .map { bookings =>
              bookings
                .collect { it => Interval.from(it.from, it.to) match { case Right(value) => value } }
                .exists { _ encloses interval }
            }

          _ <- EitherT
            .cond[F](!conflicts, Applicative[F].unit, new Error("Unable to book spot - spot is already in use"))
            .rethrowT

          booking <- bookingRepo.create(booking)

        } yield booking
      }
    }
}
