package org.multics.baueran.frep.frontend.public.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav
import org.querki.jquery._
import org.scalajs.dom
import org.multics.baueran.frep.shared.frontend.serverUrl

object NavBar {

  // If #<anchor> doesn't exist on current page, reload the main landing page, where it will exist!
  private def goToAnchor(anchor: String) = {
    val divElem = dom.document.getElementById(anchor)
    if (divElem == null) {
      dom.window.location.replace(s"${serverUrl()}/#" + anchor)
      dom.window.location.reload(true)
    }
  }

  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    nav(cls:="navbar py-0 py-md-0 fixed-top navbar-expand-sm navbar-light", id:="public_nav_bar",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      div(id:="nav_bar_logo"),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div(cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:=s"${serverUrl()}/#about", onclick:={ () => goToAnchor("about")})("About")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:=s"${serverUrl()}/#features", onclick:={ () => goToAnchor("features")})("Features")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="#", onclick:= { () =>
                // TODO: Using JQuery here, because the below doesn't work. Not nice, I know!
                // dom.document.getElementById("content").innerHTML = "<object type='text/html' data='pricing.html'></object>"
                $("#content").load(s"${serverUrl()}/assets/html/pricing.html")
              })("Pricing")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="#", onclick:= { () =>
                // TODO: Same as bove.
                $("#content").load(s"${serverUrl()}/assets/html/faq.html")
              })("FAQ")),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", style:="margin-right:10px;", href:=s"#content_bottom", onclick:={ () => goToAnchor("content_bottom") })("Contact")),
            li(cls:="navbar-item", style:="margin-right:5px;",
              a(cls:="py-0", href:=s"${serverUrl()}/assets/html/login.html", p(cls:="btn btn-sm btn-success my-auto",
                span(cls := "oi oi-account-login", title := "Login", aria.hidden := "true"),
                " Login"
              ))),
            li(cls:="navbar-item",
              a(cls:="py-0", href:=s"${serverUrl()}/assets/html/register.html", p(cls:="btn btn-sm btn-danger my-auto",
                span(cls := "oi oi-check", title := "Register", aria.hidden := "true"),
                " Register"
              )))
          )
        )
      )
    )
  }
}
