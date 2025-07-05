package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.{HttpRequest2, Member}
import org.multics.baueran.frep.shared.frontend.{Notify, OorepHtmlButton, OorepHtmlElement, OorepHtmlInput}
import scalatags.JsDom.all.*
import org.scalajs.dom
import org.scalajs.dom.Event
import io.circe.parser.parse

object SettingsModal extends OorepHtmlElement {
  def getId() = "SettingsModal_UniqueId_783424hjkshdfuih324793284"

  private var settingsUsername = ""
  private var settingsEmail = ""

  def updateWithDataFromDB(memberId: Int) = {
    HttpRequest2("sec/settings")
      .withQueryParameters("memberId" -> memberId.toString)
      .onSuccess((response: String) =>
        parse(response) match {
          case Right(jsonMember) => {
            val cursor = jsonMember.hcursor
            cursor.as[Member] match {
              case Right(member) => {
                settingsUsername = member.member_name
                settingsEmail = member.email
              }
              case Left(error) => {
                println(s"SettingsModal: Received corrupted member data from backend: ${error}")
              }
            }
          }
          case Left(error) => {
            println(s"SettingsModal: Received corrupted JSON data from backend: ${error}")
          }
        }
      )
      .send()
  }

  private object CloseButton extends OorepHtmlButton {
    def getId() = "SettingsModal_CloseButton_982347dfg"
    def apply() = {

      button(id := getId(), `type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
    }
  }

  private object Form extends OorepHtmlElement {
    def getId() = "SettingsModal_Form_892347932847923"

    object SubmitButton extends OorepHtmlButton {
      def getId() = "SettingsModal_Form_SubmitButton_23847923"

      def onSubmit = (event: Event) => {
        event.preventDefault()

        val username = UsernameInput.getText().trim
        val email = EmailInput.getText().trim

        if (username.length == 0 || email.length == 0) {
          if (Notify.noAlertsVisible())
            new Notify("tempFeedbackAlert", "Saving settings failed. Fields cannot be empty!")
        } else {
          // TODO: Save settings
          //
          //          HttpRequest2("sec/save_settings")
          //            .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
          //            .onSuccess((_) => {
          //              CaseSection.updateCaseViewAndDataStructures()
          //              SettingsModal.CloseButton.click()
          //            })
          //            .onFailure((_) => {
          //              if (Notify.noAlertsVisible())
          //                new Notify("tempFeedbackAlert", "Saving settings failed. Server error.")
          //            })
          //            .post(
          //              "username" -> username,
          //              "email" -> email
          //            )
        }
      }

      def apply() = {
        button(cls := "btn btn-primary mb-2", style := "margin-left:8px;", `type` := "submit", id := getId(), disabled := true,
          onclick := onSubmit,
          "Submit")
      }
    }

    object CancelButton extends OorepHtmlButton {
      def getId() = "SettingsModal_Form_CancelButton_23849723"
      def apply() = {
        button(cls := "btn mb-2 btn-secondary", data.dismiss := "modal",
          "Cancel",
          onclick := { (event: Event) =>
            event.preventDefault()
            UsernameInput.setText("")
            EmailInput.setText("")
          })
      }
    }

    object UsernameInput extends OorepHtmlInput {
      def getId() = "SettingsModal_Form_UsernameInput_9823749"
      def apply() = {
        input(cls := "form-control",
          id := getId(),
          placeholder := "Enter your username",
          required,
          oninput := { (event: dom.KeyboardEvent) =>
            validateInputs()
          }
        )
      }
    }

    object EmailInput extends OorepHtmlInput {
      def getId() = "SettingsModal_Form_EmailInput_2384972"
      def apply() = {
        input(`type` := "email", cls := "form-control",
          id := getId(),
          placeholder := "Enter your email address",
          required,
          oninput := { (event: dom.KeyboardEvent) =>
            validateInputs()
          }
        )
      }
    }

    private def validateInputs(): Unit = {
      // TODO: For now, the settings dialog cannot be edited, only cancelled.
      //  To allow editing, uncomment the following and enable the write method to transmit changes back to the backend.
      //
      //      if (UsernameInput.getText().trim.length > 0 && EmailInput.getText().trim.length > 0)
      //        SubmitButton.enable()
      //      else
      //        SubmitButton.disable()
    }

    def apply() = {
      form(id := getId(),
        // Username row: label and input right next to each other
        div(cls := "form-group row align-items-center",
          label(`for` := UsernameInput.getId(), cls := "col-sm-2 col-form-label", "Username:"),
          div(cls := "col-sm-10",
            UsernameInput()
          )
        ),
        // E-Mail row: label and input right next to each other
        div(cls := "form-group row align-items-center",
          label(`for` := EmailInput.getId(), cls := "col-sm-2 col-form-label", "E-Mail:"),
          div(cls := "col-sm-10",
            EmailInput()
          )
        ),
        // Bottom buttons row
        div(cls := "d-flex flex-row-reverse mt-4",
          SubmitButton(),
          CancelButton()
        )
      )
    }
  }

  def apply() = {
    div(cls := "modal fade", tabindex := "-1", role := "dialog", id := getId(),
      onshow := { (event: Event) =>
        Form.UsernameInput.setText(settingsUsername)
        Form.EmailInput.setText(settingsEmail)
        Form.SubmitButton.disable()
      },
      div(cls := "modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls := "modal-content",
          div(cls := "modal-header",
            h5(cls := "modal-title", "Settings"),
            CloseButton()
          ),
          div(cls := "modal-body",
            Form()
          )
        )
      )
    )
  }

}
