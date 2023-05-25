package com.github.sashjakk.pages.authorization

import com.github.sashjakk.AppCommand
import com.raquo.laminar.api.L._

import scala.collection.immutable.HashMap

object AuthorizationPage {
  def apply(commands: Observer[AppCommand]): Element = {
    val pages = HashMap("Register" -> (0, Registration(commands)), "Login" -> (1, Login(commands)))

    val tabIndex = Var(initial = 0)

    val tabs = tabIndex.signal.map { current =>
      pages.map { case (key, (index, _)) =>
        val isActive = Option.when(index == current)(cls("is-active"))
        li(isActive, a(key, onClick.mapTo(index) --> tabIndex))
      }.toList
    }

    val content = tabIndex.signal.map { current =>
      pages.values
        .find { case (index, _) => current == index }
        .map(_._2)
    }

    div(div(cls("tabs is-centered"), ul(children <-- tabs)), child.maybe <-- content)
  }
}
