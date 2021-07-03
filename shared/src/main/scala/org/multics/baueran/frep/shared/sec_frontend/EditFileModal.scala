package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.{HttpRequest, Method}
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.{Caze}
import org.multics.baueran.frep.shared.frontend.{Case, Repertorise, getCookieData, serverUrl, apiPrefix}
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all.{onclick, _}
import rx.Var
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._
import org.querki.jquery.$

import scala.scalajs.js
import scala.math.{max, min}
import scala.util.{Failure, Success, Try}

object EditFileModal {

  val fileName_fileId = Var(("", ""))
  private var currentFileDescription: Option[String] = None
  private val currentlyAssociatedCaseHeaders: Var[List[(Int, String)]] = Var(Nil)
  private val currentlySelectedCaseId = Var(-1)
  private val currentlySelectedCaseHeader = Var("")
  private val casesHeight = Rx(max(200, min(100, currentlyAssociatedCaseHeaders().length * 30)))
  private val caseAnchors = Rx {
    currentlyAssociatedCaseHeaders() match {
      case Nil =>
        List(a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="-1", href:="#list-profile", role:="tab", "<no cases created yet>").render)
      case _ => currentlyAssociatedCaseHeaders()
        .sortBy(_._2) // date, effectively
        .reverse
        .map { case (caseId, caseEnrichedDescr) =>
          a(cls:="list-group-item list-group-item-action", id:=caseId.toString(), data.toggle:="list", href:="#list-profile", role:="tab",
            onclick:={ (event: Event) =>
              $("#openFileEditFileModal").removeAttr("disabled")
              $("#deleteFileEditFileModal").removeAttr("disabled")
              currentlySelectedCaseId() = caseId
            },
            caseEnrichedDescr)
            .render
        }
    }
  }

  fileName_fileId.foreach { case (fileName,fileId) if (fileId.forall(_.isDigit)) =>
    // Reset UI and some data
    currentlySelectedCaseId() = -1
    currentlySelectedCaseHeader() = ""
    currentFileDescription = None
    $("#openFileEditFileModal").attr("disabled", true)
    $("#deleteFileEditFileModal").attr("disabled", true)

    // Callback function for http request below...
    def updateModal(response: Try[SimpleHttpResponse]) = {
      $("body").css("cursor", "default")

      response match {
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              json.hcursor.as[Seq[(String, Option[Int], Option[String])]] match {
                case Right(results) =>
                  currentlyAssociatedCaseHeaders() =
                    results.toList.map(result =>
                      result match {
                        case (_, Some(caseId), Some(caseEnrichedHdr)) => Some(caseId, caseEnrichedHdr)
                        case _ => None
                      }
                    ).flatten

                  if (results.length > 0)
                    currentFileDescription = Some(results.head._1)
                  $("#fileDescrEditFileModal").`val`(currentFileDescription.getOrElse(""))

                case Left(err) =>
                  println("Decoding of cases' ids and headers failed: " + err + "; " + response.get.body)
              }
            }
            case Left(msg) =>
              println(s"INFO: No file information received as there probably aren't any (or an error occurred): $msg")
              currentlyAssociatedCaseHeaders() = List()
              $("#fileDescrEditFileModal").`val`(currentFileDescription.getOrElse(""))
          }
        }
        case error: Failure[SimpleHttpResponse] => println("ERROR: " + error.get.body)
      }
    }

    // The call-back is defined right above this if-condition!
    if (fileName.length() > 0 && fileId.forall(_.isDigit)) {
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/file_overview")
        .withQueryParameters("fileId" -> fileId)
        .send()
        .onComplete((r: Try[SimpleHttpResponse]) => {
          updateModal(r)
        })
    }
  }

  private def areYouSureModalCase() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="editFileModalAreYouSureCase",
      div(cls:="modal-dialog", role:="document",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", Rx("Really delete case " + currentlySelectedCaseHeader() + "?")),
            button(`type`:="button", cls:="close", data.dismiss:="modal", aria.label:="Close", span(aria.hidden:="true", "\u00d7"))
          ),
          div(cls:="modal-body",
            p("Deleting this case cannot be undone!")
          ),
          div(cls:="modal-footer",
            button(`type`:="button", cls:="btn btn-secondary", data.dismiss:="modal", "Cancel"),
            button(`type`:="button", cls:="btn btn-primary", data.dismiss:="modal",
              onclick:= { (event: Event) =>
                HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/del_case")
                  .withMethod(Method.DELETE)
                  .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
                  .withBody(MultiPartBody(
                    "caseId" -> PlainTextBody(currentlySelectedCaseId.now.toString()),
                    "memberId" -> PlainTextBody(getCookieData(dom.document.cookie, CookieFields.id.toString).getOrElse(""))))
                  .send()

                // If the deleted case is currently opened, update current view by basically removing that case.
                if (Case.descr.isDefined && (Case.descr.get.id == currentlySelectedCaseId.now)) {
                  Case.descr = None
                  Case.updateCaseViewAndDataStructures()
                  Case.rmCaseDiv()
                }
                else
                  println("EditFileModal: the case which was meant to be deleted from DB, was not currently opened. Nothing to be redrawn on screen.")
              },
              "Delete")
          )
        )
      )
    )
  }

  private def mainModal() = {

    def getCaseFromCurrentSelection() = {
      val anchorNode = dom.document.querySelector("#editFileAvailableCasesList .active")
      currentlySelectedCaseId() = anchorNode.id.toInt
      currentlySelectedCaseHeader() = anchorNode.textContent
    }

    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="editFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%; overflow-y: initial;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", Rx(fileName_fileId()._1)),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body", style:="height: auto; overflow-y: auto;",
          // TODO: If one wants to scroll modal body, we need a height. Right now, however, we don't want that:
          // div(cls:="modal-body", style:="height: 80vh; overflow-y: auto;",

            div(cls:="form-group mb-2",
              div(cls:="mb-3",
                label(`for`:="fileDescr", "Description"),
                textarea(cls:="form-control", id:="fileDescrEditFileModal", rows:="8", placeholder:="A more verbose description of the file", style:="height: 20vh;",
                  onkeyup:= { (event: Event) =>
                    currentFileDescription match {
                      case Some(fileDescription) =>
                        if ($("#fileDescrEditFileModal").`val`().toString() != fileDescription)
                          $("#saveFileDescrEditFileModalButton").removeAttr("disabled")
                        else
                          $("#saveFileDescrEditFileModalButton").attr("disabled", true)
                      case None =>
                        if ($("#fileDescrEditFileModal").`val`().toString().length == 0)
                          $("#saveFileDescrEditFileModalButton").attr("disabled", true)
                        else
                          $("#saveFileDescrEditFileModalButton").removeAttr("disabled")
                    }
                  })
              ),
              div(cls:="form-row d-flex flex-row-reverse",
                button(cls:="btn btn-primary mb-2 mr-2 ml-2", id:="saveFileDescrEditFileModalButton", data.toggle:="modal", data.dismiss:="modal", disabled:=true,
                  onclick:= { (event: Event) =>
                    fileName_fileId.now match {
                      case (fileName, fileId) if fileId.forall(_.isDigit) =>
                        HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/update_file_description")
                          .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
                          .put(MultiPartBody(
                            "filedescr" -> PlainTextBody($("#fileDescrEditFileModal").`val`().toString().trim()),
                            "fileId"    -> PlainTextBody(fileId)))
                      case _ => ;
                    }
                    $("#saveFileDescrEditFileModalButton").attr("disabled", true)
                    js.eval("$('#editFileModal').modal('hide');") // TODO: This is ugly! No idea for an alternative :-(
                  },
                  "Save"),
                button(cls:="btn mb-2 btn-secondary", data.dismiss:="modal", "Cancel")
              )
            ),

            div(cls:="border-top my-3"),

            div(cls:="form-group",
              div(cls:="form-row",
                label(`for`:="editFileAvailableFilesList", "Cases"),
                div(cls:="col-12 list-group", role:="tablist", id:="editFileAvailableCasesList", style:="height: 30vh; overflow-y: scroll;",
                  // TODO: It seems, we no longer need this RX here...
                  // cls:="col-12 list-group", role:="tablist", id:="editFileAvailableCasesList", style:=Rx("height: " + casesHeight.toString() + "px; overflow-y: scroll;"),
                  caseAnchors
                )
              ),
              div(cls:="form-row d-flex flex-row-reverse",
                button(cls:="btn btn-primary mb-2 mt-2 ml-2", id:="openFileEditFileModal", data.toggle:="modal", data.dismiss:="modal", disabled:=true,
                  onclick:={ (event: Event) =>
                    getCaseFromCurrentSelection()

                    HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/case")
                      .withQueryParameter("caseId", currentlySelectedCaseId.now.toString())
                      .send()
                      .onComplete({
                        case response: Success[SimpleHttpResponse] => {
                          parse(response.get.body) match {
                            case Right(json) => {
                              val cursor = json.hcursor
                              cursor.as[Caze] match {
                                case Right(caze) => {
                                  Case.descr = Some(caze)
                                  Case.cRubrics = caze.results
                                  Repertorise.showResults()
                                  Case.updateCaseHeaderView() // So that the buttons Add, Edit, etc. are redrawn properly
                                }
                                case Left(err) => println("Decoding of case failed: " + err)
                              }
                            }
                            case Left(err) => println("Parsing of case (is it JSON?): " + err)
                          }
                        }
                        case error: Failure[SimpleHttpResponse] => println("Lookup of case failed: " + error.toString())
                      })
                  },
                  "Open"),
                button(cls:="btn mb-2 mt-2 btn-secondary", id:="deleteFileEditFileModal", data.toggle:="modal", data.dismiss:="modal", data.target:="#editFileModalAreYouSureCase", disabled:=true,
                  onclick:= { (event: Event) => getCaseFromCurrentSelection() },
                  "Delete")

              )
            )

          )
        )
      )
    )
  }

  def apply() = div(areYouSureModalCase(), mainModal())

}
