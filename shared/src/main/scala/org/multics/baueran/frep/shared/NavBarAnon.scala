package org.multics.baueran.frep.shared

import org.multics.baueran.frep.shared.TopLevelUtilCode.toggleTheme
import org.multics.baueran.frep.shared.frontend.{OorepHtmlElement, serverUrl}
import org.scalajs.dom
import scalatags.JsDom.all.{id, *}
import org.scalajs.dom.{Event, html}
import scalatags.JsDom.tags2.nav

object NavBarAnon extends OorepHtmlElement {

  override def getId() = "nav_bar"

  override def apply() = {
    nav(cls:="navbar py-0 py-md-0 navbar-expand-sm navbar-light p-3 bg-dark navbar-dark shadow", id:=getId(),
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler", aria.label:="Menu",
        span(cls:="navbar-toggler-icon"),
      ),
      div(id:="nav_bar_logo",
        a(cls:="navbar-brand py-0", style:="margin-top:8px;", href:=serverUrl(), h5(cls:="freetext", "OOREP"))
      ),
      div(cls:="navbar-header pull-right",
        form(cls:="form-inline",
          div(cls:="nav-item custom-control custom-switch mr-sm-2", style:="line-height: 7px; padding-right: 0rem; padding-left: 3rem;",
            input(`type`:="checkbox", cls:="navbar-toggler form-control custom-control-input", id:="transparency", checked,
              onclick := { (event: Event) => toggleTheme() }),
            label(cls:="navbar-text custom-control-label p-0 small", `for`:="transparency",
              span(cls:="oi oi-moon", style:="font-size: 18px; padding-top: 0px;", title:="Dark mode")
            )
          )
        )
      ),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div(cls:="ml-auto",
          ul(cls:="navbar-nav mr-auto",
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", id:="about_link", href:="@{org.multics.baueran.frep.backend.controllers.Get.staticServerUrl}/#about", "About")
            ),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", id:="features_link", href:="@{org.multics.baueran.frep.backend.controllers.Get.staticServerUrl}/#features", "Features")
            ),
            li(cls:="navbar-item",
              a(cls:="nav-link py-0", href:="/faq", "FAQ")
            ),
            li(cls:="navbar-item", style:="margin-right:10px;",
              a(cls:="nav-link py-0", href:="#disclaimer_div", "Contact")
            ),
            li(cls:="navbar-item", style:="margin-right:5px;",
              a(cls:="py-0", href:="/login",
                p(cls:="btn btn-sm btn-light my-auto text-nowrap",
                  span(cls:="oi oi-account-login", title:="Login", aria.hidden:="true"),
                  "Login"
                )
              )
            ),
            li(cls:="navbar-item",
              a(cls:="py-0", href:="/register",
                p(cls:="btn btn-sm btn-light my-auto text-nowrap",
                  span(cls:="oi oi-check", title:="Register", aria.hidden:="true"),
                  "Register"
                )
              )
            )
          )
        )
      )
    ).asInstanceOf[scalatags.JsDom.TypedTag[html.Element]]
  }
}
