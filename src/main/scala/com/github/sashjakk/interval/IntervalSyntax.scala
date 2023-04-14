package com.github.sashjakk.interval

import java.time.Instant

object IntervalSyntax {
  implicit class IntervalCreators(val value: Instant) extends AnyVal {
    def ->(to: Instant): Interval = Interval(value, to)
  }

  implicit class InstantComparisons(val value: Instant) extends AnyVal {
    def >=(other: Instant): Boolean = value.compareTo(other) >= 0
    def <=(other: Instant): Boolean = value.compareTo(other) <= 0
  }

  implicit class InstantOperations(val value: Instant) extends AnyVal {
    def +(other: Instant): Instant = Instant.ofEpochSecond(value.getEpochSecond + other.getEpochSecond)
    def -(other: Instant): Instant = Instant.ofEpochSecond(value.getEpochSecond - other.getEpochSecond)
  }

  implicit class InstantCreators(val value: Int) extends AnyVal {
    def h: Instant = Instant.ofEpochSecond(value * 60 * 60)
    def m: Instant = Instant.ofEpochSecond(value * 60)
    def s: Instant = Instant.ofEpochSecond(value)
  }
}
