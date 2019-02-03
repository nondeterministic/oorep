package org.multics.baueran.frep.shared.sec_frontend

import org.querki.jquery._

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{id, input, li, _}
import scalatags.JsDom.tags2.nav
import org.scalajs.dom.Event

import scala.scalajs.js

object NewFileModal {

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="newFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Create a new file"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              form(
                div(cls:="form-group",
                  label(`for`:="fileHeader", "ID"),
                  input(cls:="form-control",
                    id:="fileHeader",
                    placeholder:="A simple, unique file identifier")
                  // required)
                ),
                div(cls:="form-group",
                  label(`for`:="fileDescr", "Description"),
                  textarea(cls:="form-control", id:="fileDescr", rows:="3", placeholder:="A more verbose description of the file")
                ),
                div(
                  button(data.dismiss:="modal", cls:="btn mb-2",
                    "Cancel",
                    onclick:={ (event: Event) =>
                      $("#fileHeader").`val`("")
                      $("#fileDescr").`val`("")
                    }),
                  button(cls:="btn btn-primary mb-2", `type`:="button",
                    "Submit",
                    onclick:={(event: Event) =>
                      event.stopPropagation()
                      println("PRESSED SOME SHIT!")
                      js.eval("$('#newFileModal').modal('hide');")
                    })
                )
              )
            )
          )
        )
      )
    )

  }
}
