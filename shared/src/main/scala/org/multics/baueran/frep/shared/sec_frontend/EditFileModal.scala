package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.{HttpRequest, Method}
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.Caze
import org.multics.baueran.frep.shared.frontend.{Case, OorepHtmlButton, OorepHtmlElement, RepertoryView, apiPrefix, getCookieData, serverUrl}
import org.scalajs.dom
import org.scalajs.dom.{Event, document}
import scalatags.JsDom.all.{onclick, _}
import rx.Var
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._
import org.scalajs.dom.html

import scala.math.{max, min}
import scala.util.{Failure, Success, Try}
import scala.language.implicitConversions

object EditFileModal extends OorepHtmlElement {
  def getId() = "editFileModal"

  private object AreYouSureModalCase extends OorepHtmlElement {
    def getId() = "editFileModalAreYouSureCase"

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", id := getId(),
        div(cls := "modal-dialog", role := "document",
          div(cls := "modal-content",
            div(cls := "modal-header",
              h5(cls := "modal-title", Rx("Really delete case " + currentlySelectedCaseHeader() + "?")),
              button(`type` := "button", cls := "close", data.dismiss := "modal", aria.label := "Close", span(aria.hidden := "true", "\u00d7"))
            ),
            div(cls := "modal-body",
              p("Deleting this case cannot be undone!")
            ),
            div(cls := "modal-footer",
              button(`type` := "button", cls := "btn btn-secondary", data.dismiss := "modal", "Cancel"),
              button(`type` := "button", cls := "btn btn-primary", data.dismiss := "modal",
                onclick := { (event: Event) =>
                  HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/del_case")
                    .withMethod(Method.DELETE)
                    .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
                    .withBody(MultiPartBody(
                      "caseId" -> PlainTextBody(currentlySelectedCaseId.now.toString()),
                      "memberId" -> PlainTextBody(getCookieData(dom.document.cookie, CookieFields.id.toString).getOrElse(""))))
                    .send()
                    .onComplete({
                      case _: Success[SimpleHttpResponse] =>
                        getCookieData(dom.document.cookie, CookieFields.id.toString) match {
                          case Some(memberId) => FileModalCallbacks.updateMemberFiles(memberId.toInt)
                          case None => println("EditFileModal: Deleting of case failed.")
                        }
                      case _ => ;
                    })

                  // If the deleted case is currently opened, update current view by basically removing that case.
                  if (Case.descr.isDefined && (Case.descr.get.id == currentlySelectedCaseId.now))
                    Case.removeFromMemory()
                  else
                    println("EditFileModal: the case which was meant to be deleted from DB, was not currently opened. Nothing to be redrawn on screen.")
                },
                "Delete")
            )
          )
        )
      )
    }
  }

  private object MainModal extends OorepHtmlElement {
    def getId() = EditFileModal.getId()

    object CloseButton extends OorepHtmlButton {
      def getId() = "EditFileModal_MainModal_CloseButton_sdfdgferw234"

      def apply() = {
        button(id:=getId(), `type` := "button", cls := "close", data.dismiss := "modal", "\u00d7")
      }
    }

    private object AvailableCasesList extends OorepHtmlElement {
      def getId() = "editFileAvailableCasesList"

      def setCurrCaseFromSelection() = {
        val anchorNode = dom.document.querySelector(s"#${getId()} .active")
        currentlySelectedCaseId() = anchorNode.id.toInt
        currentlySelectedCaseHeader() = anchorNode.textContent
      }

      def apply() = {
        div(cls := "col-12 list-group", role := "tablist", id := getId(), style := "height: 30vh; overflow-y: scroll;",
          // TODO: It seems, we no longer need this RX here...
          // cls:="col-12 list-group", role:="tablist", id:="editFileAvailableCasesList", style:=Rx("height: " + casesHeight.toString() + "px; overflow-y: scroll;"),
          caseAnchors
        )
      }
    }

    object HtmlButtonCaseDelete extends OorepHtmlButton {
      def getId() = "deleteFileEditFileModal"

      def apply() = {
        button(cls := "btn mb-2 mt-2 btn-secondary", id := getId(), data.toggle := "modal", data.dismiss := "modal", data.target := s"#${AreYouSureModalCase.getId()}", disabled := true,
          onclick := { (event: Event) => AvailableCasesList.setCurrCaseFromSelection() },
          "Delete")
      }
    }

    object HtmlButtonCaseOpen extends OorepHtmlButton {
      def getId() = "openFileEditFileModal"

      def apply() = {
        button(cls := "btn btn-primary mb-2 mt-2 ml-2", id := getId(), data.toggle := "modal", data.dismiss := "modal", disabled := true,
          onclick := { (event: Event) =>
            AvailableCasesList.setCurrCaseFromSelection()

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
                          RepertoryView.showResults()
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
          "Open")
      }
    }

    object FileDescriptionTextArea extends OorepHtmlElement {
      def getId() = "fileDescrEditFileModal"

      def setTextValue(newValue: String): Unit = {
        getNode() match {
          case Some(node) => node.asInstanceOf[html.TextArea].value = newValue
          case None => ;
        }
      }

      def getTextValue(): String = {
        getNode() match {
          case Some(node) => node.asInstanceOf[html.TextArea].value
          case None => ""
        }
      }

      def apply() = {
        textarea(cls := "form-control", id := getId(), rows := "8", placeholder := "A more verbose description of the file", style := "height: 20vh;",
          onkeyup := { (event: Event) =>
            currentFileDescription match {
              case Some(descr) =>
                if (getTextValue() != descr)
                  MainModal.SaveFileDescriptionHtmlButton.enable()
                else
                  MainModal.SaveFileDescriptionHtmlButton.disable()
              case None =>
                if (getTextValue().length == 0)
                  MainModal.SaveFileDescriptionHtmlButton.disable()
                else
                  MainModal.SaveFileDescriptionHtmlButton.enable()
            }
          })
      }
    }

    object SaveFileDescriptionHtmlButton extends OorepHtmlButton {
      def getId() = "saveFileDescrEditFileModalButton"

      def apply() = {
        button(cls := "btn btn-primary mb-2 mr-2 ml-2", id := getId(), data.toggle := "modal", data.dismiss := "modal", disabled := true,
          onclick := { (event: Event) =>
            fileName_fileId.now match {
              case (fileName, fileId) if fileId.forall(_.isDigit) =>
                HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/update_file_description")
                  .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
                  .put(MultiPartBody(
                    "filedescr" -> PlainTextBody(FileDescriptionTextArea.getTextValue().trim()),
                    "fileId" -> PlainTextBody(fileId)))
              case _ => ;
            }
            disable()
            CloseButton.click()
          },
          "Save")
      }
    }

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", id := getId(),
        onshow := { (event: Event) => unselectCases() },
        div(cls := "modal-dialog modal-dialog-centered", role := "document", style := "min-width: 80%; overflow-y: initial;",
          div(cls := "modal-content",
            div(cls := "modal-header",
              h5(cls := "modal-title", Rx(fileName_fileId()._1)),
              CloseButton()
            ),
            div(cls := "modal-body", style := "height: auto; overflow-y: auto;",
              // TODO: If one wants to scroll modal body, we need a height. Right now, however, we don't want that:
              // div(cls:="modal-body", style:="height: 80vh; overflow-y: auto;",

              div(cls := "form-group mb-2",
                div(cls := "mb-3",
                  label(`for` := FileDescriptionTextArea.getId(), "Description"),
                  FileDescriptionTextArea()
                ),
                div(cls := "form-row d-flex flex-row-reverse",
                  SaveFileDescriptionHtmlButton(),
                  button(cls := "btn mb-2 btn-secondary", data.dismiss := "modal", "Cancel")
                )
              ),

              div(cls := "border-top my-3"),

              div(cls := "form-group",
                div(cls := "form-row",
                  label(`for` := AvailableCasesList.getId(), "Cases"),
                  AvailableCasesList()
                ),
                div(cls := "form-row d-flex flex-row-reverse",
                  HtmlButtonCaseOpen(),
                  HtmlButtonCaseDelete()
                )
              )

            )
          )
        )
      )
    }
  }

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
              MainModal.HtmlButtonCaseOpen.enable()
              MainModal.HtmlButtonCaseDelete.enable()
              currentlySelectedCaseId() = caseId
            },
            caseEnrichedDescr)
            .render
        }
    }
  }

  private def unselectCases(): Unit = {
    currentlySelectedCaseId() = -1
    currentlySelectedCaseHeader() = ""
    for (anchr <- caseAnchors.now)
      anchr.classList.remove("active")
  }

  fileName_fileId.foreach { case (fileName,fileId) if (fileId.forall(_.isDigit)) =>
    // Reset UI and some data
    unselectCases()
    currentFileDescription = None
    MainModal.HtmlButtonCaseOpen.disable()
    MainModal.HtmlButtonCaseDelete.disable()

    // Callback function for http request below...
    def updateModal(response: Try[SimpleHttpResponse]) = {
      document.body.style.cursor = "default"

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
                  MainModal.FileDescriptionTextArea.setTextValue(currentFileDescription.getOrElse(""))

                case Left(err) =>
                  println("Decoding of cases' ids and headers failed: " + err + "; " + response.get.body)
              }
            }
            case Left(msg) =>
              println(s"INFO: No file information received as there probably aren't any (or an error occurred): $msg")
              currentlyAssociatedCaseHeaders() = List()
              MainModal.FileDescriptionTextArea.setTextValue(currentFileDescription.getOrElse(""))
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

  def apply() = {
    div(
      AreYouSureModalCase(), MainModal()
    )
  }

}
