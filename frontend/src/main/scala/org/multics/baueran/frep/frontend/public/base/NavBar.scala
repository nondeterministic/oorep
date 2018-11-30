package org.multics.baueran.frep.frontend.public.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav
import org.scalajs.dom.Event
import org.querki.jquery._
import org.multics.baueran.frep.frontend.public.authentication.FormLogin

object NavBar {
  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    nav(cls:="navbar py-0 fixed-top navbar-expand-sm bg-dark navbar-dark",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      a(cls:="navbar-brand", href:="index.html",
        img(src:="logo_small.png")
      ),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div(cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("About")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Features")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("Pricing")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("FAQ")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("Contact")),
            li(cls:="navbar-item active", style:="background-color: DarkGreen; margin-right: 5px; margin-left:10px;",
              a(cls:="nav-link", href:="login.html")("Login")),
            li(cls:="navbar-item active", style:="background-color: FireBrick;",
              a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("Register"))
          )
        )
      )
    )
  }
}
