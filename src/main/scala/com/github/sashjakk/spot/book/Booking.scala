package com.github.sashjakk.spot.book

import java.time.{Instant, LocalDateTime}
import java.util.UUID

final case class Booking(id: UUID, shareId: UUID, userId: UUID, from: Instant, to: Instant)
