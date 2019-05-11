package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import dom.Event
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.PlainTextBody
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import scalatags.JsDom.all._

import scalajs.js
import scala.collection.mutable
import rx.Var
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._
import org.multics.baueran.frep.shared
import shared._
import shared.Defs.serverUrl
import shared.sec_frontend.NewFileModal.currFIle
import shared.frontend.RemedyFormat.RemedyFormat
import shared.sec_frontend.FileModalCallbacks._
import org.scalajs.dom.raw.HTMLInputElement
import org.querki.jquery.$
import org.scalajs.dom

import scala.util.{Failure, Success}

object Case {

  var descr: Option[shared.Caze] = None
  var cRubrics = mutable.ArrayBuffer[CaseRubric]()
  var remedyScores = mutable.HashMap[String,Integer]()
  private var prevCase: Option[shared.Caze] = None

  // ------------------------------------------------------------------------------------------------------------------
  def size() = cRubrics.size

  // ------------------------------------------------------------------------------------------------------------------
  def addRepertoryLookup(r: CaseRubric) = {
    if (cRubrics.filter(cr => cr.rubric.id == r.rubric.id && cr.repertoryAbbrev == r.repertoryAbbrev).length == 0)
      cRubrics += r
  }

  // ------------------------------------------------------------------------------------------------------------------
  def updateCaseViewAndDataStructures() = {
    def updateAllCaseDataStructures() = {
      val memberId = getCookieData(dom.document.cookie, "oorep_member_id") match {
        case Some(id) => updateMemberFiles(id.toInt); id.toInt
        case None => println("WARNING: updateDataStructures() failed. Could not get memberID from cookie."); -1
      }

      remedyScores.clear()
      cRubrics.foreach(caseRubric => {
        caseRubric.weightedRemedies.foreach { case WeightedRemedy(r, w) => {
          remedyScores.put(r.nameAbbrev, remedyScores.getOrElseUpdate(r.nameAbbrev, 0) + caseRubric.rubricWeight * w)
        }}
      })

      if (descr != None) {
        descr = Some(shared.Caze(descr.get.id, descr.get.header, descr.get.member_id, descr.get.date, descr.get.description, cRubrics.toList))

        // If user is logged in, attempt to update case in DB (if it exists; see comment in Post.scala),
        // and if previous case != current case...
        if (memberId >= 0 && prevCase != descr) {
          // Before we write the case to disk, we update the date to record the change.
          // We do not do this above, as the prevCase != descr check would always fail then!
          descr = Some(shared.Caze(descr.get.id, descr.get.header, descr.get.member_id, (new js.Date()).toISOString(), descr.get.description, cRubrics.toList))

          HttpRequest(serverUrl() + "/updatecase")
            .post(PlainTextBody(Caze.encoder(descr.get).toString()))
        }
      }

      if (cRubrics.size == 0)
        descr = None
    }

    // Update data structures first
    updateAllCaseDataStructures()

    // Now, put previous case to current case
    prevCase = descr

    // Redraw table header
    $("#analysisTHead").empty()
    $("#analysisTHead").append(th(attr("scope"):="col", "W.").render)
    $("#analysisTHead").append(th(attr("scope"):="col", "Rep.").render)
    $("#analysisTHead").append(th(attr("scope"):="col", "Symptom").render)
    remedyScores.toList.sortWith(_._2 > _._2).map(_._1).foreach(abbrev =>
      $("#analysisTHead").append(th(attr("scope") := "col", div(cls:="vertical-text", style:="width: 30px;", abbrev)).render))

    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.

    // Redraw table body
    $("#analysisTBody").empty()
    for (cr <- cRubrics) {
      val trId = cr.rubric.fullPath.replaceAll("[^A-Za-z0-9]", "") + "_" + cr.repertoryAbbrev

      $("#analysisTBody").append(
        tr(scalatags.JsDom.attrs.id := trId,
          td(cr.rubricWeight.toString()),
          td(cr.repertoryAbbrev),
          td(style := "white-space: nowrap;", data.toggle := "tooltip", title := cr.rubric.fullPath, cr.rubric.fullPath.shorten)
        ).render)

      remedyScores.toList.sortWith(_._2 > _._2).map(_._1) foreach (abbrev => {
        if (cr.rubricWeight > 0 && cr.containsRemedyAbbrev(abbrev))
          $("#" + trId).append(td(data.toggle := "tooltip", title := abbrev, "" + (cr.getRemedyWeight(abbrev) * cr.rubricWeight)).render)
        else
          $("#" + trId).append(td(data.toggle := "tooltip", title := abbrev, " ").render)
      })
    }
  }

