package com.github.sashjakk.interval

import com.github.sashjakk.interval.IntervalSyntax._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import java.time.Instant
import java.time.temporal.ChronoUnit

class IntervalTest extends AnyFlatSpec with EitherValues with Matchers with TableDrivenPropertyChecks {
  "Interval" should "create interval" in {
    val from = Instant.now()
    val to = Instant.now().plus(1, ChronoUnit.DAYS)

    val interval = Interval.from(from, to)
    assert(interval.isRight)
  }

  it should "fail to create if `from` is after `to`" in {
    val from = Instant.now()
    val to = Instant.now().minus(1, ChronoUnit.DAYS)

    val interval = Interval.from(from, to)
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
      (
        new Interval(from = Instant.parse("1970-01-01T00:30:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T01:00:00Z")),
        true
      ),

      // [---]
      //  [-]
      (
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:30:00Z"), to = Instant.parse("1970-01-01T01:00:00Z")),
        true
      ),

      //  [-]
      // [---]
      (
        new Interval(from = Instant.parse("1970-01-01T00:30:00Z"), to = Instant.parse("1970-01-01T01:00:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        true
      ),

      // [-]
      //    [-]
      (
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T00:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T01:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        false
      ),

      //    [-]
      // [-]
      (
        new Interval(from = Instant.parse("1970-01-01T01:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T00:30:00Z")),
        false
      )
    )

    forAll(scenarios) { _ overlaps _ shouldBe _ }
  }

  it should "check enclosing" in {
    val scenarios = Table(
      ("a", "b", "encloses"),

      // [---]
      //  [---]
      (
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T01:00:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:30:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        false
      ),

      //  [---]
      // [---]
      (
        new Interval(from = Instant.parse("1970-01-01T00:30:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T01:00:00Z")),
        false
      ),

      // [---]
      //  [-]
      (
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:30:00Z"), to = Instant.parse("1970-01-01T01:00:00Z")),
        true
      ),

      //  [-]
      // [---]
      (
        new Interval(from = Instant.parse("1970-01-01T00:30:00Z"), to = Instant.parse("1970-01-01T01:00:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        false
      ),

      // [-]
      //    [-]
      (
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T00:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T01:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        false
      ),

      //    [-]
      // [-]
      (
        new Interval(from = Instant.parse("1970-01-01T01:00:00Z"), to = Instant.parse("1970-01-01T01:30:00Z")),
        new Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T00:30:00Z")),
        false
      )
    )

    forAll(scenarios) { _ encloses _ shouldBe _ }
  }

  it should "calculate distance in seconds between intervals" in {
    val scenarios = Table(
      ("a", "b", "distance"),
      (Interval(1.h, 2.h), Interval(2.h + 1.m, 3.h), 60),
      (Interval(1.h, 2.h), Interval(3.h, 5.h), 1 * 60 * 60),
      (Interval(1.h, 2.h), Interval(1.h + 59.m, 5.h), 0)
    )

    forAll(scenarios) { _ distance _ shouldBe _ }
  }

  it should "cut out interval" in {
    val interval = Interval(0.h, 10.h)
    val busy = Interval(1.h, 2.h)

    interval.cut(busy).value should contain theSameElementsAs
      Set(Interval(0.h, 1.h), Interval(2.h, 10.h))
  }

  it should "cut out all intervals" in {
    val scenarios = Table(
      ("interval", "busy", "free"),
      (Interval(0.h, 10.h), List(Interval(0.h, 10.h)), Set.empty[Interval]),
      (Interval(0.h, 10.h), List(Interval(0.h, 2.h), Interval(2.h, 4.h), Interval(4.h, 5.h)), Set(Interval(5.h, 10.h))),
      (
        Interval(0.h, 10.h),
        List(Interval(1.h, 2.h), Interval(5.h, 6.h), Interval(6.h, 7.h)),
        Set(Interval(0.h, 1.h), Interval(2.h, 5.h), Interval(7.h, 10.h))
      ),
      (
        Interval(0.h, 10.h),
        List(Interval(4.h, 5.h), Interval(5.h + 10.m, 6.h), Interval(7.h, 10.h)),
        Set(Interval(0.h, 4.h), Interval(6.h, 7.h))
      ),
      (
        Interval(0.h, 10.h),
        List(Interval(1.h, 2.h), Interval(2.h + 20.m, 6.h)),
        Set(Interval(0.h, 1.h), Interval(2.h, 2.h + 20.m), Interval(6.h, 10.h))
      ),
      (
        Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-03T00:00:00Z")),
        List(
          Interval(from = Instant.parse("1970-01-01T00:10:00Z"), to = Instant.parse("1970-01-02T23:00:00Z")),
          Interval(from = Instant.parse("1970-01-02T23:30:00Z"), to = Instant.parse("1970-01-02T23:59:00Z"))
        ),
        Set(
          Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T00:10:00Z")),
          Interval(from = Instant.parse("1970-01-02T23:00:00Z"), to = Instant.parse("1970-01-02T23:30:00Z")),
          Interval(from = Instant.parse("1970-01-02T23:59:00Z"), to = Instant.parse("1970-01-03T00:00:00Z"))
        ),
      ),
      (
        Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-03T00:00:00Z")),
        List(
          Interval(from = Instant.parse("1970-01-01T00:10:00Z"), to = Instant.parse("1970-01-02T23:00:00Z")),
          Interval(from = Instant.parse("1970-01-02T23:10:00Z"), to = Instant.parse("1970-01-02T23:59:00Z"))
        ),
        Set(
          Interval(from = Instant.parse("1970-01-01T00:00:00Z"), to = Instant.parse("1970-01-01T00:10:00Z")),
          Interval(from = Instant.parse("1970-01-02T23:59:00Z"), to = Instant.parse("1970-01-03T00:00:00Z"))
        ),
      )
    )

    forAll(scenarios) { (interval, busy, free) =>
      interval.cutAll(busy).value should contain theSameElementsAs free
    }
  }
}
