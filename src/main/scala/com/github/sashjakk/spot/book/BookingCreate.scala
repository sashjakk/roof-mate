package com.github.sashjakk.spot.book

import java.time.Instant
import java.util.UUID

final case class BookingCreate(shareId: UUID, userId: UUID, from: Instant, to: Instant)
