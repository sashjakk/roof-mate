package com.github.sashjakk.interval

import cats.implicits.catsSyntaxEitherId
import com.github.sashjakk.interval.IntervalSyntax._

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.annotation.tailrec
import scala.collection.immutable.SortedSet

case class Interval(from: Instant, to: Instant) {
  def overlaps(other: Interval): Boolean =
    from <= other.to && to >= other.from

  def encloses(other: Interval): Boolean =
    other.from >= from && other.to <= to

  def distance(other: Interval): Long =
    Math.max(other.from.getEpochSecond - to.getEpochSecond, 0)

  def union(other: Interval, minDistance: Long = 15 * 60): Set[Interval] =
    if (distance(other) > minDistance) Set(this, other)
    else Set(Interval(from, other.to))

  def cut(other: Interval): Either[Error, Set[Interval]] = {
    Either.cond(
      this encloses other,
      Set(Interval.from(from, other.from), Interval.from(other.to, to)).collect { case Right(value) => value },
      new Error("Unable to cut with non enclosing interval")
    )
  }

  def cutAll(intervals: List[Interval], minDistance: Long = 15 * 60): Either[Error, Set[Interval]] = {

    @tailrec
    def loop(intervals: List[Interval], slots: SortedSet[Interval] = SortedSet(this)): SortedSet[Interval] = {
      intervals match {
        case Nil => slots
        case it :: tail =>
          slots.find(_ encloses it) match {
            case None => loop(tail, slots)
            case Some(target) =>
              target.cut(it) match {
                case Left(_)     => slots
                case Right(free) => loop(tail, slots - target ++ free)
              }
          }
      }
    }

    val combined = intervals
      .foldRight(SortedSet.empty[Interval]) { (it, acc) =>
        if (acc.isEmpty) acc + it
        else acc.tail concat it.union(acc.head, minDistance)
      }
      .toList

    loop(combined).asRight
  }

  override def toString: String = s"$from -> $to"
}

object Interval {
  def apply(from: Instant, to: Instant): Interval =
    new Interval(from, to)

  def from(
    from: LocalDateTime,
    to: LocalDateTime,
    zoneOffset: ZoneOffset = ZoneOffset.UTC
  ): Either[Throwable, Interval] = {
    Interval.from(from.toInstant(zoneOffset), to.toInstant(zoneOffset))
  }

  def from(from: Instant, to: Instant): Either[Throwable, Interval] =
    Either
      .cond(
        !from.isAfter(to) && (to.getEpochSecond - from.getEpochSecond) > 0,
        new Interval(from, to),
        new Error("Invalid time range")
      )

  implicit def orderingByFromValue: Ordering[Interval] =
    Ordering.by(_.from)
}
