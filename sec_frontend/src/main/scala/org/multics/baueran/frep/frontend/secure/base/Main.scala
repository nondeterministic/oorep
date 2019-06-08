package org.multics.baueran.frep.frontend.secure.base

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.scalajs.dom.document
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel
import org.querki.jquery._
import scalatags.JsDom.all._

import scala.util.{Failure, Success}
import org.multics.baueran.frep.shared._
import frontend.{Case, Disclaimer, Repertorise}
import sec_frontend.{AddToFileModal, EditFileModal, FileModalCallbacks, NewFileModal, OpenFileModal}

@JSExportTopLevel("MainSecure")
object Main {

  def main(args: Array[String]): Unit = {
    $(dom.document.body).append(div(style:="width:100%;", id:="nav_bar").render)
    $(dom.document.body).append(div(style:="width:100%;", id:="content").render)
    $(dom.document.body).append(div(style:="width:100%;", id:="content_bottom").render)

    // No access without valid cookies!
    HttpRequest("http://localhost:9000/authenticate")
      .withCrossDomainCookies(true)
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          dom.document.body.appendChild(AddToFileModal().render)
          dom.document.body.appendChild(OpenFileModal().render)
          dom.document.body.appendChild(EditFileModal().render)
          dom.document.body.appendChild(NewFileModal().render)

          dom.document.body.appendChild(Case.analysisModalDialogHTML().render)
          dom.document.body.appendChild(Case.editDescrModalDialogHTML().render)

          $("#nav_bar").empty()
          $("#nav_bar").append(NavBar().render)
          $("#content").append(Repertorise.apply().render)
          $("#content_bottom").append(Disclaimer.toHTML().render)

          val memberId = response.get.body.toInt
          FileModalCallbacks.updateMemberFiles(memberId)
        }
        case error: Failure[SimpleHttpResponse] => {
          $("#content").append(p("Not authorized.").render)
        }
      })

    // Stuff to make the NavBar (dis)appear dynamically
    var navBarDark = false
    $(dom.window).scroll(() => {
      if (Repertorise.results.now.size == 0) {
        if ($(document).scrollTop() > 150) {
          if (!navBarDark) {
            $("#public_nav_bar").addClass("bg-dark navbar-dark shadow p-3 mb-5")
            $("#nav_bar_logo").append(a(cls := "navbar-brand py-0", href := serverUrl(), "OOREP").render)
            navBarDark = true
          }
        }
        else {
          $("#public_nav_bar").removeClass("bg-dark navbar-dark shadow p-3 mb-5")
          $("#nav_bar_logo").empty()
          navBarDark = false
        }
      }
    })
  }

}
