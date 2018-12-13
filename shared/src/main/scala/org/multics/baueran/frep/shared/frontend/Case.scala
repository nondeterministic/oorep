package org.multics.baueran.frep.shared.frontend

import org.querki.jquery.$
import org.scalajs.dom.Event
import scalatags.JsDom.all._

import scala.collection.mutable

import rx.Var
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._

import org.multics.baueran.frep.shared.frontend.RemedyFormat.RemedyFormat
import org.multics.baueran.frep.shared.{BetterString, CaseRubric, BetterCaseRubric}

object Case {
  var id = ""
  var cRubrics = mutable.ArrayBuffer[CaseRubric]()
  var remedyScores = mutable.HashMap[String,Integer]()
  var remedyFormat = RemedyFormat.NotFormatted

  // ------------------------------------------------------------------------------------------------------------------
  def isEmpty() = id.length() == 0 && cRubrics.size == 0

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
      caseRubric.weightedRemedies.foreach { case (r, w) => {
        remedyScores.put(r.nameAbbrev, remedyScores.getOrElseUpdate(r.nameAbbrev, 0) + caseRubric.rubricWeight * w)
      }}
    })
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
  def toHTML(format: RemedyFormat) = {
    remedyFormat = format

    def caseRow(crub: CaseRubric) = {
      implicit def crToCR(cr: CaseRubric) = new BetterCaseRubric(cr)

      val remedies =
        if (remedyFormat == RemedyFormat.NotFormatted)
          crub.getRawRemedies()
        else
          crub.getFormattedRemedies()

      val weight = Var(crub.rubricWeight.toString())

      tr(scalatags.JsDom.attrs.id:="crub_" + crub.rubric.id + crub.repertoryAbbrev,
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
        td(cls:="text-right",
          button(cls:="btn btn-sm", `type`:="button",
            scalatags.JsDom.attrs.id:=("rmBut_" + crub.rubric.id + crub.repertoryAbbrev),
            style:="vertical-align: middle; display: inline-block",
            onclick:={ (event: Event) => {
              event.stopPropagation()
              cRubrics.remove(cRubrics.indexOf(crub))
              $("#crub_" + crub.rubric.id + crub.repertoryAbbrev).remove()
              updateDataStructures()

              // Enable add-button in results, if removed symptom was in the displayed results list...
              $("#button_" + crub.repertoryAbbrev + "_" + crub.rubric.id).removeAttr("disabled")
            }
            }, "Remove")
        )
      )
    }

    div(cls:="container-fluid",
      analysisModalDialogHTML(),

      // HTML which is visible right from the start...
      div(
        b("Case '" + id + "':"),
        button(cls:="btn btn-sm btn-dark", `type`:="button", data.toggle:="modal", data.target:="#caseAnalysisModal", style:="margin-left:25px; margin-bottom: 5px;",
          onclick:= { (event: Event) => {
            updateAnalysisView()
          }}, "Analyse...")),
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