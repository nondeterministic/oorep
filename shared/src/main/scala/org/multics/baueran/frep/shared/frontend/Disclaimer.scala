package org.multics.baueran.frep.shared.frontend

import org.querki.jquery.$
import scalatags.JsDom.all._

import scala.scalajs.js

object Disclaimer {

  // TODO: Using JQuery here, because don't know how else to do it. Not nice, I know!
  private def loadAndScroll(file: String) = {
    $("#content").load(serverUrl() + "/assets/html/" + file)
    js.eval("$('html, body').animate({ scrollTop: 0 }, 'fast');")
  }

  def toHTML() = {
    div(cls:="jumbotron jumbo-dark horizontal-center", id:="content_bottom", style:="position: relative;",
//      a(href:="https://gitlab.com/nondeterministic/oorep",
//        span(style:="font-family: tahoma; font-size: 20px; position: absolute; top:50px; right:-45px; display:block; -webkit-transform: rotate(45deg); -moz-transform: rotate(45deg); background-color:red; color:white; padding: 4px 30px 4px 30px; z-index:99",
//          "Fork Me On GitLab")
//      ),
      a(href:="https://github.com/nondeterministic/oorep",
        img(style:="position: absolute; top: 0; right: 0; border: 0;", src:="https://s3.amazonaws.com/github/ribbons/forkme_right_red_aa0000.png", alt:="Fork me on GitHub")
      ),
      br,
      div(style:="font-size:12px; max-width:700px;",
        h4(style:="text-align:center; margin-bottom: 10px",
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
      div(style:="margin-top: 1cm;",
        a(cls:="underline", style:="color:white;", href:=serverUrl(), "Home"),
        " | ",
        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("impressum.html") }, "Impressum"),
        " | ",
        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("contact.html") }, "Contact"),
        " | ",
        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("cookies.html") }, "Privacy policy"),
      )
    )
  }

}
