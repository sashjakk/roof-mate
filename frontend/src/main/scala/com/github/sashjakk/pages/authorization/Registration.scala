package com.github.sashjakk.pages.authorization

import com.github.sashjakk.components.Field
import com.github.sashjakk.user.{User, UserCreate}
import com.github.sashjakk.{AppCommand, Authorized}
import com.raquo.laminar.api.L._
import io.laminext.fetch.Fetch
import io.laminext.fetch.circe._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

object Registration {
  def apply(commands: Observer[AppCommand]): Element = {
    val user = Var(initial = UserCreate("", "", ""))

    val name = user.updater[String]((user, name) => user.copy(name = name))
    val surname = user.updater[String]((user, surname) => user.copy(surname = surname))
    val phone = user.updater[String]((user, phone) => user.copy(phone = phone))

    def create: EventStream[AppCommand] = {
      Fetch
        .post("api/users/create", body = jsonRequestBody(user.now()))
        .decode[User]
        .map(it => Authorized(it.data))
    }

    div(
      cls("container"),
      div(
        cls := "column is-half is-offset-one-quarter",
        Field("Name", name),
        Field("Surname", surname),
        Field("Phone", phone),
        Field("Password", Observer.empty),
        div(
          cls := "field is-grouped",
          div(
            cls := "control",
            button(cls := "button is-link", "Register", onClick.flatMapStream { _ => create } --> commands)
          )
        )
      )
    )
  }
}
