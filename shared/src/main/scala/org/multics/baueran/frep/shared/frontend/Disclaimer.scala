package org.multics.baueran.frep.shared.frontend

import org.querki.jquery.$
import org.scalajs.dom
import scalatags.JsDom.all._

import scala.scalajs.js

class Disclaimer(parentId: String, openLinksIn: String) {

  // TODO: Using JQuery here, because don't know how else to do it. Not nice, I know!
  private def loadAndScroll(file: String) = {
    $(s"#${openLinksIn}").load(serverUrl() + "/assets/html/" + file)
    js.eval("$('html, body').animate({ scrollTop: 0 }, 'fast');")
  }

  def add() = {
    $(s"#${parentId}").append(
      div(id:="disclaimer_div", cls:="jumbotron jumbo-dark text-center center-block", style:="display: none; position: relative;",
        a(href:="https://github.com/nondeterministic/oorep",
          img(style:="position: absolute; top: 0; right: 0; border: 0;", src:=s"${serverUrl()}/assets/html/img/forkme_right_red_aa0000.png", alt:="Fork me on GitHub")
          // img(style:="position: absolute; top: 0; right: 0; border: 0;", src:=s"${serverUrl()}/assets/html/img/forkme_right_red.png", alt:="Fork me on GitLab")
        ),
        br,
        div(cls:="container", style:="font-size:12px;",
          h4(style:="text-align:center; margin-bottom: 20px",
            span("Hinweis / Disclaimer")
          ),
          p(style:="text-align:justify;",
            """
            Diese Homöopathie-Website ist ausschließlich zu Informationszwecken gedacht.
            Sie ist nicht geeignet zur Selbstmedikation oder ersetzt den Besuch eines Arztes oder Apothekers.
            Die Wirksamkeit von Homöopathie ist wissenschaftlich nicht bewiesen!
            Der Autor dieser Website übernimmt keine Verantwortung für die Richtigkeit der Ergebnisse oder die Folgen,
            die aus einem unsachgemäßen Gebrauch resultieren.
            """),
          p(style:="text-align:justify;",
            """
            This homeopathy web site is intended for information purposes only.
            It is not suitable for self-medication or can replace the visit of a medical doctor/GP.
            Scientifically, homeopathy was never proven to actually work!
            The author of this web site does not take responsibility for the correctness of this sites'
            results or the consequences of misusing them.
            """)
        ),
        br,
        div(cls:="container",
          a(cls:="underline", style:="color:white;", href:=serverUrl(), "Home"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="https://twitter.com/OOREP1", "News"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("impressum.html") }, "Impressum"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("contact.html") }, "Contact"),
          " | ",
          a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("cookies.html") }, "Privacy policy"),
        ),
        br,
        div(cls:="container", style:="margin-top: 0.5cm; font-size:12px;",
          p("Copyright ", raw("&copy;"), " 2020-2021 Andreas Bauer. All rights reserved.")
        )
      ).render
    )
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
