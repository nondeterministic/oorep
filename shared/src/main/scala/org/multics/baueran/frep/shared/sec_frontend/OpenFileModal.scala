package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.Defs.{CookieFields, HeaderFields}
import org.multics.baueran.frep.shared.HttpRequest2
import org.multics.baueran.frep.shared.TopLevelUtilCode.getDocumentCsrfCookie
import org.multics.baueran.frep.shared.frontend.{Case, MainView, OorepHtmlButton, OorepHtmlElement, apiPrefix, getCookieData, serverUrl}
import org.scalajs.dom
import org.scalajs.dom.{Event, document, html}
import scalatags.JsDom.all._

object OpenFileModal extends FileModal("OpenFileModal__") {

  private def requestFileDeletion() = {
    getCookieData(dom.document.cookie, CookieFields.id.toString) match {
      case Some(memberId) => {
        HttpRequest2("sec/del_file_and_cases")
          .withMethod("DELETE")
          .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
          .withBody(
            ("memberId" -> memberId.toString()),
            ("fileId" -> selected_file_id.getOrElse(-1).toString))
          .onSuccess((_) => { FileModalCallbacks.updateMemberFiles(memberId.toInt) })
          .send()

        // If the file had currently a case opened in the case view,
        // remove it from screen to avoid weird database behaviour,
        // in case the user then modifies the case...
        if (Case.getCurrOpenFileId() == selected_file_id) {
          Case.removeFromMemory()
          MainView.CaseDiv.empty()
        }
      }
      case None => ; // TODO: Display error modal?!
    }
  }

  private object AreYouSureModal extends OorepHtmlElement {
    def getId() = "OpenFileModal_AreYouSure_dfgdfgdgOpen"

    def getModalHeaderId() = "OpenFileModal_AreYouSure_Header_sdkjhsdkfj"

    def apply() = {

      div(cls := "modal fade", tabindex := "-1", role := "dialog", id := getId(),
        div(cls := "modal-dialog", role := "document",
          div(cls := "modal-content",
            div(cls := "modal-header",
              h5(id := getModalHeaderId(), cls := "modal-title", s"Really delete file?"),
              button(`type` := "button", cls := "close", data.dismiss := "modal", aria.label := "Close", span(aria.hidden := "true", "\u00d7"))
            ),
            div(cls := "modal-body",
              p("Deleting this file will also delete all of its cases!")
            ),
            div(cls := "modal-footer",
              button(`type` := "button", cls := "btn btn-secondary", data.dismiss := "modal", "Cancel"),
              button(`type` := "button", cls := "btn btn-primary", data.dismiss := "modal", onclick := { (event: Event) => requestFileDeletion() }, "Delete")
            )
          )
        )
      )
    }
  }

  object MainModal extends OorepHtmlElement {
    def getId() = "OpenFileModal_MainModal_dsfkljsdkljh23P43efsdjkfjhkl345klHGg345jkl"

    object SubmitButton extends OorepHtmlButton {
      def getId() = "OpenFileModal_SubmitButton_JHGjk345bfdh5mer345jkldfgf"

      def apply() = {
        button(cls := "btn btn-primary mb-2", style := "margin-left:8px;", id := getId(), `type` := "button", disabled := true,
          data.toggle := "modal", data.dismiss := "modal", data.target := s"#${EditFileModal.getId()}",
          onclick := { (event: Event) =>
            document.body.style.cursor = "wait"
            EditFileModal.update(selected_file_header.getOrElse("SOMETHING WENT WRONG"), selected_file_id.getOrElse(-1).toString)
            Case.updateCurrOpenFile(selected_file_id)
          },
          "Open"
        )
      }
    }

    object DeleteButton extends OorepHtmlButton {
      def getId() = "OpenFileModal_DeleteButton_52345JfdgfgfHgsdfG3345rthjk3455jkldfgfGJK"

      def apply() = {
        button(cls := "btn mb-2 btn-secondary", style := "margin-left:8px;", id := getId(), data.toggle := "modal", data.dismiss := "modal",
          data.target := s"#${AreYouSureModal.getId()}", disabled := true,
          onclick := { (ev: Event) =>
            // Set file header in the AreYouSureModal before displaying it
            dom.document.getElementById(AreYouSureModal.getModalHeaderId()) match {
              case null => ;
              case modal => modal.asInstanceOf[dom.html.Heading].textContent = s"Really delete file ${selected_file_header.getOrElse("")}?"
            }
          },
          "Delete"
        )
      }
    }

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", id := getId(),
        onshow := { (event: Event) =>
          updateData()

          if (selected_file_id == None) {
            DeleteButton.disable()
            SubmitButton.disable()
          }
        },
        div(cls := "modal-dialog modal-dialog-centered", role := "document", style := "min-width: 80%;",
          div(cls := "modal-content",
            div(cls := "modal-header",
              h5(cls := "modal-title", "Select file"),
              button(`type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
            ),
            div(cls := "modal-body",
              modalBodyFileSelection(),
              div(cls := "form-group d-flex flex-row-reverse",
                SubmitButton(),
                DeleteButton(),
                button(data.dismiss := "modal", cls := "btn mb-2 btn-secondary",
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

  def enableButtons(): Unit = {
    MainModal.SubmitButton.enable()
    MainModal.DeleteButton.enable()
  }

  def disableButtons(): Unit = {
    MainModal.SubmitButton.disable()
    MainModal.DeleteButton.disable()
  }

  def apply() = div(AreYouSureModal(), MainModal())
}
