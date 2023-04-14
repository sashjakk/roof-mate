package com.github.sashjakk.spot.book

import cats.implicits.{catsSyntaxEitherId, catsSyntaxEq}

import java.util.UUID
import scala.collection.mutable

trait BookingRepo {
  def create(booking: BookingCreate): Either[Throwable, Booking]
  def findByShareId(id: UUID): List[Booking]
}

object BookingRepo {
  def inMemory(memory: mutable.Map[UUID, Booking] = mutable.Map.empty): BookingRepo =
    new BookingRepo {
      override def create(booking: BookingCreate): Either[Throwable, Booking] = {
        val uuid = UUID.randomUUID()
        val entity = Booking(uuid, booking.shareId, booking.userId, booking.from, booking.to)

        memory += uuid -> entity

        entity.asRight
      }

      override def findByShareId(id: UUID): List[Booking] =
        memory.values.filter(_.shareId === id).toList
    }
}
