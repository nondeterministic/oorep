package org.multics.baueran.frep.shared.frontend

import org.querki.jquery.$
import org.scalajs.dom
import scalatags.JsDom.all._

import scala.scalajs.js

class Disclaimer(parentId: String, openLinksIn: String) {

  // TODO: Using JQuery here, because don't know how else to do it. Not nice, I know!
  private def loadAndScroll(file: String) = {
    $(s"#${openLinksIn}").load(serverUrl() + file)
    js.eval("$('html, body').animate({ scrollTop: 0 }, 'fast');")
  }

  def add() = {
    dom.document.getElementById(s"${parentId}").appendChild(
      div(
        id:="disclaimer_div",
        cls:="jumbotron jumbo-dark text-center center-block",
        style:="display: none; position: relative;"
      ).render)

    dom.document.getElementById("disclaimer_div").appendChild(div(id:="disclaimer_text").render)
    // TODO: Using JQuery here, because don't know how else to do it. Not nice, I know!
    $("#disclaimer_text").load(s"${serverUrl()}/partial/disclaimer_text_only")

    dom.document.getElementById("disclaimer_div").appendChild(
        div(cls:="container",
          a(cls:="underline", style:="color:white;", href:=serverUrl(), "Home"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="https://twitter.com/OOREP1", "News"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("/partial/impressum") }, "Impressum"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("/partial/contact") }, "Contact"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("/partial/cookies") }, "Privacy policy")
        ).render)

    dom.document.getElementById("disclaimer_div").appendChild(br.render)

    dom.document.getElementById("disclaimer_div").appendChild(
      div(cls:="container", style:="margin-top: 0.5cm; font-size:12px;",
        p("Copyright ", raw("&copy;"), " 2020-2021 Andreas Bauer. All rights reserved.")
      ).render)
  }

  def hide() = {
    dom.document.getElementById("disclaimer_div") match {
      case null => ;
      case loading => loading.asInstanceOf[dom.html.Div].style.display = "none"
    }
  }

  def show() = {
    dom.document.getElementById("disclaimer_div") match {
      case null => ;
      case loading => loading.asInstanceOf[dom.html.Div].style.display = "block"
    }
  }

}
