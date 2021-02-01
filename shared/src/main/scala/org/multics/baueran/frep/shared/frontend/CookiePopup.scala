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
      $(s"#${parentId}").load(s"${serverUrl()}$file")
      js.eval("$('html, body').animate({ scrollTop: 0 }, 'fast');")
    }

    getCookieData(dom.document.cookie, CookieFields.cookiePopupAccepted.toString) match {
      case None =>
        val dialog =

          div(cls:="alert text-center cookiealert", role:="alert", id:="cookiePopup",
            b("Do you like cookies? "),
            "\uD83C\uDF6A We use cookies to ensure you get the best experience on our website. ",
            a(href:="#",
              onclick:= { () =>
                $(s"#${parentId}").empty()
                js.eval("$('#cookiePopup').modal('hide');")
                loadAndScroll("/partial/cookies")
              }, "Learn more"),
            button(`type`:="button", cls:="btn btn-primary btn-sm acceptcookies",
              onclick:= { (event: Event) =>
                event.stopPropagation()
                HttpRequest(s"${serverUrl()}/${apiPrefix()}/accept_cookies")
                  .send()
                  .onComplete({
                    case _: Success[SimpleHttpResponse] =>
                      $("#cookiePopup").removeClass("show")
                      $("#cookiePopup").hide()
                    case _ =>
                      println("Error: Cookie popup not destroyed.")
                  })
              }, "I understand")
          )

        $("#body").empty()
        dom.document.body.appendChild(dialog.render)
        $("#cookiePopup").addClass("show")
      case Some(_) =>
        println("Initial cookie already present. Not adding cookie popup to page.")
    }
  }

}
