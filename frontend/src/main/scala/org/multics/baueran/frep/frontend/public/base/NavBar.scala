package org.multics.baueran.frep.frontend.public.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav
import org.scalajs.dom.Event
import org.querki.jquery._
import org.scalajs.dom
import org.multics.baueran.frep.shared.Defs._

object NavBar {

  // If #<anchor> doesn't exist, reload intro page, where it does exist!
  private def goToAnchor(anchor: String) = {
    val divElem = dom.document.getElementById(anchor)
    if (divElem == null) {
      dom.window.location.replace("index.html#" + anchor)
      dom.window.location.reload(true)
    }
  }

  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    nav(cls:="navbar py-0 fixed-top navbar-expand-sm navbar-light", id:="public_nav_bar", style:="height:60px; line-height:55px;",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      div(id:="nav_bar_logo"),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div(cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="#about", onclick:={ () => goToAnchor("about")})("About")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="#features", onclick:={ () => goToAnchor("features")})("Features")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="#", onclick:= { () =>
                // TODO: Using JQuery here, because the below doesn't work. Not nice, I know!
                $("#content").load("pricing.html")
                // dom.document.getElementById("content").innerHTML = "<object type='text/html' data='pricing.html'></object>"
              })("Pricing")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="#", onclick:= { () =>
                // TODO: Same as bove.
                $("#content").load("faq.html")
              })("FAQ")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="#content_bottom", onclick:={ () => goToAnchor("content_bottom") })("Contact")),
            li(cls:="navbar-item", style:="margin-right:5px; margin-left:10px;",
              a(cls:="py-0", href:="login.html", p(cls:="btn btn-sm btn-success my-auto", "Login"))),
            li(cls:="navbar-item",
              a(cls:="py-0", href:="register.html", p(cls:="btn btn-sm btn-danger my-auto", onclick:={ () => println("pressed6") }, "Register")))
          )
        )
      )
    )
  }
}
