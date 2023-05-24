package com.github.sashjakk.pages.main

import com.github.sashjakk.components.Field
import com.github.sashjakk.spot.{SpotCreate, _}
import com.github.sashjakk.user.User
import com.github.sashjakk.{AppCommand, SpotCreated}
import com.raquo.laminar.api.L._
import io.laminext.fetch.Fetch
import io.laminext.fetch.circe._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

object CreateSpot {
  def apply(user: User, commands: Observer[AppCommand]): Element = {
    val spot = Var(initial = SpotCreate("", user.id))
    val identifier = spot.updater[String]((spot, identifier) => spot.copy(identifier = identifier))

    def create: EventStream[AppCommand] = {
      Fetch
        .post("api/spots", body = jsonRequestBody(spot.now()))
        .decode[Spot]
        .map(it => SpotCreated(it.data))
    }

    div(
      cls := "column",
      h3(cls := "title is-3", "Create spot"),
      Field("Spot identifier", identifier),
      div(
        cls := "field is-grouped",
        div(
          cls := "control",
          button(cls := "button is-link", "Create", onClick.flatMapStream { _ => create } --> commands)
        )
      )
    )
  }
}
