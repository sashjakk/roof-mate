package com.github.sashjakk.pages.main

import com.github.sashjakk.spot.Spot
import com.raquo.laminar.api.L._

object SpotList {
  def apply(spots: List[Spot]): Element = {
    val spotsSignal = EventStream.fromValue(spots)

    div(
      cls := "column",
      h3(cls := "title is-3", "My spots"),
      div(
        cls := "field",
        label(cls := "label", "Spot"),
        div(
          cls := "control",
          div(
            cls := "select",
            select(children <-- spotsSignal.split(_.id) { (_, it, _) =>
              option(selected := false, it.identifier)
            })
          )
        ),
        div(
          cls := "field is-grouped",
          div(cls := "control", input(cls := "input", typ := "datetime-local")),
          div(cls := "control", input(cls := "input", typ := "datetime-local"))
        ),
        div(
          cls := "field is-grouped",
          div(cls := "control", button(cls := "button is-link", "Share")),
          div(cls := "control", button(cls := "button is-link is-light", "Cancel"))
        )
      )
    )
  }
}
