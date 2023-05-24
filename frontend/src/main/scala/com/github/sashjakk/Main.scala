package com.github.sashjakk

import com.github.sashjakk.pages.RegistrationPage
import com.github.sashjakk.pages.main.MainPage
import com.raquo.laminar.api.L._
import org.scalajs.dom

object Main {
  def main(args: Array[String]): Unit = {
    val state = Var[AppState](Unauthorized())

    val commander = Observer[AppCommand] {
      case UserRegistered(user) => state.set(Ready(user))
      case SpotCreated(spot) =>
        state.update {
          case current: Ready => current.copy(spots = current.spots :+ spot)
          case it             => it
        }
    }

    def app(state: Signal[AppState], commands: Observer[AppCommand]): Element = {
      val page = state.map {
        case Unauthorized() => RegistrationPage(commands)
        case state: Ready   => MainPage(state, commands)
      }

      div(child <-- page)
    }

    renderOnDomContentLoaded(container = dom.document.getElementById("main"), rootNode = app(state.signal, commander))
  }
}
