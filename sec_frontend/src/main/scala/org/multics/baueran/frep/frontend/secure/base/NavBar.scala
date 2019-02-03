package org.multics.baueran.frep.frontend.secure.base

// import org.querki.jquery._

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{id, input, li, _}
import scalatags.JsDom.tags2.nav
//import org.scalajs.dom.Event
//
//import scala.scalajs.js

object NavBar {

//  def newFileModal() = {
//    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="newFileModal",
//      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
//        div(cls:="modal-content",
//          div(cls:="modal-header",
//            h5(cls:="modal-title", "Create a new file"),
//            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
//          ),
//          div(cls:="modal-body",
//            div(cls:="table-responsive",
//              form(
//                div(cls:="form-group",
//                  label(`for`:="fileHeader", "ID"),
//                  input(cls:="form-control",
//                    id:="fileHeader",
//                    placeholder:="A simple, unique file identifier")
//                    // required)
//                ),
//                div(cls:="form-group",
//                  label(`for`:="fileDescr", "Description"),
//                  textarea(cls:="form-control", id:="fileDescr", rows:="3", placeholder:="A more verbose description of the file")
//                ),
//                div(
//                  button(data.dismiss:="modal", cls:="btn mb-2",
//                    "Cancel",
//                    onclick:={ (event: Event) =>
//                      $("#fileHeader").`val`("")
//                      $("#fileDescr").`val`("")
//                    }),
//                  button(cls:="btn btn-primary mb-2", `type`:="button",
//                    "Submit",
//                    onclick:={(event: Event) =>
//                      event.stopPropagation()
//                      println("PRESSED SOME SHIT!")
//                      js.eval("$('#newFileModal').modal('hide');")
//                    })
//                )
//              )
//            )
//          )
//        )
//      )
//    )
//  }

  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    nav(cls:="navbar py-0 fixed-top navbar-expand-sm navbar-light", id:="public_nav_bar", style:="height:60px; line-height:55px;",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      div(id:="nav_bar_logo"),// a(cls := "navbar-brand py-0", href := serverUrl(), "OOREP")),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div( // cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Repertory")),
            li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Materia Medica")),
            li(cls:="navbar-item dropdown", a(cls:="nav-link dropdown-toggle", href:="#", data.toggle:="dropdown")("File"),
              div(cls:="dropdown-menu",
                a(cls:="dropdown-item", href:="#", data.toggle:="modal", data.target:="#newFileModal")("New.."),
                a(cls:="dropdown-item", href:="#")("Open...")
              )
            )
          )
        ),
        div(cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Settings"))
          )
        )
      )
    )
  }

}
