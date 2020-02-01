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
      a(href:="https://gitlab.com/nondeterministic/oorep",
        // img(style:="position: absolute; top: 0; right: 0; border: 0;", src:="https://s3.amazonaws.com/github/ribbons/forkme_right_red_aa0000.png", alt:="Fork me on GitHub")
        img(style:="position: absolute; top: 0; right: 0; border: 0;", src:=s"${serverUrl()}/assets/html/img/forkme_right_red.png", alt:="Fork me on GitLab")
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
      div(style:="margin-top: 0.5cm;",
        a(cls:="underline", style:="color:white;", href:=serverUrl(), "Home"),
        " | ",
        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("impressum.html") }, "Impressum"),
        " | ",
        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("contact.html") }, "Contact"),
        " | ",
        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () => loadAndScroll("cookies.html") }, "Privacy policy"),
      ),
      br,
      div(style:="margin-top: 0.5cm; font-size:12px;",
        p(style:="text-align:justify;",
          "Copyright ", raw("&copy;"), " 2020 Andreas Bauer. All rights reserved."
        )
      )
    )
  }

}
