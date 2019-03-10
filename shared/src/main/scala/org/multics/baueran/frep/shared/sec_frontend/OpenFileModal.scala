package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.syntax._
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.multics.baueran.frep.shared.frontend.Case
import org.scalajs.dom.Event
import scalatags.JsDom.all._

import scala.scalajs.js
import scala.util.{Failure, Success}

object OpenFileModal {

  var selected_file_id: String = ""

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="openFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Select file"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="form-group",
              div(cls:="list-group", role:="tablist", id:="openFileAvailableFilesList", style:="height: 250px; overflow-y: scroll;",
                a(cls := "list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", "<no files created yet>"))
            ),
            div(cls:="form-group",
              button(data.dismiss:="modal", cls:="btn mb-2", "Cancel"),
              button(data.dismiss:="modal", cls:="btn mb-2", "Delete"),
              button(cls:="btn btn-primary mb-2", `type`:="button", id:="submitOpenFileModal", disabled:=true,
                "Open",
                onclick:={(event: Event) =>
                  // event.stopPropagation()

                  Case.descr match {
                    case Some(caze) => {
                      HttpRequest(serverUrl() + "/savecase")
                        .post(MultiPartBody(
                          "case" -> PlainTextBody(caze.asJson.toString()),
                          "fileheader" -> PlainTextBody(selected_file_id)))
                        .onComplete({
                          case response: Success[SimpleHttpResponse] => {
                            println("Received: " + response.get.body)
                            js.eval("$('#openFileModal').modal('hide');")
                          }
                          case response: Failure[SimpleHttpResponse] => {
                            println("Failure: " + response.get.body)
                          }
                        })
                    }
                  }

                  js.eval("$('#openFileModal').modal('hide');")
                })

            )
          )
        )
      )
    )

  }
}
