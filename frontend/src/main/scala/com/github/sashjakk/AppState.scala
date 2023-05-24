package com.github.sashjakk

import com.github.sashjakk.spot.Spot
import com.github.sashjakk.user.User

sealed trait AppState

case class Unauthorized() extends AppState
case class Ready(user: User, spots: List[Spot] = List.empty) extends AppState
