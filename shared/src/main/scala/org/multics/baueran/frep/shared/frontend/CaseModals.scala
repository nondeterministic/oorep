package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import dom.Event
import dom.raw.HTMLInputElement
import scalatags.JsDom.all._
import org.querki.jquery.$
import scala.scalajs.js

import org.multics.baueran.frep.shared
import shared.Defs.CookieFields

object CaseModals {

  // ------------------------------------------------------------------------------------------------------------------
  // The modal-dialog skeleton for the case repertorisation view
  object Repertorisation {

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", scalatags.JsDom.attrs.id := "caseAnalysisModal",
        div(cls := "modal-dialog modal-dialog-centered", role := "document", style := "min-width: 80%;",
          div(cls := "modal-content",
            div(cls := "modal-header",
              h5(cls := "modal-title", "Case repertorisation"),
              button(`type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
            ),
            div(cls := "modal-body",
              div(cls := "table-responsive",
                table(cls := "table case table-striped table-hover table-sm table-bordered",
                  thead(scalatags.JsDom.attrs.id := "analysisTHead",
                    th(attr("scope") := "col", "W."),
                    th(attr("scope") := "col", "Rep."),
                    th(attr("scope") := "col", "Rubric")
                  ),
                  tbody(scalatags.JsDom.attrs.id := "analysisTBody")
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
  object EditDescription {

    private val html =
      div(cls := "modal fade", tabindex := "-1", role := "dialog", scalatags.JsDom.attrs.id := "caseDescriptionModal",
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
                    label(`for` := "caseDescrId", "ID"),
                    input(cls := "form-control", id := "caseDescrId", placeholder := "A simple, unique case identifier", required)
                  ),
                  div(cls := "form-group",
                    label(`for` := "caseDescrDescr", "Description"),
                    textarea(cls := "form-control", id := "caseDescrDescr", rows := "3", placeholder := "A more verbose description of the case")
                  ),
                  div(cls := "d-flex flex-row-reverse",
                    button(cls := "btn btn-primary mb-2", style := "margin-left:8px;", `type` := "button",
                      "Submit",
                      onclick := { (event: Event) =>
                        event.stopPropagation()

                        val caseIdTxt = dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].value
                        dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].setAttribute("readonly", "readonly")
                        val caseDescrTxt = dom.document.getElementById("caseDescrDescr").asInstanceOf[HTMLInputElement].value
                        val memberId = getCookieData(dom.document.cookie, CookieFields.id.toString) match {
                          case Some(id) => id.toInt
                          case None => -1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
                        }

                        Case.descr = Some(shared.Caze( // WHERE CAZE IS INITIALLY CREATED: WITH ID -1!
                          (if (Case.descr.isDefined) Case.descr.get.id else -1),
                          caseIdTxt,
                          memberId,
                          (new js.Date()).toISOString(),
                          caseDescrTxt,
                          Case.cRubrics))

                        dom.document.getElementById("caseHeader").textContent = s"Case '${Case.descr.get.header}':"
                        $("#openNewCaseButton").hide()
                        $("#editDescrButton").show()
                        $("#closeCaseButton").show()
                        $("#addToFileButton").removeAttr("disabled")
                        js.eval("$('#caseDescriptionModal').modal('hide');")

                        Case.updateCaseViewAndDataStructures()
                      }),
                    button(data.dismiss := "modal", cls := "btn mb-2 btn-secondary",
                      "Cancel",
                      onclick := { (event: Event) =>
                        Case.descr match {
                          case Some(descr) =>
                            $("#caseDescrId").`val`(descr.header)
                            $("#caseDescrDescr").`val`(descr.description)
                          case None =>
                            $("#caseDescrId").`val`("")
                            $("#caseDescrDescr").`val`("")
                        }
                      }
                    )
                  )
                )
              )
            )
          )
        )
      )

    def setDescriptionId(newId: String) = $("#caseDescrId").`val`(newId)

    def setDescription(newDescr: String) = $("#caseDescrDescr").`val`(newDescr)

    def setCaseDescriptionIdEditable() =
      if (dom.document.getElementById("caseDescrId") != null)
        dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].removeAttribute("readonly")

    def setCaseDescriptionIdReadOnly() =
      if (dom.document.getElementById("caseDescrId") != null)
        dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].removeAttribute("readonly")

    def apply() = html
  }

}
