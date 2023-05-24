package com.github.sashjakk.spot

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

package object share {
  implicit val shareCreateCodec: Codec[ShareCreate] = deriveCodec
  implicit val shareEncoder: Codec[Share] = deriveCodec
}
