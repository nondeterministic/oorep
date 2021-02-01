package org.multics.baueran.frep.frontend.public.base

import org.scalajs.dom
import dom.document
import org.querki.jquery.$

import scala.scalajs.js.annotation.JSExportTopLevel
import org.querki.jquery._
import scalatags.JsDom.all._
import org.multics.baueran.frep.shared.frontend.{Case, Disclaimer, Repertorise, serverUrl, LoadingSpinner}
import org.multics.baueran.frep.shared.sec_frontend.RepertoryModal

@JSExportTopLevel("Main")
object Main {

  def main(args: Array[String]): Unit = {
    $("#temporary_content").empty() // This is the static page which is shown when JS is disabled

    dom.document.body.appendChild(div(style:="width:100%;", id:="nav_bar").render)
    dom.document.body.appendChild(div(style:="width:100%;", id:="content").render)
    dom.document.body.appendChild(div(style:="width:100%;", id:="content_bottom").render)
    dom.document.body.appendChild(Case.analysisModalDialogHTML().render)

    // Stuff to make the NavBar (dis)appear dynamically in a reactive sense
    var navBarDark = false
    $(dom.window).scroll(() => {
      if (Repertorise._repertorisationResults.now.size == 0) {
        if ($(document).scrollTop() > 150) {
          if (!navBarDark) {
            $("#public_nav_bar").addClass("bg-dark navbar-dark shadow p-3 mb-5")
            $("#nav_bar_logo").append(a(cls := "navbar-brand py-0", style:="margin-top:8px;", href := serverUrl(), h5(cls:="freetext", "OOREP")).render)
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

    val disclaimer = new Disclaimer("content_bottom", "content")
    val loadingSpinner = new LoadingSpinner("content")
    loadingSpinner.add()
    disclaimer.add()
    Repertorise.init(loadingSpinner, disclaimer)

    if (dom.window.location.toString.contains("show")) {
      $("#nav_bar").empty()
      $("#nav_bar").append(NavBar().render)
      $("#nav_bar").addClass("d-none") // Hide navbar (Repertorise.scala will show it again)
    }
    else {

      $("#nav_bar").empty()
      $("#nav_bar").append(NavBar().render)

      $("#content").append(Repertorise().render)
      $("#content").append(div(id:="content_about").render)
      $("#content_about").load(s"${serverUrl()}/partial/about")
      $("#content").append(div(id:="content_features").render)
      $("#content_features").load(s"${serverUrl()}/partial/features")
    }
  }

}
