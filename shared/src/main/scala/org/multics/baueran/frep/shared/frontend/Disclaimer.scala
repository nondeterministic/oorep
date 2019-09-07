package org.multics.baueran.frep.shared.frontend

import org.querki.jquery.$
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav
import org.multics.baueran.frep.shared.Defs._

object Disclaimer {

  def toHTML() = {
    div(cls:="jumbotron jumbo-dark horizontal-center", id:="content_bottom", style:="position: relative;",
      a(href:="https://github.com/you",
        img(style:="position: absolute; top: 0; right: 0; border: 0;", src:="https://s3.amazonaws.com/github/ribbons/forkme_right_red_aa0000.png", alt:="Fork me on GitHub")
      ),
      br,
      div(style:="font-size:12px; max-width:700px;",
        h4(style:="text-align:center; margin-bottom: 10px",
          span("Hinweis / Disclaimer")
        ),
        p(style:="text-align:justify;",
          """
            Diese Website ist ausschließlich für Übungszwecke gedacht.
            Sie ist nicht geeignet zur Selbstmedikation oder ersetzt den Besuch eines Arztes oder Apothekers.
            Die Wirksamkeit von Homöopathie ist wissenschaftlich nicht bewiesen!
            Der Autor dieser Website übernimmt keine Verantwortung für die Richtigkeit der Ergebnisse oder die Folgen,
            die aus einem unsachgemäßen Gebrauch resultieren.
          """),
        p(style:="text-align:justify;",
          """
            This web site is intended as a training tool only.
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

        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () =>
          // TODO: Using JQuery here, because the below doesn't work. Not nice, I know!
          $("#content").load(relHtmlPath() + "impressum.html")
        }, "Impressum"),

        " | ",

        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () =>
          // TODO: See above!
          $("#content").load(relHtmlPath() + "contact.html")
        }, "Contact"),

        " | ",

        a(cls:="underline", style:="color:white;", href:="#", onclick:= { () =>
          // TODO: See above!
          $("#content").load(relHtmlPath() + "cookies.html")
        }, "Cookie policy"),
      )
    )
  }

}
