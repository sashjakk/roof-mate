package com.github.sashjakk.components

import com.raquo.laminar.api.L._

object Field {
  def apply(labelText: String, value: Observer[String]): Element = {
    div(
      cls := "field",
      label(labelText, cls := "label"),
      div(
        cls := "control",
        input(cls := "input", typ := "text", placeholder := labelText, onInput.mapToValue --> value)
      )
    )
  }
}
