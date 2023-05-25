package com.github.sashjakk.pages.authorization

import com.github.sashjakk.components.Field
import com.github.sashjakk.user.{User, UserLogin}
import com.github.sashjakk.{AppCommand, Authorized}
import com.raquo.laminar.api.L._
import io.laminext.fetch.Fetch
import io.laminext.fetch.circe._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

object Login {
  def apply(commands: Observer[AppCommand]): Element = {
    val user = Var(UserLogin(""))
    val phone = user.updater[String] { (user, phone) => user.copy(phone = phone) }

    def login: EventStream[AppCommand] = {
      Fetch
        .post("api/users/login", body = jsonRequestBody(user.now()))
        .decode[User]
        .map(it => Authorized(it.data))
    }

    div(
      cls("container"),
      div(
        cls("column is-half is-offset-one-quarter"),
        Field("Phone", phone),
        Field("Password", Observer.empty),
        div(
          cls("field is-grouped"),
          div(cls("control"), button(cls("button is-link"), "Login", onClick.flatMapStream { _ => login } --> commands))
        )
      )
    )

  }
}
