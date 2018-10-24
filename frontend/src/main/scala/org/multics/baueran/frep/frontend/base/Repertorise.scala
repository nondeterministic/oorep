package org.multics.baueran.frep.frontend.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import scalatags.JsDom.tags.{input, label}
import org.scalajs.dom
import org.scalajs.dom.html._
import org.scalajs.dom.{Element, Event, MouseEvent, html}
import org.scalajs.dom.raw.HTMLFormElement
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.dom

import scala.collection.mutable
import org.querki.jquery._
import fr.hmil.roshttp.{BackendConfig, HttpRequest}
import org.scalajs.dom.XMLHttpRequest

import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import io.circe.Json
import io.circe._
import io.circe.parser._
import io.circe.generic.semiauto._
import org.scalajs.dom.raw.WebSocket
import rx.Ctx.Owner.Unsafe._
import rx._
import rx.{Rx, Var}
import org.multics.baueran.frep.shared._

import scala.scalajs.js
import RemedyFormat._
import org.multics.baueran.frep.frontend.util.BetterCaseRubric

object Repertorise {

  var remedyFilter = ""
  var symptomQuery = ""
  var remedyFormat = RemedyFormat.NotFormatted
  var selectedRepertory = ""
  var results = mutable.ListBuffer[CaseRubric]()

