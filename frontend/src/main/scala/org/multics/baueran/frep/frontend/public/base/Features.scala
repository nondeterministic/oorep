package org.multics.baueran.frep.frontend.public.base

import org.multics.baueran.frep.shared.frontend.serverUrl
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

object Features {
  def toHTML() =
    div(cls:="jumbotron jumbo-light horizontal-center", id:="features", style:="position: relative;",
      div(cls:="container",
        div(cls:="row h-100",
          div(cls:="col-md-auto",
            img(style:="height:350px", cls:="image", src:=s"${serverUrl()}/assets/html/img/drops-of-water-578897_960_720.jpg")
          ),
          div(cls:="col my-auto",
            h3(style:="text-align:left;margin-bottom: 10px",
              span("Features")
            ),
            div(style:="text-align:left;max-width:700px;",
              ul(
                li("Free and open: you can use this server for free or download the source code and run your own"),
                li("Platform-independent: all you need is a web-browser"),
                li("Access to common and well-known homeopathic repertories"),
                li("Very easy to use: only the essential features - no distractions"),
                li("Search using wildcards, e.g., 'cough*, dry*'"),
                li("Combine different repertories in a single case")
              )
            )
          )
        )
      )
    )

}
