package com.github.sashjakk.spot.share

import java.time.Instant
import java.util.UUID

final case class ShareCreate(spotId: UUID, from: Instant, to: Instant)
