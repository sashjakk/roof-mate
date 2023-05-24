package com.github.sashjakk.spot

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

package object book {
  implicit val bookingCreateCodec: Codec[BookingCreate] = deriveCodec
  implicit val bookingCodec: Codec[Booking] = deriveCodec
}
