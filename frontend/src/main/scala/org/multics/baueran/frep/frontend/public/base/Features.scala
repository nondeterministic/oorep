package org.multics.baueran.frep.frontend.public.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

object Features {
  def toHTML() = {
    div(cls:="jumbotron jumbo-light horizontal-center", id:="features", style:="position: relative;",
      div(style:="font-size:11px; max-width:700px;",
        h3(style:="text-align:center; margin-bottom: 10px",
          span("Features")
        ),
        p("""
            Diese Website richtet sich speziell an Studenten der Homöopathie bzw. professionelle Homöopathen und ist ausschließlich für Übungszwecke gedacht.
            Sie ist nicht geeignet zur Selbstmedikation oder ersetzt den Besuch eines Arztes oder Apothekers.
            Die Wirksamkeit von Homöopathie ist wissenschaftlich nicht bewiesen.
            Der Autor dieser Website übernimmt keine Verantwortung für die Richtigkeit der Ergebnisse oder die Folgen, die aus einem unsachgemäßen Gebrauch resultieren.
          """),
        p("""
            This web site is intended for students of homeopathy or professional homeopaths as a training tool only.
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
