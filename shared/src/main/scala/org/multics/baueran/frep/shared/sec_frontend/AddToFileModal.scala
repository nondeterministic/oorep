package org.multics.baueran.frep.shared.sec_frontend

import org.scalajs.dom
import org.multics.baueran.frep.shared.frontend.{Case, OorepHtmlButton, OorepHtmlElement, apiPrefix, getCookieData, serverUrl}
import org.multics.baueran.frep.shared.Defs.CookieFields
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import scalatags.JsDom.all.{id, _}
import org.scalajs.dom.Event
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success}
import io.circe.syntax._

object AddToFileModal extends FileModal with OorepHtmlElement {
  def getId() = "addToFileModal"

  object CloseButton extends OorepHtmlButton {
    def getId() = "AddToFileModal_CloseButton_fsdfsd2345rsfd"

    def apply() = {
      button(id:=getId(), `type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
    }
  }

  object SubmitButton extends OorepHtmlButton {
    def getId() = "submitAddToFileModal"

    def apply() = {
      button(cls := "btn btn-primary mb-2", style := "margin-left:8px;", `type` := "button", id := getId(), disabled := true,
        "Submit",
        onclick := { (event: Event) =>
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
                    AddToFileModal.CloseButton.click()
                  }
                  case response: Failure[SimpleHttpResponse] => {
                    println("ERROR: AddToFileModal: " + response.get.body)
                  }
                })
            }
            case None =>
              println("ERROR: AddToFileModal: no case description available to complete operation. This should never have happened.")
          }
          CloseButton.click()
        }
      )
    }
  }

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:=getId(),
      onshow := { (event: Event) =>
        if (selected_file_id.now == None)
          SubmitButton.disable()
      },
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Choose file to add current case to"),
            CloseButton()
          ),
          div(cls:="modal-body",
            modalBodyFileSelection(),
            div(cls:="form-group d-flex flex-row-reverse",
              SubmitButton(),
              button(data.dismiss:="modal", cls:="btn mb-2 btn-secondary",
                onclick := { (event: Event) =>
                  unselectAll()
                },
                "Cancel")
            )
          )
        )
      )
    )
  }

}
