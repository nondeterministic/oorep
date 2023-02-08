package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.FIle
import org.multics.baueran.frep.shared.frontend.{Case, Notify, OorepHtmlButton, OorepHtmlElement, OorepHtmlInput, OorepHtmlTextArea, apiPrefix, getCookieData, serverUrl}
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.PlainTextBody
import fr.hmil.roshttp.response.SimpleHttpResponse
import scalatags.JsDom.all.{id, input, _}
import org.scalajs.dom
import org.scalajs.dom.Event
import monix.execution.Scheduler.Implicits.global

import scala.scalajs.js
import scala.util.Success
import io.circe.syntax._

object NewFileModal extends OorepHtmlElement {
  def getId() = "NewFileModal_EEEdsfakjDD345nmbv8f89HG34jkdf8934RSCjfr8943jkarsklfg"

  object CloseButton extends OorepHtmlButton {
    def getId() = "NewFileModal_CloseButton_sdfdsf24532"

    def apply() = {
      button(id := getId(), `type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
    }
  }

  object Form extends OorepHtmlElement {
    def getId() = "NewModal_Form_7659587fdvdf032424jhbjd32sfGGHjhHKJEEWYYCVVKGHFJHD56476476"

    object SubmitButton extends OorepHtmlButton {
      def getId() = "NewFileModal_Form_SubmitButton_dsfsdfkjhsdklj324234nkbsdfkljh34rt34kl"

      def onSubmit = (event: Event) => {
        event.preventDefault() // Without this, the submit button also closes on wrong input and reloads the application

        val memberId = getCookieData(dom.document.cookie, CookieFields.id.toString) match {
          case Some(id) => id.toInt
          case None => -1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
        }

        if (HeaderInput.getText().trim.length == 0 || OpenFileModal.headers().count(_ == HeaderInput.getText().trim) != 0) {
          if (Notify.noAlertsVisible())
            new Notify("tempFeedbackAlert", "Saving file failed. Make sure file ID is unique and not empty!")
        }
        else {
          val currFIle = Some(FIle(None, HeaderInput.getText().trim, memberId, (new js.Date()).toISOString(), DescriptionTextArea.getText(), List.empty))

          HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/save_file")
            .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
            .post(PlainTextBody(currFIle.get.asJson.toString()))
            .onComplete({
              case _: Success[SimpleHttpResponse] => {
                Case.updateCaseViewAndDataStructures()
                HeaderInput.setText("")
                DescriptionTextArea.setText("")
                NewFileModal.CloseButton.click()
              }
              case _ => {
                if (Notify.noAlertsVisible())
                  new Notify("tempFeedbackAlert", "Saving file failed. Make sure file ID is unique!")
              }
            })
        }
      }

      def apply() = {
        button(cls := "btn btn-primary mb-2", style := "margin-left:8px;", `type` := "submit", id := getId(), disabled := true,
          onclick := onSubmit,
          "Submit")
      }
    }

    object CancelButton extends OorepHtmlButton {
      def getId() = "NewFileModal_Form_CancelButton_dsfHJGJHGksdfkjhsdklj324234nkbsdfkljh34rt34kl"

      def apply() = {
        button(cls := "btn mb-2 btn-secondary", data.dismiss := "modal",
          "Cancel",
          onclick := { (event: Event) =>
            event.preventDefault()
            HeaderInput.setText("")
            DescriptionTextArea.setText("")
          })
      }
    }

    object HeaderInput extends OorepHtmlInput {
      def getId() = "NewFileModal_Form_HeaderInput_nbmnbNMBNMBHJGJHGUIF7657687ghzxcjksd23234IFY"

      def apply() = {
        input(cls := "form-control",
          id := getId(),
          placeholder := "A simple, unique file identifier",
          required,
          oninput := { (event: dom.KeyboardEvent) =>
            if (getText().trim.length > 0)
              SubmitButton.enable()
            else
              SubmitButton.disable()
          }
        )
      }
    }
    object DescriptionTextArea extends OorepHtmlTextArea {
      def getId() = "NewFileModal_Form_DescriptionTextArea_HJJHGJKGJGKJG4534534532535345345423"

      def apply() =
        textarea(cls := "form-control", id := getId(), rows := "3", placeholder := "A more verbose description of the file")
    }

    def apply() = {
      form(id := getId(),
        div(cls := "form-group",
          label(`for` := HeaderInput.getId(), "ID"),
          HeaderInput()
        ),
        div(cls := "form-group",
          label(`for` := DescriptionTextArea.getId(), "Description"),
          DescriptionTextArea()
        ),
        div(cls := "d-flex flex-row-reverse",
          SubmitButton(),
          CancelButton()
        )
      )
    }
  }

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:=getId(),
      onshow := { (event: Event) => Form.SubmitButton.disable() },
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Create a new file"),
            CloseButton(),
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              Form()
            )
          )
        )
      )
    )
  }
}
