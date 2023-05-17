package com.github.sashjakk.spot.book

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.util.UUID
import scala.collection.mutable

trait BookingRepo[F[_]] {
  def create(booking: BookingCreate): F[Booking]
  def findByShareId(id: UUID): F[List[Booking]]
}

object BookingRepo {
  def inMemory[F[_]: Sync](memory: mutable.Map[UUID, Booking] = mutable.Map.empty): BookingRepo[F] =
    new BookingRepo[F] {
      override def create(booking: BookingCreate): F[Booking] =
        Sync[F].delay {
          val uuid = UUID.randomUUID()
          val entity = Booking(uuid, booking.shareId, booking.userId, booking.from, booking.to)

          memory += uuid -> entity

          entity
        }

      override def findByShareId(id: UUID): F[List[Booking]] =
        Sync[F].delay(memory.values.filter(_.shareId === id).toList)
    }

  def postgres[F[_]: Async](transactor: Transactor[F]): BookingRepo[F] = {
    new BookingRepo[F] {
      val selectBooking = fr"select * from bookings"

      override def create(booking: BookingCreate): F[Booking] = {
        sql"""
            insert into bookings (share_id, user_id, from_timestamp, to_timestamp) values
            (${booking.shareId}, ${booking.userId}, ${booking.from}, ${booking.to})
          """.update
          .withUniqueGeneratedKeys[UUID]("id")
          .transact(transactor)
          .map(id => Booking(id, booking.shareId, booking.userId, booking.from, booking.to))
      }

      override def findByShareId(id: UUID): F[List[Booking]] = {
        (selectBooking ++ fr"where share_id = $id")
          .query[Booking]
          .to[List]
          .transact(transactor)
      }
    }
  }
}
