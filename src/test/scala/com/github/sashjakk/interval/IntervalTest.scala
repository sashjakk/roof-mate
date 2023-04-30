package com.github.sashjakk.interval

import com.github.sashjakk.interval.IntervalSyntax._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import java.time.Instant
import java.time.temporal.ChronoUnit

class IntervalTest extends AnyFlatSpec with EitherValues with Matchers with TableDrivenPropertyChecks {
  type ErrOr[A] = Either[Throwable, A]

  "Interval" should "create interval" in {
    val from = Instant.now()
    val to = Instant.now().plus(1, ChronoUnit.DAYS)

    val interval = Interval.from[ErrOr](from, to)
    assert(interval.isRight)
  }

  it should "fail to create if `from` is after `to`" in {
    val from = Instant.now()
    val to = Instant.now().minus(1, ChronoUnit.DAYS)

    val interval = Interval.from[ErrOr](from, to)
    interval.left.value.getMessage shouldBe "Invalid time range"
  }

  it should "check overlapping" in {
    val scenarios = Table(
      ("a", "b", "overlaps"),

      // [---]
      //  [---]
      (0.h -> 1.h, 30.m -> (1.h + 30.m), true),

      //  [---]
      // [---]
      (30.m -> (1.h + 30.m), 0.h -> 1.h, true),

      // [---]
      //  [-]
      (0.h -> (1.h + 30.m), 30.m -> 1.h, true),

      //  [-]
      // [---]
      (30.m -> 1.h, 0.h -> 30.m, true),

      // [-]
      //    [-]
      (0.h -> 30.m, 1.h -> (1.h + 30.m), false),

      //    [-]
      // [-]
      (1.h -> (1.h + 30.m), 0.h -> 30.m, false)
    )

    forAll(scenarios) { _ overlaps _ shouldBe _ }
  }

  it should "check enclosing" in {
    val scenarios = Table(
      ("a", "b", "encloses"),

      // [---]
      //  [---]
      (0.h -> 1.h, 30.m -> (1.h + 30.m), false),

      //  [---]
      // [---]
      (30.m -> (1.h + 30.m), 0.h -> 1.h, false),

      // [---]
      //  [-]
      (0.h -> (1.h + 30.m), 30.m -> 1.h, true),

      //  [-]
      // [---]
      (30.m -> 1.h, 0.h -> 30.m, false),

      // [-]
      //    [-]
      (0.h -> 30.m, 1.h -> (1.h + 30.m), false),

      //    [-]
      // [-]
      (1.h -> (1.h + 30.m), 0.h -> 30.m, false)
    )

    forAll(scenarios) { _ encloses _ shouldBe _ }
  }

  it should "calculate distance in seconds between intervals" in {
    val scenarios = Table(
      ("a", "b", "distance"),
      (1.h -> 2.h, (2.h + 1.m) -> 3.h, 60),
      (1.h -> 2.h, 3.h -> 5.h, 1 * 60 * 60),
      (1.h -> 2.h, 1.h + 59.m -> 5.h, 0)
    )

    forAll(scenarios) { _ distanceTo _ shouldBe _ }
  }

  it should "cut out interval" in {
    val interval = 0.h -> 10.h
    val busy = 1.h -> 2.h

    interval.cut[ErrOr](busy).value should contain theSameElementsAs
      Set(0.h -> 1.h, 2.h -> 10.h)
  }

  it should "cut out all intervals" in {
    val scenarios = Table(
      ("interval", "busy", "free"),
      (0.h -> 10.h, List(0.h -> 10.h), Set.empty[Interval]),
      (0.h -> 10.h, List(0.h -> 2.h, 2.h -> 4.h, 4.h -> 5.h), Set(5.h -> 10.h)),
      (0.h -> 10.h, List(1.h -> 2.h, 5.h -> 6.h, 6.h -> 7.h), Set(0.h -> 1.h, 2.h -> 5.h, 7.h -> 10.h)),
      (0.h -> 10.h, List(4.h -> 5.h, (5.h + 10.m) -> 6.h, 7.h -> 10.h), Set(0.h -> 4.h, 6.h -> 7.h)),
      (0.h -> 10.h, List(1.h -> 2.h, (2.h + 20.m) -> 6.h), Set(0.h -> 1.h, 2.h -> (2.h + 20.m), 6.h -> 10.h)),
      (
        0.h -> 3.d,
        List(10.m -> (2.d + 23.h), (2.d + 23.h + 30.m) -> (2.d + 23.h + 59.m)),
        Set(0.m -> 10.m, (2.d + 23.h) -> (2.d + 23.h + 30.m), (2.d + 23.h + 59.m) -> 3.d),
      ),
      (
        0.h -> 3.d,
        List(10.m -> (2.d + 23.h), (2.d + 23.h + 10.m) -> (2.d + 23.h + 59.m)),
        Set(0.m -> 10.m, (2.d + 23.h + 59.m) -> 3.d),
      )
    )

    forAll(scenarios) { (interval, busy, free) =>
      interval.cutAll[ErrOr](busy).value should contain theSameElementsAs free
    }
  }
}
