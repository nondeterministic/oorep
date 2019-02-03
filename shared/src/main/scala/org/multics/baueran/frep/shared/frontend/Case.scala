package org.multics.baueran.frep.shared.frontend

import java.util.Date
import java.text.SimpleDateFormat

import org.querki.jquery.$
import org.scalajs.dom
import dom.Event
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import scalatags.JsDom.all._

import scalajs.js
import scala.collection.mutable
import rx.Var
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._
import org.multics.baueran.frep.shared
import org.scalajs.dom.raw.HTMLInputElement
import shared._
import shared.Defs.{AppMode, serverUrl}
import shared.frontend.RemedyFormat.RemedyFormat

import scala.util.Success

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe._, io.circe.parser._
import io.circe.syntax._

object Case {

  var descr: Option[shared.Caze] = None
  var cRubrics = mutable.ArrayBuffer[CaseRubric]()
  var remedyScores = mutable.HashMap[String,Integer]()

  // ------------------------------------------------------------------------------------------------------------------
  def size() = cRubrics.size

  // ------------------------------------------------------------------------------------------------------------------
  def addRepertoryLookup(r: CaseRubric) = {
    if (cRubrics.filter(cr => cr.rubric.id == r.rubric.id && cr.repertoryAbbrev == r.repertoryAbbrev).length == 0)
      cRubrics += r
  }

  // ------------------------------------------------------------------------------------------------------------------
  def updateDataStructures() = {
    remedyScores.clear()
    cRubrics.foreach(caseRubric => {
      caseRubric.weightedRemedies.foreach { case WeightedRemedy(r, w) => {
        remedyScores.put(r.nameAbbrev, remedyScores.getOrElseUpdate(r.nameAbbrev, 0) + caseRubric.rubricWeight * w)
      }}
    })

    if (descr != None)
      descr = Some(shared.Caze(descr.get.id, descr.get.header, descr.get.member_id, (new js.Date()).toISOString(), descr.get.description, cRubrics.toList))

    if (cRubrics.size == 0)
      descr = None
  }

  // ------------------------------------------------------------------------------------------------------------------
  def updateAnalysisView() = {
    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.

    // Update data structures
    updateDataStructures()

    // Redraw table header
    $("#analysisTHead").empty()
    $("#analysisTHead").append(th(attr("scope"):="col", "W.").render)
    $("#analysisTHead").append(th(attr("scope"):="col", "Rep.").render)
    $("#analysisTHead").append(th(attr("scope"):="col", "Symptom").render)
    remedyScores.toList.sortWith(_._2 > _._2).map(_._1).foreach(abbrev =>
      $("#analysisTHead").append(th(attr("scope") := "col", div(cls:="vertical-text", style:="width: 30px;", abbrev)).render))

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
                  descr match {
                    case None => input(cls:="form-control", id:="caseDescrId", placeholder:="A simple, unique case identifier", required)
                    case Some(d) => input(cls:="form-control", id:="caseDescrId", placeholder:="A simple, unique case identifier", required, value:=d.description)
                  }
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
                      val caseDescrTxt = dom.document.getElementById("caseDescrDescr").asInstanceOf[HTMLInputElement].value
                      val memberId = getCookieData(dom.document.cookie, "oorep_member_id") match {
                        case Some(id) => id.toInt
                        case None => -1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
                      }

                      descr = Some(shared.Caze(0, caseIdTxt, memberId, (new js.Date()).toISOString(), caseDescrTxt, cRubrics.toList))
                      dom.document.getElementById("caseHeader").textContent = s"Case '${descr.get.header}':"
                      js.eval("$('#caseDescriptionModal').modal('hide');")
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
  def toHTML(remedyFormat: RemedyFormat, appMode: AppMode.AppMode) = {

    def caseRow(crub: CaseRubric) = {
      implicit def crToCR(cr: CaseRubric) = new BetterCaseRubric(cr)

      val remedies =
        if (remedyFormat == RemedyFormat.NotFormatted)
          crub.getRawRemedies()
        else
          crub.getFormattedRemedies()

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
              updateDataStructures()

              // Enable add-button in results, if removed symptom was in the displayed results list...
              $("#button_" + crub.repertoryAbbrev + "_" + crub.rubric.id).removeAttr("disabled")

              // If this was last case-rubric, clear case div
              if (cRubrics.size == 0)
                $("#caseDiv").empty()
            }
            }, "Remove")
        )
      )
    }

    def header() = {
      val analyseButton =
        button(cls:="btn btn-sm btn-primary", `type`:="button", data.toggle:="modal", data.target:="#caseAnalysisModal", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            updateAnalysisView()
          }}, "Analyse")
      val editDescrButton =
        button(cls:="btn btn-sm btn-dark", `type`:="button", data.toggle:="modal", data.target:="#caseDescriptionModal", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            descr match {
              case Some(descr) =>
                $("#caseDescrId").`val`(descr.header)
                $("#caseDescrDescr").`val`(descr.description)
              case None => ;
            }
          }
          }, "Edit description")
      val addToFileButton =
        button(cls:="btn btn-sm btn-dark", `type`:="button", data.toggle:="modal", data.target:="#TODO", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            println("descr.results.size: " + descr.get.results.size)
            HttpRequest(serverUrl() + "/savecase")
              // .post(PlainTextBody(Caze.encoder(descr.get).toString()))
              .post(PlainTextBody(descr.get.asJson.toString()))
              .onComplete({
                case response: Success[SimpleHttpResponse] => println("Received: " + response.toString())
                case _ => println("Failure!")
              })
            println("TODO2")
          }
          }, "Add to file")
      val removeFromFileButton =
        button(cls:="btn btn-sm btn-dark", `type`:="button", data.toggle:="modal", data.target:="#TODO", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            println("TODO")
          }
          }, "Remove from file")
      val showFileButton =
        button(cls:="btn btn-sm btn-dark", `type`:="button", data.toggle:="modal", data.target:="#TODO", style:="margin-left:5px; margin-bottom: 5px;",
          onclick := { (event: Event) => {
            println("TODO")
          }
          }, "Show file")

      if (appMode == AppMode.Secure && descr != None) {
        div(
          b(id:="caseHeader", "Case '" + descr.get.header + "':"),
          editDescrButton,
          addToFileButton,
          analyseButton
        )
      }
      else if (appMode == AppMode.Secure && descr == None) {
        div(
          b(id:="caseHeader", "Case: "),
          editDescrButton,
          addToFileButton,
          analyseButton
        )
      }
      else { // if (appMode == AppMode.Public)
        div(
          b(id:="caseHeader", "Case: "),
          editDescrButton,
          addToFileButton,
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