  def updateCaseHeaderView() = {
    getCookieData(dom.document.cookie, "oorep_member_id") match {
      // Not logged in...
      case None =>
        $("#openNewCaseButton").hide()
        $("#editDescrButton").hide()
        $("#closeCaseButton").hide()
      // Logged in...
      case Some(_) =>
        descr match {
          // Case doesn't exist...
          case None =>
            $("#openNewCaseButton").show()
            $("#editDescrButton").hide()
            $("#closeCaseButton").hide()
            $("#addToFileButton").attr("disabled", true)
          // Case exists...
          case Some(_) =>
            $("#openNewCaseButton").hide()
            $("#editDescrButton").show()
            $("#closeCaseButton").show()
            $("#addToFileButton").removeAttr("disabled")
        }
    }
  }

  // ------------------------------------------------------------------------------------------------------------------
  // The modal-dialog HTML-code for showing the case analysis
  def analysisModalDialogHTML() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", scalatags.JsDom.attrs.id:="caseAnalysisModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        // style:="left: 10%; min-width: 80%; margin-left:0px; margin-right:0px; margin-top:20px;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Case analysis"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              table(cls:="table table-striped table-hover table-sm table-bordered",
                thead(scalatags.JsDom.attrs.id:="analysisTHead", style:="padding: 1px !important; height: 110px;",
                  th(attr("scope"):="col", "W."),
                  th(attr("scope"):="col", "Rep."),
                  th(attr("scope"):="col", style:="padding: 1px !important; height: 150px; width: 100%;", "Symptom")
                ),
                tbody(scalatags.JsDom.attrs.id:="analysisTBody")
              )
            )
          )
        )
      )
    )
  }

  // ------------------------------------------------------------------------------------------------------------------
  // The modal-dialog HTML-code for editing case description
  def editDescrModalDialogHTML() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", scalatags.JsDom.attrs.id:="caseDescriptionModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Case description"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              form(
                div(cls:="form-group",
                  label(`for`:="caseDescrId", "ID"),
                  input(cls:="form-control", id:="caseDescrId", placeholder:="A simple, unique case identifier", required)
                ),
                div(cls:="form-group",
                  label(`for`:="caseDescrDescr", "Description"),
                  textarea(cls:="form-control", id:="caseDescrDescr", rows:="3", placeholder:="A more verbose description of the case")
                ),
                div(
                  button(data.dismiss:="modal", cls:="btn mb-2",
                    "Cancel",
                    onclick:={ (event: Event) =>
                      descr match {
                        case Some(descr) =>
                          $("#caseDescrId").`val`(descr.header)
                          $("#caseDescrDescr").`val`(descr.description)
                        case None =>
                          $("#caseDescrId").`val`("")
                          $("#caseDescrDescr").`val`("")
                      }
                    }),
                  button(cls:="btn btn-primary mb-2", `type`:="button",
                    "Submit",
                    onclick:={(event: Event) =>
                      event.stopPropagation()

                      val caseIdTxt = dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].value
                      dom.document.getElementById("caseDescrId").asInstanceOf[HTMLInputElement].setAttribute("readonly", "readonly")
                      val caseDescrTxt = dom.document.getElementById("caseDescrDescr").asInstanceOf[HTMLInputElement].value
                      val memberId = getCookieData(dom.document.cookie, "oorep_member_id") match {
                        case Some(id) => id.toInt
                        case None => -1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
                      }

                      descr = Some(shared.Caze(0, caseIdTxt, memberId, (new js.Date()).toISOString(), caseDescrTxt, cRubrics.toList))
                      dom.document.getElementById("caseHeader").textContent = s"Case '${descr.get.header}':"
                      $("#openNewCaseButton").hide()
                      $("#editDescrButton").show()
                      $("#closeCaseButton").show()
                      $("#addToFileButton").removeAttr("disabled")
                      js.eval("$('#caseDescriptionModal').modal('hide');")

                      updateCaseViewAndDataStructures()
                    })
                )
              )
            )
          )
        )
      )
    )
  }

  // ------------------------------------------------------------------------------------------------------------------
  def toHTML(remedyFormat: RemedyFormat) = {
    updateCaseViewAndDataStructures()

    def caseRow(crub: CaseRubric) = {
      implicit def crToCR(cr: CaseRubric) = new BetterCaseRubric(cr)

      val remedies =
        if (remedyFormat == RemedyFormat.NotFormatted)
          crub.getRawRemedies()
        else
          crub.getFormattedRemedies()

      // The weight label on the drop-down button, which needs to change automatically on new user choice
      val weight = Var(crub.rubricWeight.toString())

      tr(scalatags.JsDom.attrs.id := "crub_" + crub.rubric.id + crub.repertoryAbbrev,
        td(
          button(`type` := "button", cls := "btn dropdown-toggle btn-sm", style := "width: 45px;", data.toggle := "dropdown", weight),
          div(cls := "dropdown-menu",
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 0; weight() = "0" }, "0 (ignore)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 1; weight() = "1" }, "1 (normal)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 2; weight() = "2" }, "2 (important)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 3; weight() = "3" }, "3 (very important)"),
            a(cls := "dropdown-item", href := "#caseSectionOfPage", onclick := { (event: Event) => crub.rubricWeight = 4; weight() = "4" }, "4 (essential)")
          )
        ),
        td(crub.repertoryAbbrev),
        td(crub.rubric.fullPath),
        td(remedies.take(remedies.size - 1).map(l => span(l, ", ")) ::: List(remedies.last)),
        td(cls := "text-right",
          button(cls := "btn btn-sm", `type` := "button",
            scalatags.JsDom.attrs.id := ("rmBut_" + crub.rubric.id + crub.repertoryAbbrev),
            style := "vertical-align: middle; display: inline-block",
            onclick := { (event: Event) => {
              event.stopPropagation()
              crub.rubricWeight = 1
              cRubrics.remove(cRubrics.indexOf(crub))
              $("#crub_" + crub.rubric.id + crub.repertoryAbbrev).remove()

              // Enable add-button in results, if removed symptom was in the displayed results list...
              $("#button_" + crub.repertoryAbbrev + "_" + crub.rubric.id).removeAttr("disabled")

              // If this was last case-rubric, clear case div
              if (cRubrics.size == 0)
                $("#caseDiv").empty()

              updateCaseViewAndDataStructures()
            }
            }, "Remove")
        )
      )
    }

    def header() = {
      val analyseButton =
        button(cls:="btn btn-sm btn-primary", `type`:="button", data.toggle:="modal", data.target:="#caseAnalysisModal", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            updateCaseViewAndDataStructures()
          }},
          "Analyse")
      val editDescrButton =
        button(cls:="btn btn-sm btn-dark", id:="editDescrButton", `type`:="button", data.toggle:="modal", data.target:="#caseDescriptionModal", style:="display: none; margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            descr match {
              case Some(descr) =>
                $("#caseDescrId").`val`(descr.header)
                $("#caseDescrDescr").`val`(descr.description)
              case None => ;
            }
          }
          }, "Edit case description")
      val openNewCaseButton =
        button(cls:="btn btn-sm btn-dark", id:="openNewCaseButton", `type`:="button", data.toggle:="modal", data.target:="#caseDescriptionModal", style:="margin-left:5px; margin-bottom: 5px;", "Open new case")
      val closeCaseButton =
        button(cls:="btn btn-sm btn-dark", id:="closeCaseButton", `type`:="button", style:="display: none; margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            for (crub <- cRubrics) {
              crub.rubricWeight = 1
              $("#crub_" + crub.rubric.id + crub.repertoryAbbrev).remove()
              // Enable add-button in results, if removed symptom was in the displayed results list...
              $("#button_" + crub.repertoryAbbrev + "_" + crub.rubric.id).removeAttr("disabled")
            }
            cRubrics = new mutable.ArrayBuffer[CaseRubric]()
            $("#caseDiv").empty()
          }
          }, "Close case")
      val addToFileButton =
        button(cls:="btn btn-sm btn-dark", id:="addToFileButton", `type`:="button", data.toggle:="modal", data.target:="#addToFileModal", disabled:=true, style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            updateCaseViewAndDataStructures()
          }},
          "Add case to file")

      getCookieData(dom.document.cookie, "oorep_member_id") match {
        case Some(_) =>
          if (descr != None) {
            div(
              b(id:="caseHeader", "Case '" + descr.get.header + "':"),
              editDescrButton,
              closeCaseButton,
              addToFileButton,
              analyseButton
            )
          }
          else {
            div(
              b(id:="caseHeader", "Case: "),
              editDescrButton,
              openNewCaseButton,
              closeCaseButton,
              addToFileButton,
              analyseButton
            )
          }
        case None =>
          div(
            b(id:="caseHeader", "Case: "),
            analyseButton
          )
      }
    }

    div(cls:="container-fluid",
      analysisModalDialogHTML(),
      editDescrModalDialogHTML(),

      // HTML which is visible right from the start...
      div(header),
      div(cls:="table-responsive",
        table(cls:="table table-striped table-sm table-bordered",
          thead(cls:="thead-dark", scalatags.JsDom.attrs.id:="caseTHead",
            th(attr("scope"):="col", "Weight"),
            th(attr("scope"):="col", "Rep."),
            th(attr("scope"):="col", "Symptom"),
            th(attr("scope"):="col",
              a(scalatags.JsDom.attrs.id:="caseSectionOfPage",
                cls:="underline", href:="#caseSectionOfPage", style:="color:white;",
                onclick:={ (event: Event) => $("#remediesFormatButton").click() },
                "Remedies")
            ),

            th(attr("scope"):="col", " ")
          ),
          tbody(scalatags.JsDom.attrs.id:="caseTBody", cRubrics.map(crub => caseRow(crub)))
        )
      )
    )
  }
}
