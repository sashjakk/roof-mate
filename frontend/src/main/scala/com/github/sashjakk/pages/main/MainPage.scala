package com.github.sashjakk.pages.main

import com.github.sashjakk.{AppCommand, Ready}
import com.raquo.laminar.api.L._

object MainPage {
  def apply(state: Ready, commands: Observer[AppCommand]): Element = {
    div(
      cls := "columns",
      div(
        cls := "column",
        h3(s"Hey, ${state.user.name}"),
        div(cls := "columns", div(cls := "column", CreateSpot(state.user, commands), SpotList(state.spots)))
      )
    )
  }
}
