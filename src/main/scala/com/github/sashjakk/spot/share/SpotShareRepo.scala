package com.github.sashjakk.spot.share

import cats.implicits.{catsSyntaxEitherId, catsSyntaxEq}

import java.util.UUID
import scala.collection.mutable

trait SpotShareRepo {
  def create(share: ShareCreate): Either[Throwable, Share]
  def findById(id: UUID): Option[Share]
  def findBySpotId(id: UUID): Option[Share]
  def all(): List[Share]
}

object SpotShareRepo {
  def inMemory(memory: mutable.Map[UUID, Share] = mutable.Map.empty): SpotShareRepo =
    new SpotShareRepo {
      override def create(share: ShareCreate): Either[Throwable, Share] = {
        val uuid = UUID.randomUUID()
        val entity = Share(uuid, share.spotId, share.from, share.to)

        memory += uuid -> entity

        entity.asRight
      }

      override def findById(id: UUID): Option[Share] =
        memory.get(id)

      override def findBySpotId(id: UUID): Option[Share] =
        memory.values.find(_.spotId === id)

      override def all(): List[Share] = memory.values.toList
    }
}
