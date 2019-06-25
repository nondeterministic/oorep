package org.multics.baueran.frep.shared.sec_frontend

import org.querki.jquery.$
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.multics.baueran.frep.shared.frontend.Case
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import scalatags.JsDom.all.{id, _}
import org.scalajs.dom.Event
import monix.execution.Scheduler.Implicits.global
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._

import scala.scalajs.js
import scala.util.{Failure, Success}
import io.circe.syntax._

object AddToFileModal extends FileModal {

  def enableButtons() = {
    $("#submitAddToFileModal").removeAttr("disabled")
  }

  def disableButtons() = {
    $("#submitAddToFileModal").attr("disabled", true)
  }

  def submitIsDisabled() = ($("#submitAddToFileModal").hasOwnProperty("disabled"))

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="addToFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Choose file to add current case to"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="form-group",
              div(cls:="list-group", role:="tablist", id:="addToFileAvailableFilesList", style:="height: 250px; overflow-y: scroll;", Rx(files()))
            ),
            div(cls:="form-group",
              button(data.dismiss:="modal", cls:="btn mb-2", "Cancel"),
              button(cls:="btn btn-primary mb-2", `type`:="button", id:="submitAddToFileModal", disabled:=true,
                "Submit",
                onclick:={(event: Event) =>
                  Case.descr match {
                    case Some(caze) => {
                      HttpRequest(serverUrl() + "/savecase")
                        .post(MultiPartBody(
                          "case" -> PlainTextBody(caze.asJson.toString()),
                          "fileheader" -> PlainTextBody(selected_file_id.now)))
                        .onComplete({
                          case response: Success[SimpleHttpResponse] => {
                            println("Received: " + response.get.body)
                            Case.updateCurrOpenCaseId(response.get.body.toInt)
                            Case.updateCurrOpenFile(Some(selected_file_id.now))
                            Case.updateCaseViewAndDataStructures()
                            Case.updateCaseHeaderView()
                            js.eval("$('#addToFileModal').modal('hide');")
                          }
                          case response: Failure[SimpleHttpResponse] => {
                            println("Failure: " + response.get.body)
                          }
                        })
                    }
                  }

                  js.eval("$('#addToFileModal').modal('hide');")
                })

            )
          )
        )
      )
    )
  }

}
