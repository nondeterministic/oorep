package org.multics.baueran.frep.frontend.public.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

object Features {
  def toHTML() =
    div(cls:="jumbotron jumbo-light horizontal-center", id:="features", style:="position: relative;",
      div(cls:="container",
        div(cls:="row",
          div(cls:="col align-self-center",
            h3(style:="text-align:center; margin-bottom: 10px",
              span("Features")
            ),
            div(style:="font-size:12px; max-width:700px;",
              ul(
                li("Free and open: you can use this server for free or download the source code and run your own"),
                li("Platform-independent: all you need is a web-browser"),
                li("Access to common homeopathic repertories (Kent, Hering, etc.)"),
                li("Very easy to use: only the essential features - no distractions"),
                li("Combination of different repertories possible"),
                li("From a homeopathy enthusiast for homeopathy enthusiasts ;-)")
              )
            )
          ),
          div(cls:="col-md-auto",
            img(style:="height:350px", src:="img/drops-of-water-578897_960_720.jpg")
          )
        )
      )
    )

}
