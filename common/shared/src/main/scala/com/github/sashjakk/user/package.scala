package com.github.sashjakk

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

package object user {
  implicit val userCreateCodec: Codec[UserCreate] = deriveCodec
  implicit val userLoginCodec: Codec[UserLogin] = deriveCodec
  implicit val userCodec: Codec[User] = deriveCodec
}
