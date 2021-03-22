package org.multics.baueran.frep.frontend.public.base

import org.multics.baueran.frep.shared.frontend.serverUrl
import scalatags.JsDom.all._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLButtonElement

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("ChangePasswordForm")
object ChangePasswordForm {

  @JSExport("show")
  def show(memberId: Int, pcrId: String) = {
    val contentDiv = dom.document.getElementById("content").asInstanceOf[dom.html.Element]
    contentDiv.innerHTML = ""
    contentDiv.appendChild(apply(memberId, pcrId).render)
  }

  private def isAllowedPassword(password: String) = {
    val length = password.length > 7
    val hasLowerCaseChar = password.filter(_.isLower).length > 0
    val hasUpperCaseChar = password.filter(_.isUpper).length > 0
    val hasNumber = password.filter(_.isDigit).length > 0
    val hasSpecial = password.filter(char =>
      char == '&' || char == '*' || char == '_' || char == '+' || char == '-' || char == '#' || char == '@' || char == '$' || char == '%' || char == '!' || char == '.'
    ).length > 0

    length && hasLowerCaseChar && hasUpperCaseChar && hasNumber && hasSpecial
  }

  private def formHtmlCode(memberId: Int, pcrId: String) = {

    def passwordNotAllowedAlert() = {
      div(cls := "alert alert-danger", id:="passwordNotAllowedAlert", role := "alert",
        button(`type` := "button", cls := "close", data.dismiss := "alert", span(aria.hidden := "true", raw("&times;"))),
        b("Entered password is not secure. Your password must"),
        ul(
          li(b("be at least 8 characters long,")),
          li(b("contain a lower case character,")),
          li(b("an upper case character,")),
          li(b("a number and a special symbol like &, *, _, +, -, #, @, $, %, !."))
        )
      )
    }

    def getFormInputs() = {
      (dom.document.getElementById("pass1"), dom.document.getElementById("pass2")) match {
        case (null, _) => None
        case (_, null) => None
        case (p1, p2) => (p1.asInstanceOf[dom.html.Input].value.trim, p2.asInstanceOf[dom.html.Input].value.trim) match {
          case (pass1: String, pass2: String) if (pass1 == pass2) => Some(pass1, pass2) // This is the only "good-case"
          case _ => None
        }
      }
    }

    def handleOninput(event: dom.KeyboardEvent) = {
      getFormInputs() match {
        case Some((pass1, pass2)) if (pass1.length > 0 && pass2.length > 0) =>
          val button = dom.document.getElementById("submitButton").asInstanceOf[HTMLButtonElement]
          button.removeAttribute("disabled")

        case None =>
          val button = dom.document.getElementById("submitButton").asInstanceOf[HTMLButtonElement]
          button.setAttribute("disabled", "true")
      }
    }

    def handleOnsubmit(event: dom.UIEvent) = {
      dom.document.getElementById("passwordNotAllowedAlert") match {
        case null => ;
        case alert =>
          dom.document.getElementById("changePasswordFormContainer").asInstanceOf[dom.html.Element].removeChild(alert.asInstanceOf[dom.html.Element])
      }

      getFormInputs() match {
        case Some((pass, _)) if (isAllowedPassword(pass)) =>
          println("Congrats!")
        case _ =>
          event.preventDefault()
          event.stopPropagation()
          dom.document.getElementById("changePasswordFormContainer").asInstanceOf[dom.html.Element].appendChild(passwordNotAllowedAlert().render)
      }
    }

    div(cls:="container", id:="changePasswordFormContainer", style:="padding-top:200px; padding-bottom:200px;",

      div(cls:="col text-center",
        img(src:=s"${serverUrl()}/assets/html/img/logo_small.png", alt:="OOREP - open online repertory of homeopathy")
      ),

      div(cls:="row justify-content-center", style:="padding-top:50px; padding-bottom:30px;",
        div(cls:="col-md-4 text-center",
          div(cls:="card",
            h5(cls:="card-header", "Enter a new password"),
            div(cls:="card-body",

              p("You will be redirected to the start page after pressing the submit button."),

              form(cls:="form-signin", action:="/api/submit_new_password", enctype:="text/html;charset=UTF-8", method:="POST",
                onsubmit:={ (event: dom.UIEvent) => handleOnsubmit(event) },

                input(`type`:="hidden", name:="pcrId", value:=s"${pcrId}"),
                input(`type`:="hidden", name:="memberId", value:=s"${memberId}"),

                p(input(`type`:="password", name:="pass1", id:="pass1", cls:="form-control", placeholder:="Enter password", required,
                  oninput:={ (event: dom.KeyboardEvent) => handleOninput(event) }
                )),

                p(input(`type`:="password", name:="pass2", id:="pass2", cls:="form-control", placeholder:="Repeat password", required,
                  oninput:={ (event: dom.KeyboardEvent) => handleOninput(event) }
                )),

                button(cls:="btn btn-lg btn-primary btn-block", `type`:="submit", id:="submitButton", "Submit", disabled:=true)
              )
            )
          )
        )
      )
    )
  }

  def apply(memberId: Int, pcrId: String) = {
    formHtmlCode(memberId, pcrId)
  }

}
