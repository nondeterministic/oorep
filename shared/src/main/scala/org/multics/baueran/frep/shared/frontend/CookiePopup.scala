package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import dom.Event
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.CookieFields
import org.querki.jquery.$

import scala.scalajs.js
import scalatags.JsDom.all._

import scala.util.Success

class CookiePopup(parentId: String) {

  // /////////////////////////////////////////////////////////////////////////////////////////
  // This whole popup is ugly as sin.  In an ideal world, it shouldn't be here.  But it's
  // technical uglyness doesn't matter for now, because it is displayed only once
  // when the user visits the site for the first time, or if the cookie has been removed.
  //
  // The whole dialog should not be necessary and instead of refactoring it, I'm hoping that
  // legislation will change to make it no longer necessary one day.  (But, I am realistic
  // and am not holding my breath.)
  // /////////////////////////////////////////////////////////////////////////////////////////

  def add() = {

    def loadAndScroll(file: String) = {
      $(s"#${parentId}").load(s"${serverUrl()}/assets/html/$file")
      js.eval("$('html, body').animate({ scrollTop: 0 }, 'fast');")
    }

    getCookieData(dom.document.cookie, CookieFields.cookiePopupAccepted.toString) match {
      case None =>
        val dialog =
          div(cls:="modal fade modalless", data.backdrop:="static", data.keyboard:="false", tabindex:="-1", role:="dialog", id:="cookiePopup",
            div(cls:="modal-dialog", role:="document", style:="min-width: 50%;",
              div(cls:="modal-content",
                div(cls:="modal-header",
                  h5(cls:="modal-title", "Cookie-Einverständniserklärung / Agreement to use cookies")
                ),
                div(cls:="modal-body",
                  p("Diese Website benötigt Cookies als technische Grundvoraussetzung. Durch Klicken des Annehmen-Buttons erklären Sie, dass sie unsere ",
                    a(href:="#",
                      onclick:= { () =>
                        $(s"#${parentId}").empty()
                        js.eval("$('#cookiePopup').modal('hide');")
                        loadAndScroll("datenschutz.html")
                      }, "Datenschutzerklärung"),
                    " gelesen und verstanden haben und einverstanden mit der Vewendung der übertragenen Cookies sind."),
                  p("The basic functionality of this web site depends on the use of cookies. By clicking the accept button, you acknowledge that you have read and understand our ",
                    a(href:="#",
                      onclick:= { () =>
                        js.eval("$('#cookiePopup').modal('hide');")
                        loadAndScroll("cookies.html")
                      }, "privacy policy"),
                    ", and consent to the use and transmission of cookies.")
                ),
                div(cls:="modal-footer",
                  button(`type`:="button", cls:="btn btn-secondary", data.dismiss:="modal",
                    onclick:= { (event: Event) =>
                      event.stopPropagation()
                      dom.window.location.assign("https://www.europarl.europa.eu/")
                    }, "Ablehnen / Refuse"),
                  button(`type`:="button", cls:="btn btn-primary", data.dismiss:="modal", data.toggle:="modal",
                    onclick:= { (event: Event) =>
                      event.stopPropagation()
                      HttpRequest(s"${serverUrl()}/${apiPrefix()}/accept_cookies")
                        .send()
                        .onComplete({
                          case _: Success[SimpleHttpResponse] =>
                            js.eval("$('#cookiePopup').modal('hide');")
                          case _ =>
                            println("Error: Cookie popup not destroyed.")
                        })
                    }, "Annehmen / Accept"),
                )
              )
            )
          )

        $("#body").empty()
        dom.document.body.appendChild(dialog.render)
        js.eval("$('#cookiePopup').modal('show');")
      case Some(_) =>
        println("Initial cookie already present. Not adding cookie popup to page.")
    }
  }

}
