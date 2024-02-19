package org.multics.baueran.frep.shared.sec_frontend

import org.scalajs.dom
import org.multics.baueran.frep.shared.frontend.{Case, OorepHtmlButton, OorepHtmlElement, getCookieData}
import org.multics.baueran.frep.shared.Defs.{CookieFields, HeaderFields}
import org.multics.baueran.frep.shared.HttpRequest2
import scalatags.JsDom.all.{id, _}
import org.scalajs.dom.Event
import io.circe.syntax._
import org.multics.baueran.frep.shared.TopLevelUtilCode.getDocumentCsrfCookie

object AddToFileModal extends FileModal("AddToFileModal__") with OorepHtmlElement {
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
            case Some(caze) =>
              HttpRequest2("sec/save_case")
                .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
                .onSuccess((response: String) => {
                  Case.updateCurrOpenCaseId(response.toInt)
                  Case.updateCurrOpenFile(selected_file_id)
                  Case.updateCaseViewAndDataStructures()
                  Case.updateCaseHeaderView()
                  AddToFileModal.CloseButton.click()
                })
                .post(
                  "fileId" -> selected_file_id.getOrElse(-1).toString(),
                  "case" -> caze.asJson.toString()
                )
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
        updateData()

        if (selected_file_id == None)
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
