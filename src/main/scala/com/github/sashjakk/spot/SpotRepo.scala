package com.github.sashjakk.spot

import cats.implicits.{catsSyntaxEitherId, catsSyntaxEq}

import java.util.UUID
import scala.collection.mutable

trait SpotRepo {
  def create(spot: SpotCreate): Either[Throwable, Spot]
  def findById(id: UUID): Option[Spot]
  def findByIdentifier(identifier: String): Option[Spot]
}

object SpotRepo {
  def inMemory(memory: mutable.Map[UUID, Spot] = mutable.Map.empty): SpotRepo =
    new SpotRepo {
      override def create(spot: SpotCreate): Either[Throwable, Spot] = {
        val uuid = UUID.randomUUID()
        val entity = Spot(uuid, spot.identifier, spot.userId)

        memory += uuid -> entity

        entity.asRight
      }

      override def findById(id: UUID): Option[Spot] = memory.get(id)

      override def findByIdentifier(identifier: String): Option[Spot] =
        memory.values.find(_.identifier === identifier)
    }
}
