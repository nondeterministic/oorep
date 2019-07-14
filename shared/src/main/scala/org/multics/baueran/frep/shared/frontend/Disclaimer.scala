package org.multics.baueran.frep.shared.frontend

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

object Disclaimer {
  def toHTML() = {
    div(cls:="jumbotron jumbo-dark horizontal-center", id:="content_bottom", style:="position: relative;",
      a(href:="https://github.com/you",
        img(style:="position: absolute; top: 0; right: 0; border: 0;", src:="https://s3.amazonaws.com/github/ribbons/forkme_right_red_aa0000.png", alt:="Fork me on GitHub")
      ),
      br,
      div(style:="font-size:12px; max-width:700px;",
        h3(style:="text-align:center; margin-bottom: 10px",
          span("Hinweis/Disclaimer")
        ),
        p("""
            Diese Website richtet sich speziell an Studenten der Homöopathie bzw. professionelle Homöopathen, sowie Hobby-Homöopathen und ist ausschließlich für Übungszwecke gedacht.
            Sie ist nicht geeignet zur Selbstmedikation oder ersetzt den Besuch eines Arztes oder Apothekers.
            Die Wirksamkeit von Homöopathie ist wissenschaftlich nicht bewiesen.
            Der Autor dieser Website übernimmt keine Verantwortung für die Richtigkeit der Ergebnisse oder die Folgen, die aus einem unsachgemäßen Gebrauch resultieren.
          """),
        p("""
            This web site is intended for students of homeopathy, professional and hobby homeopaths as a training tool only.
            It is not suitable for self-medication or can replace the visit of a medical doctor/GP.
            Scientifically, homeopathy was never reliably proven to work.
            The author of this web site does not take responsibility for the correctness of this sites' results or the consequences of misusing them.
          """)
      ),
      br,
      div(
        a(style:="color:white;", href:="#", "Impressum"),
        " | ",
        a(style:="color:white;", href:="#", "Contact"),
        " | ",
        a(style:="color:white;", href:="#", "Terms and conditions"),
        " | ",
        a(style:="color:white;", href:="#", "Cookie policy"),
      )
    )
  }

}
