package org.multics.baueran.frep.frontend.secure.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav
import org.scalajs.dom.Event
import org.querki.jquery._
import org.multics.baueran.frep.shared.Defs._

object NavBar {
  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    nav(cls:="navbar py-0 fixed-top navbar-expand-sm navbar-light", id:="public_nav_bar", style:="height:60px; line-height:55px;",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      div(id:="nav_bar_logo"),// a(cls := "navbar-brand py-0", href := serverUrl(), "OOREP")),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div( // cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Repertory")),
            li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Materia Medica")),
            li(cls:="navbar-item dropdown", a(cls:="nav-link dropdown-toggle", href:="#", data.toggle:="dropdown")("File"),
              div(cls:="dropdown-menu",
                a(cls:="dropdown-item", href:="#")("New.."),
                a(cls:="dropdown-item", href:="#")("Open...")
              )
            )
          )
        ),
        div(cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Settings"))
          )
        )
      )
    )
  }
}
