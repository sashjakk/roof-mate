package com.github.sashjakk.pages.main

import com.github.sashjakk.spot.Spot
import com.github.sashjakk.{AppCommand, Ready}
import com.raquo.laminar.api.L._
import io.laminext.fetch.Fetch
import io.laminext.fetch.circe._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

object MainPage {
  def apply(state: Ready, commands: Observer[AppCommand]): Element = {
    val spots = Fetch
      .get(s"api/users/${state.user.id}/spots")
      .decode[List[Spot]]
      .map(_.data)

    div(
      cls("columns"),
      div(
        cls("column is-one-quarter is-offset-one-quarter"),
        CreateSpot(state.user, commands),
        ShareSpot(commands, spots.startWith(List.empty).signal)
      )
    )

//    div(
//      cls := "columns",
//      div(
//        cls := "column",
//        h3(s"Hey, ${state.user.name}"),
//        div(cls := "columns", div(cls := "column", CreateSpot(state.user, commands), ShareSpot(state.spots)))
//      )
//    )
  }
}

//<div class="tile is-ancestor">
//  <div class="tile is-4 is-vertical is-parent">
//    <div class="tile is-child box">
//      <p class="title">One</p>
//    </div>
//    <div class="tile is-child box">
//      <p class="title">Two</p>
//    </div>
//  </div>
//  <div class="tile is-parent">
//    <div class="tile is-child box">
//      <p class="title">Three</p>
//    </div>
//  </div>
//</div>
