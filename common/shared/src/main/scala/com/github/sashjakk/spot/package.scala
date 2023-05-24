package com.github.sashjakk

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

package object spot {
  implicit val spotCreateDecoder: Codec[SpotCreate] = deriveCodec
  implicit val spotEncoder: Codec[Spot] = deriveCodec

  implicit val freeSpotCodec: Codec[FreeSpot] = deriveCodec
}
