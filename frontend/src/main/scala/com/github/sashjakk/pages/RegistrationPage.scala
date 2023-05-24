package com.github.sashjakk.pages

import com.github.sashjakk.components.Field
import com.github.sashjakk.user.{User, UserCreate}
import com.github.sashjakk.{AppCommand, UserRegistered}
import com.raquo.laminar.api.L._
import io.laminext.fetch.Fetch
import io.laminext.fetch.circe._
import io.laminext.fetch.circe.jsonRequestBody
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

object RegistrationPage {
  def apply(commands: Observer[AppCommand]): Element = {
    val user = Var(initial = UserCreate("", "", ""))

    val name = user.updater[String]((user, name) => user.copy(name = name))
    val surname = user.updater[String]((user, surname) => user.copy(surname = surname))
    val phone = user.updater[String]((user, phone) => user.copy(phone = phone))

    def create: EventStream[AppCommand] = {
      Fetch
        .post("api/users", body = jsonRequestBody(user.now()))
        .decode[User]
        .map(it => UserRegistered(it.data))
    }

    div(
      cls := "container is-fluid",
      div(
        cls := "columns",
        div(cls := "column"),
        div(
          cls := "column is-three-quarters",
          h3(cls := "title is-3", "Registration"),
          Field("Name", name),
          Field("Surname", surname),
          Field("Phone", phone),
          Field("Password", Observer.empty),
          div(
            cls := "field is-grouped",
            div(
              cls := "control",
              button(cls := "button is-link", "Submit", onClick.flatMapStream { _ => create } --> commands)
            )
          )
        ),
        div(cls := "column")
      )
    )
  }
}
