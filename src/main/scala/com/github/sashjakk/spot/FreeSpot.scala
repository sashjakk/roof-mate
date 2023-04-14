package com.github.sashjakk.spot

import com.github.sashjakk.interval.Interval

import java.util.UUID

final case class FreeSpot(spotId: UUID, timeSlots: Set[Interval])
