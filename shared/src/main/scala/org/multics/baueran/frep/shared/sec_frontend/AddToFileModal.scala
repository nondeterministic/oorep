package org.multics.baueran.frep.shared.sec_frontend

import org.scalajs.dom
import org.querki.jquery.$
import org.multics.baueran.frep.shared.frontend.{getCookieData, Case, serverUrl, apiPrefix}
import org.multics.baueran.frep.shared.Defs.CookieFields
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
            div(cls:="form-group d-flex flex-row-reverse",
              button(cls:="btn btn-primary mb-2", style:="margin-left:8px;", `type`:="button", id:="submitAddToFileModal", disabled:=true,
                "Submit",
                onclick:={(event: Event) =>
                  Case.descr match {
                    case Some(caze) => {
                      HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/save_case")
                        .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
                        .post(MultiPartBody(
                          "fileId" -> PlainTextBody(selected_file_id.now.getOrElse(-1).toString()),
                          "case" -> PlainTextBody(caze.asJson.toString())))
                        .onComplete({
                          case response: Success[SimpleHttpResponse] => {
                            Case.updateCurrOpenCaseId(response.get.body.toInt)
                            Case.updateCurrOpenFile(selected_file_id.now)
                            Case.updateCaseViewAndDataStructures()
                            Case.updateCaseHeaderView()
                            js.eval("$('#addToFileModal').modal('hide');")
                          }
                          case response: Failure[SimpleHttpResponse] => {
                            println("ERROR: AddToFileModal: " + response.get.body)
                          }
                        })
                    }
                    case None =>
                      println("ERROR: AddToFileModal: no case description available to complete operation. This should never have happened.")
                  }

                  js.eval("$('#addToFileModal').modal('hide');")
                }),
              button(data.dismiss:="modal", cls:="btn mb-2 btn-secondary", "Cancel")
            )
          )
        )
      )
    )
  }

}
