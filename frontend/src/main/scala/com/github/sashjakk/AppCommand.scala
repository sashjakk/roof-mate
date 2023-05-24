package com.github.sashjakk

import com.github.sashjakk.spot.Spot
import com.github.sashjakk.user.User

sealed trait AppCommand

case class UserRegistered(user: User) extends AppCommand
case class SpotCreated(spot: Spot) extends AppCommand
