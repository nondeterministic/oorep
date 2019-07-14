package org.multics.baueran.frep.frontend.public.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

object About {
  def toHTML() = {
    div(cls:="jumbotron jumbo-dark horizontal-center", id:="about", style:="position: relative;",
      div(cls:="container",
        div(cls:="row",
          div(cls:="col",
            img(style:="height:500px", src:="img/hahnemann_statue.png")
          ),
          div(cls:="col align-self-center",
            h3(style:="text-align:center; margin-bottom: 10px",
              span("About")
            ),
            div(style:="font-size:12px; max-width:700px;",
              p("""
             OOREP is an open online repertory for homeopathy.  It is open, because its source code is freely available,
             it is online, because a running instance of it is served right here on this very server for your convenience.
             It is NOT some kind of medical recommendation system - please read the disclaimer below.  Repertorisation is
             the looking up of homeopathic repertories, and as such this system does not provide any information, which
             you cannot obtain from its original sources.  However, online repertorisation is so much easier sometimes than
             looking up the categories yourself.
                """),
              p("""
             OOREP was especially developed for people who study homeopathy, either professionally or as a hobby like I am.
             I am not making any money from developing this software or from a homeopathic practice of any sorts.  As an interested
             layperson I have found it difficult, if not impossible, to get access to a good electronic repertorisation.
             Most homeopathic softwares are very expensive and intended to be used by people who make money by using them.
             For those of us who don't (or don't yet), like hobbyists or students, the access to these kinds of programs used to be very slim.
             With OOREP I intend to improve this sitation somewhat.  So I hope it is useful to others, too.
                """)
            )
          )
        )
      )
    )
  }

}
