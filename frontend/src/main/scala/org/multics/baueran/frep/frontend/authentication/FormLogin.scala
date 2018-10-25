package org.multics.baueran.frep.frontend.authentication

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

class FormLogin {
  def toHTML() = {
    div(cls:="text-center",
      link(href:="signin.css", rel:="stylesheet"),
      form(cls:="form-signin",
        img(src:="logo_small.png"),
        h1(cls:="h3 mb-3 font-weight-normal", "Please sign in"),
        label(`for`:="inputEmail", cls:="sr-only", "Email address"),
        input(`type`:="email", id:="inputEmail", cls:="form-control", placeholder:="Email address", required:="true"),
        label(`for`:="inputPassword", cls:="sr-only", "Password"),
        input(`type`:="password", id:="inputPassword", cls:="form-control", placeholder:="Password", required:="true"),
        div(cls:="checkbox mb-3",
          label(
            input(`type`:="checkbox", value:="remember-me"), " Remember me"
          )
        ),
        button(cls:="btn btn-lg btn-primary btn-block", `type`:="submit", "Sign in"),
        p(cls:="mt-5 mb-3 text-muted", a(href:="#", "Forgot your username or password?"))
      )
    )
  }

}
