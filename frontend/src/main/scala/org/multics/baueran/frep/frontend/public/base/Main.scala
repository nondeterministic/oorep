package org.multics.baueran.frep.frontend.public.base

import org.scalajs.dom
import dom.html
import dom.raw._
import dom.document
import scalatags.JsDom.all._

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation._
import org.querki.jquery._
import scalatags.JsDom.all._

import org.multics.baueran.frep.shared.Defs._
import org.multics.baueran.frep.shared.frontend.Repertorise
import org.multics.baueran.frep.shared.frontend.Disclaimer

@JSExportTopLevel("Main")
object Main {

  def main(args: Array[String]): Unit = {
    $(dom.document.body).append(div(style:="width:100%;", id:="nav_bar").render)
    $(dom.document.body).append(div(style:="width:100%;", id:="content").render)
    $(dom.document.body).append(div(style:="width:100%;", id:="content_bottom").render)

    $("#nav_bar").empty()
    $("#nav_bar").append(NavBar.apply().render)
    $("#content").append(Repertorise.apply().render)
    $("#content").append(About.toHTML().render)
    $("#content").append(Features.toHTML().render)
    $("#content_bottom").append(Disclaimer.toHTML().render)

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
