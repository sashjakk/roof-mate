package com.github.sashjakk.interval

import cats.MonadThrow
import cats.syntax.all._
import com.github.sashjakk.interval.IntervalSyntax._

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.collection.immutable.SortedSet

case class Interval private[interval] (from: Instant, to: Instant) {
  def overlaps(other: Interval): Boolean =
    from <= other.to && to >= other.from

  def encloses(other: Interval): Boolean =
    other.from >= from && other.to <= to

  def distanceTo(other: Interval): Long =
    Math.max(other.from.getEpochSecond - to.getEpochSecond, 0)

  def duration: Long =
    Math.max(to.getEpochSecond - from.getEpochSecond, 0)

  def unionWith(other: Interval, minDistance: Long = 15 * 60): Set[Interval] =
    if (distanceTo(other) > minDistance) Set(this, other)
    else Set(Interval(from, other.to))

  def cut[F[_]: MonadThrow](other: Interval): F[Set[Interval]] = {
    if (encloses(other)) {
      (Interval.from(from, other.from), Interval.from(other.to, to))
        .mapN((a, b) => Set(a, b).filter(_.duration > 0))
    } else
      MonadThrow[F].raiseError(new Error("Unable to cut with non enclosing interval"))
  }

  def cutAll[F[_]: MonadThrow](intervals: List[Interval], minDistance: Long = 15 * 60): F[Set[Interval]] = {
    type In = (List[Interval], SortedSet[Interval])
    type Out = SortedSet[Interval]

    val combined = intervals
      .foldRight(SortedSet.empty[Interval]) { (it, acc) =>
        if (acc.isEmpty) acc + it
        else acc.tail ++ it.unionWith(acc.head, minDistance)
      }
      .toList

    MonadThrow[F]
      .tailRecM[In, Out]((combined, SortedSet(this))) { case (intervals, slots) =>
        intervals match {
          case Nil => slots.asRight[In].pure[F]
          case it :: tail =>
            slots.find(_ encloses it) match {
              case None => (tail, slots).asLeft[Out].pure[F]
              case Some(target) =>
                target.cut[F](it).redeem(_ => slots.asRight[In], free => (tail, slots - target ++ free).asLeft[Out])
            }
        }
      }
      .map(_.toSet)
  }

  override def toString: String = s"$from -> $to"
}

object Interval {
  private[interval] def apply(from: Instant, to: Instant): Interval =
    new Interval(from, to)

  def from[F[_]: MonadThrow](
    from: LocalDateTime,
    to: LocalDateTime,
    zoneOffset: ZoneOffset = ZoneOffset.UTC
  ): F[Interval] = {
    Interval.from(from.toInstant(zoneOffset), to.toInstant(zoneOffset))
  }

  def from[F[_]: MonadThrow](from: Instant, to: Instant): F[Interval] = {
    if (!from.isAfter(to) && (to.getEpochSecond - from.getEpochSecond) >= 0)
      Interval(from, to).pure
    else
      MonadThrow[F].raiseError(new Error("Invalid time range"))
  }

  implicit def orderingByFromValue: Ordering[Interval] =
    Ordering.by(_.from)
}
