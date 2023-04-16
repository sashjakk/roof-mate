package com.github.sashjakk.spot.book

import cats.effect.Sync
import cats.implicits.catsSyntaxEq

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
}
