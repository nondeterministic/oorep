package org.multics.baueran.frep.frontend.secure.base

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.scalajs.dom.document
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.frontend.{serverUrl, apiPrefix}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel
import org.querki.jquery._
import scalatags.JsDom.all._

import scala.util.{Failure, Success}
import org.multics.baueran.frep.shared._
import frontend.{Case, Disclaimer, Repertorise, LoadingSpinner}
import Defs.deleteCookies
import sec_frontend.{AddToFileModal, EditFileModal, FileModalCallbacks, NewFileModal, OpenFileModal, RepertoryModal}

@JSExportTopLevel("MainSecure")
object Main {

  private val loadingSpinner = new LoadingSpinner("content")
  private val disclaimer = new Disclaimer("content_bottom", "content")

  // TODO: I think the below is no longer necessary if the html and js files for logged in users are protected by location/SP in the reverse proxy.
  private def authenticateAndPrepare(): Unit = {
    HttpRequest(s"${serverUrl()}/${apiPrefix()}/authenticate")
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          $("#content").append(Repertorise().render)

          try {
            val memberId = response.get.body.toInt
            FileModalCallbacks.updateMemberFiles(memberId)
          } catch {
            case exception: Throwable =>
              println("Exception: could not convert member-id '" + exception + "'. Deleting the cookies now!")
              deleteCookies()
              $("#nav_bar").empty()
              $("#content").empty()
              $("#content_bottom").empty()
              $("#content").append(p(s"Not authenticated or cookie expired. Go to ", a(href:=serverUrl(), "main page"), " instead!",
                br, "(If all else fails, try deleting all OOREP cookies from your browser.)").render)
          }
        }
        case _: Failure[SimpleHttpResponse] => {
          deleteCookies()
          loadingSpinner.remove()
          $("#content").append(p(s"Not authenticated or cookie expired. Go to ", a(href:=serverUrl(), "main page"), " instead!",
            br, "(If all else fails, try deleting all OOREP cookies from your browser.)").render)
        }
      })
  }

  def main(args: Array[String]): Unit = {
    $("#temporary_content").empty() // This is the static page which is shown when JS is disabled

    Repertorise.init(loadingSpinner, disclaimer)

    dom.document.body.appendChild(div(style := "width:100%;", id := "nav_bar").render)
    dom.document.body.appendChild(div(style := "width:100%;", id := "content").render)
    dom.document.body.appendChild(div(style := "width:100%;", id := "content_bottom").render)

    // Stuff to make the NavBar (dis)appear dynamically in a reactive sense
    var navBarDark = false
    $(dom.window).scroll(() => {
      if (Repertorise._repertorisationResults.now.size == 0 && Case.size() == 0) {
        if ($(document).scrollTop() > 150) {
          if (!navBarDark) {
            $("#public_nav_bar").addClass("bg-dark navbar-dark shadow p-3 mb-5")
            $("#nav_bar_logo").append(a(cls := "navbar-brand py-0", style:="margin-top:8px;", href := serverUrl(), h5(cls := "freetext", "OOREP")).render)
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

    loadingSpinner.add()

    dom.document.body.appendChild(AddToFileModal().render)
    dom.document.body.appendChild(OpenFileModal().render)
    dom.document.body.appendChild(EditFileModal().render)
    dom.document.body.appendChild(NewFileModal().render)
    dom.document.body.appendChild(RepertoryModal().render)
    dom.document.body.appendChild(Case.analysisModalDialogHTML().render)
    dom.document.body.appendChild(Case.editDescrModalDialogHTML().render)

    $("#nav_bar").empty()
    $("#nav_bar").append(NavBar().render)
    $("#nav_bar").addClass("d-none") // Hide navbar (Repertorise.scala will show it again)

    disclaimer.add()

    if (!dom.window.location.toString.contains("show"))
      authenticateAndPrepare()
  }

}
