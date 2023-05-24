package com.github.sashjakk.spot.share

import java.time.Instant
import java.util.UUID

final case class Share(id: UUID, spotId: UUID, from: Instant, to: Instant)
