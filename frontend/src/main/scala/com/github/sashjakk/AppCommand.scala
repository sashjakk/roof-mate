package com.github.sashjakk

import com.github.sashjakk.spot.Spot
import com.github.sashjakk.spot.share.Share
import com.github.sashjakk.user.User

sealed trait AppCommand

case class Authorized(user: User) extends AppCommand
case class SpotCreated(spot: Spot) extends AppCommand
case class SpotShared(share: Share) extends AppCommand