  // ------------------------------------------------------------------------------------------------------------------
  // Render HTML for the results of a repertory lookup directly to page.
  //
  // containingRemedyAbbrev, if not empty, will lead to only those rows drawn, that contain remedy
  // with abbrev. containingRemedyAbbrev.
  private def showResults(): Unit = {

    // This method is just to display the remedy-summary at the top of the results table.
    // It is not strictly necessary for displaying the results themselves.
    def resultingRemedies() = {
      var remedies = mutable.HashMap[Remedy, (Integer, Integer)]()

      for (cr <- results) {
        for ((r,w) <- cr.weightedRemedies) {
          if (remedies.contains(r)) {
            val weight = remedies.get(r).get._1 + w
            val occurrence = remedies.get(r).get._2 + 1
            remedies.put(r, (weight, occurrence))
          }
          else
            remedies.put(r, (w, 1))
        }
      }

      remedies
    }

    val numberOfMultiOccurrences = resultingRemedies().filter(rr => rr._2._2 > 1).size

    def resultRow(result: CaseRubric) = {

      implicit def crToCR(cr: CaseRubric) = new BetterCaseRubric(cr)

      val remedies =
        if (remedyFormat == RemedyFormat.NotFormatted)
          result.getRawRemedies()
        else
          result.getFormattedRemedies()

      tr(
        td(result.rubric.fullPath),
        td(remedies.take(remedies.size - 1).map(l => span(l, ", ")) ::: List(remedies.last)),
        td(
          button(cls:="btn btn-sm", `type`:="button", id:=("button_" + result.repertoryAbbrev + "_" + result.rubric.id),
            style:="vertical-align: middle; display: inline-block",
            (if (Case.cRubrics.contains(result)) attr("disabled"):="disabled" else ""),
            onclick:={ (event: Event) => {
              event.stopPropagation()
              Case.updateAnalysisView()
              Case.addRepertoryLookup(result)
              $("#button_" + result.repertoryAbbrev + "_" + result.rubric.id).attr("disabled", 1)
              showCase()
            }
            }, "Add")
        )
      )
    }

    $("#resultStatus").empty()
    $("#resultStatus").append(
      div(cls:="alert alert-secondary", role:="alert",
        b(a(href:="#", onclick:={ (event: Event) => remedyFilter = ""; showResults() },
          results.size + " result(s) for '" + symptomQuery + "'. ")),
        if (numberOfMultiOccurrences > 1) {
          val relevantMultiRemedies = resultingRemedies().toList
            .sortBy(-_._2._2)
            .filter(rr => rr._2._2 > 1)

          span("(Multi-occurrences in search results: ",
            relevantMultiRemedies
              .take(relevantMultiRemedies.size - 1)
              .map(rr => {
                span(
                  (rr._2._2 + "x"),
                  a(href := "#",
                    onclick := { (event: Event) =>
                      remedyFilter = rr._1.nameAbbrev
                      showResults()
                    },
                    rr._1.nameAbbrev),
                  ("(" + rr._2._1 + "), "),
                )
              }),
            span(
              (relevantMultiRemedies.last._2._2 + "x"),
              a(href:="#", onclick := { (event: Event) =>
                remedyFilter = relevantMultiRemedies.last._1.nameAbbrev
                showResults()
              }, relevantMultiRemedies.last._1.nameAbbrev),
              ("(" + relevantMultiRemedies.last._2._1 + ")")),
            ")")
        }
        else {
          span(" ")
        }
      ).render

    )

    $("#resultDiv").empty()
    $("#resultDiv").append(
      div(cls:="table-responsive",
        table(cls:="table table-striped table-sm table-bordered",
          thead(cls:="thead-dark", scalatags.JsDom.attrs.id:="resultsTHead",
            th(attr("scope"):="col", "Symptom"),
            th(attr("scope"):="col",
              a(scalatags.JsDom.attrs.id:="remediesFormatButton",
                cls:="underline", href:="#", style:="color:white;",
                onclick:={ (event: Event) =>
                  if (remedyFormat == RemedyFormat.NotFormatted)
                    remedyFormat = RemedyFormat.Formatted
                  else
                    remedyFormat = RemedyFormat.NotFormatted

                  showResults()
                  showCase()
                },
                "Remedies")
            ),
            th(attr("scope"):="col", " ")
          ),
          tbody(scalatags.JsDom.attrs.id:="resultsTBody")
        )
      ).render
    )

    if (remedyFilter.length == 0)
      results.foreach(result => $("#resultsTBody").append(resultRow(result).render))
    else
      results.filter(_.containsRemedyAbbrev(remedyFilter)).foreach(result => $("#resultsTBody").append(resultRow(result).render))
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def showCase() = {
    $("#caseDiv").empty()
    $("#caseDiv").append(Case.toHTML(remedyFormat).render)
    Case.updateAnalysisView()
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSubmitSymptom(event: Event): Unit = {
    val symptom = dom.document.getElementById("inputField").asInstanceOf[HTMLInputElement].value
    val repertory = selectedRepertory

    if (repertory.length == 0 || symptom.trim.replaceAll("[^A-Za-z0-9]", "").length <= 3) {
      println("No repertory selected, or symptom entered!")
      $("#resultStatus").empty()
      $("#resultStatus").append(
        div(cls:="alert alert-danger", role:="alert",
          b("No results returned for '" + symptom + "'. " +
            "Either no repertory selected or symptom input too short.")).render)
      return
    }

    HttpRequest("http://localhost:9000/lookup")
      .withQueryParameters(("symptom", symptom), ("repertory", repertory))
      .withCredentials(true)
      .send()
      .onComplete({
      case response: Success[SimpleHttpResponse] => {
        parse(response.get.body) match {
          case Right(json) => {
            val cursor = json.hcursor
            cursor.as[List[CaseRubric]] match {
              case Right(newResults) => {
                results.clear()
                results ++= newResults
                symptomQuery = symptom
                showResults()
                if (Case.size() > 0)
                  showCase()
              }
              case Left(t) => println("Parsing of lookup as RepertoryLookup failed: " + t)
            }
          }
          case Left(_) => println("Parsing of lookup failed (is it JSON?).")
        }
      }
      case error: Failure[SimpleHttpResponse] => {
        println("Lookup failed: " + error.toString())
        $("#resultStatus").empty()
        $("#resultStatus").append(
          div(cls:="alert alert-danger", role:="alert",
            b("No results returned for '" + symptom + "'. " +
              "Either symptom not in repertory or server error.")).render)
      }
    })
  }

  // ------------------------------------------------------------------------------------------------------------------
  def apply() = {
    val ulRepertorySelection = Rx(
      div(cls:="dropdown col-sm-2",
        button(`type`:="button",
          style:="overflow: hidden;",
          cls:="btn btn-block dropdown-toggle",
          data("toggle"):="dropdown",
          `id`:="repSelectionDropDownButton",
          "Repertories"),
        div(cls:="dropdown-menu", `id`:="repSelectionDropDown")
      )
    )

    def updateAvailableRepertories() = {
      HttpRequest("http://localhost:9000/availableReps")
        .send().onComplete({
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[Info]] match {
                case Right(infos) => {
                  infos.foreach(info => {
                    $("#repSelectionDropDown")
                      .append(a(cls := "dropdown-item", href := "#", data.value := info.abbrev,
                        onclick := { (event: Event) =>
                          selectedRepertory = info.abbrev
                          $("#repSelectionDropDownButton").text("Repertory: " + selectedRepertory)
                        }, info.abbrev).render)

                    if (info.access == RepAccess.Default) {
                      selectedRepertory = info.abbrev
                      $("#repSelectionDropDownButton").text("Repertory: " + selectedRepertory)
                    }
                  })
                }
                case Left(t) => println("Parsing of available repertories failed: " + t)
              }
            }
            case Left(_) => println("Parsing of available repertories failed (is it JSON?).")
          }
        }
        case error: Failure[SimpleHttpResponse] => println("Available repertories failed: " + error.toString)
      })
    }

    val myHTML =
      div(cls:="container-fluid",
        div(cls:="container-fluid text-center",
          div(cls:="col-sm-12 text-center",
            h2("Repertorisation")
          ),
          div(cls:="row", style:="margin-top:20px;",
            div(cls:="col-sm-1"),
            div(cls:="row col-sm-10",
              ulRepertorySelection.now,
              div(cls:="col-sm-9",
                input(cls:="form-control", `id`:="inputField",
                  onkeydown := { (event: dom.KeyboardEvent) =>
                    if (event.keyCode == 13) {
                      remedyFilter = ""
                      event.stopImmediatePropagation()
                      onSubmitSymptom(event)
                    }
                  },
                  `placeholder`:="Enter a symptom (for example: head, pain, left)")
              )
            ),
            div(cls:="col-sm-1")
          ),
          div(cls:="col-sm-12 text-center", style:="margin-top:20px;",
            button(cls:="btn btn-dark btn-primary", style:="width: 120px; margin-right:5px;", `type`:="button",
              onclick:={ (event: Event) => remedyFilter = ""; onSubmitSymptom(event); event.stopPropagation() }, "Find"),
            button(cls:="btn", style:="width: 100px;", `type`:="button",
              onclick:={ (event: Event) => $("#inputField").value(""); event.stopPropagation() }, "Clear")
          ),
        ),
        div(cls:="container-fluid", style:="margin-top: 23px;", id:="resultStatus"),
        div(cls:="container-fluid", id:="resultDiv"),
        div(cls:="span12", id:="caseDiv", {
          if (Case.size() > 0)
            Case.toHTML(remedyFormat)
          else ""
        })
      )

    updateAvailableRepertories()
    myHTML
  }
}
