package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import dom.Event
import scalatags.JsDom.all.{onkeydown, _}

import scala.scalajs.js
import org.multics.baueran.frep.shared
import shared.Defs.CookieFields

object CaseModals {

  // ------------------------------------------------------------------------------------------------------------------
  // The modal-dialog skeleton for the case repertorisation view
  object RepertorisationModal extends OorepHtmlElement {
    def getId() = "caseAnalysisModal"

    object TableHead extends OorepHtmlElement {
      def getId() = "analysisTHead"
      def apply() = {
        thead(scalatags.JsDom.attrs.id := getId(),
          th(attr("scope") := "col", "W."),
          th(attr("scope") := "col", "Rep."),
          th(attr("scope") := "col", "Rubric")
        )
      }
    }

    object TableBody extends OorepHtmlElement {
      def getId() = "analysisTBody"
      def apply() = {
        tbody(scalatags.JsDom.attrs.id := getId())
      }
    }

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", scalatags.JsDom.attrs.id := getId(),
        div(cls := "modal-dialog modal-dialog-centered", role := "document", style := "min-width: 80%;",
          div(cls := "modal-content",
            div(cls := "modal-header",
              h5(cls := "modal-title", "Case repertorisation"),
              button(`type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
            ),
            div(cls := "modal-body",
              div(cls := "table-responsive",
                table(cls := "table case table-striped table-hover table-sm table-bordered",
                  TableHead(),
                  TableBody()
                )
              )
            )
          )
        )
      )
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // The modal-dialog HTML-code for editing case description
  object EditModal extends OorepHtmlElement {
    def getId() = "caseDescriptionModal"

    object CancelButton extends OorepHtmlButton {
      def getId() = EditModal.getId() + "DOESNTMATTER_ISNOTUSED"

      def apply() = {
        button(data.dismiss := "modal", cls := "btn mb-2 btn-secondary",
          "Cancel",
          onclick := { (event: Event) =>
            Case.descr match {
              case Some(descr) =>
                CaseIdInput.setText(descr.header)
                CaseDescriptionTextArea.setText(descr.description)
              case None =>
                CaseIdInput.setText("")
                CaseDescriptionTextArea.setText("")
            }
          }
        )
      }
    }

    def onSubmit = (event: Event) => {
      val caseIdTxt = CaseIdInput.getText ()
      CaseIdInput.setReadOnly ()
      val caseDescrTxt = CaseDescriptionTextArea.getText ()
      val memberId = getCookieData (dom.document.cookie, CookieFields.id.toString) match {
        case Some (id) => id.toInt
        case None => - 1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
      }

      Case.descr = Some (shared.Caze ( // WHERE CAZE IS INITIALLY CREATED: WITH ID -1!
        (if (Case.descr.isDefined) Case.descr.get.id else - 1),
        caseIdTxt,
        memberId,
        (new js.Date () ).toISOString (),
        caseDescrTxt,
        Case.cRubrics) )

      Case.CaseHeader.setHeaderText (s"Case '${Case.descr.get.header}': ")
      Case.updateCaseHeaderView () // This mainly updates the buttons at bottom of case view
      Case.updateCaseViewAndDataStructures ()
    }

    object SubmitButton extends OorepHtmlButton {
      def getId() = "caseModalsSubmit"

      def apply() = {
        button(id := getId(), cls := "btn btn-primary mb-2", style := "margin-left:8px;", `type` := "submit", disabled := true,
          data.toggle:="modal", data.dismiss:="modal", "Submit",
          onclick := onSubmit
        )
      }
    }

    object CaseIdInput extends OorepHtmlInput {
      def getId() = "caseDescrId"

      def apply() = {
        input(cls := "form-control", id := getId(), placeholder := "A simple, unique case identifier", required,
          onkeyup := { (event: dom.KeyboardEvent) =>
            if (getText().trim.length > 0) {
              SubmitButton.enable()

              if (event.key == "Enter")
                onSubmit(event)
            } else {
              SubmitButton.disable()
            }
          }
        )
      }
    }

    object CaseDescriptionTextArea extends OorepHtmlTextArea {
      def getId() = "caseDescrDescr"

      def apply() = {
        textarea(cls := "form-control", id := getId(), rows := "3", placeholder := "A more verbose description of the case",
          onkeyup := { (event: Event) =>
            val currTextAreaText = getText()

            if (currTextAreaText.trim.length > 0 && getText().trim.length > 0 && Case.descr.isDefined && currTextAreaText.trim != Case.descr.get.description)
              SubmitButton.enable()
            else if (currTextAreaText.trim.length > 0 && Case.descr.isDefined && currTextAreaText.trim == Case.descr.get.description)
              SubmitButton.disable()
          }
        )
      }
    }

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", scalatags.JsDom.attrs.id := getId(),
        div(cls := "modal-dialog modal-dialog-centered", role := "document", style := "min-width: 80%;",
          div(cls := "modal-content",
            div(cls := "modal-header",
              h5(cls := "modal-title", "Case description"),
              button(`type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
            ),
            div(cls := "modal-body",
              div(cls := "table-responsive",
                form(
                  div(cls := "form-group",
                    label(`for` := CaseIdInput.getId(), "ID"),
                    CaseIdInput()
                  ),
                  div(cls := "form-group",
                    label(`for` := CaseDescriptionTextArea.getId(), "Description"),
                    CaseDescriptionTextArea()
                  ),
                  div(cls := "d-flex flex-row-reverse",
                    SubmitButton(),
                    CancelButton()
                  )
                )
              )
            )
          )
        )
      )
    }
  }

}
