package com.github.sashjakk.pages.main

import com.github.sashjakk.{AppCommand, SpotShared}
import com.github.sashjakk.spot.Spot
import com.github.sashjakk.spot.share.{Share, ShareCreate}
import com.raquo.laminar.api.L._
import io.laminext.fetch.Fetch
import io.laminext.fetch.circe._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import java.time.Instant

object ShareSpot {
  def apply(commands: Observer[AppCommand], spots: Signal[List[Spot]]): Element = {
    val from = Var(Instant.now())
    val to = Var(Instant.now().plusSeconds(2 * 60 * 60))
    val identifier = Var("")

    spots.map(_.head.identifier) --> identifier

    val spotSelectors = spots.split(_.id) { (_, it, _) =>
      option(selected := false, it.identifier)
    }

    def share: EventStream[AppCommand] = {
      spots
        .map { it => it.find(_.identifier == identifier.now()) }
        .flatMap { case Some(spot) =>
          Fetch
            .post("api/spots/share", jsonRequestBody(ShareCreate(spot.id, from.now(), to.now())))
            .decode[Share]
            .map(it => SpotShared(it.data))
        }
    }

    div(
      cls := "column",
      h3(cls := "title is-3", "Share"),
      div(
        cls := "field",
        label(cls := "label", "Spot"),
        div(
          cls := "control",
          div(cls := "select", select(children <-- spotSelectors, onInput.mapToValue --> identifier))
        ),
        div(
          cls := "field is-grouped",
          div(
            cls := "control",
            input(
              cls := "input",
              typ := "datetime-local",
              defaultValue <-- from.signal.map(_.toString.dropRight(8)),
              onInput.mapToValue
                .map(it => s"$it:00.000Z")
                .map(Instant.parse(_)) --> from
            )
          ),
          div(
            cls := "control",
            input(
              cls := "input",
              typ := "datetime-local",
              defaultValue <-- to.signal.map(_.toString.dropRight(8)),
              onInput.mapToValue
                .map(it => s"$it:00.000Z")
                .map(Instant.parse(_)) --> to
            )
          )
        ),
        div(
          cls := "field is-grouped",
          div(
            cls := "control",
            button(cls := "button is-link", "Share"),
            onClick.flatMapStream { _ => share } --> commands
          )
        )
      )
    )
  }
}
