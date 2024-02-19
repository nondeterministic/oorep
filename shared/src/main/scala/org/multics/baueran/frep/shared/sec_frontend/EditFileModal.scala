package org.multics.baueran.frep.shared.sec_frontend

import io.circe.parser.parse
import org.multics.baueran.frep.shared.Defs.{CookieFields, HeaderFields}
import org.multics.baueran.frep.shared.TopLevelUtilCode.getDocumentCsrfCookie
import org.multics.baueran.frep.shared.{Caze, HttpRequest2}
import org.multics.baueran.frep.shared.frontend.{Case, OorepHtmlButton, OorepHtmlElement, getCookieData}
import org.multics.baueran.frep.shared.frontend.views.repertory.RepertoryView
import org.scalajs.dom
import org.scalajs.dom.{Event, document, html}
import scalatags.JsDom.all.{onclick, _}

object EditFileModal extends OorepHtmlElement {
  def getId() = "editFileModal"

  private object AreYouSureModalCase extends OorepHtmlElement {
    def getId() = "editFileModalAreYouSureCase"

    object Header extends OorepHtmlElement {
      def getId() = AreYouSureModalCase.getId() + "_dsfjh342KJghkgds"
      def set(newHeader: String) = getNode().get.asInstanceOf[html.Heading].innerText = s"Really delete case '${newHeader}'?"

      // Before this element is drawn, set() will actually set the h5() tag to a meaningful value. Otherwise it'd be empty...
      def apply() = h5(cls := "modal-title", id := getId())
    }

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", id := getId(),
        div(cls := "modal-dialog", role := "document",
          div(cls := "modal-content",
            div(cls := "modal-header",
              Header(),
              button(`type` := "button", cls := "close", data.dismiss := "modal", aria.label := "Close", span(aria.hidden := "true", "\u00d7"))
            ),
            div(cls := "modal-body",
              p("Deleting this case cannot be undone!")
            ),
            div(cls := "modal-footer",
              button(`type` := "button", cls := "btn btn-secondary", data.dismiss := "modal", "Cancel"),
              button(`type` := "button", cls := "btn btn-primary", data.dismiss := "modal",
                onclick := { (event: Event) =>
                  HttpRequest2("sec/del_case")
                    .withMethod("DELETE")
                    .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
                    .withBody(
                      ("caseId" -> currentlySelectedCaseId.toString()),
                      ("memberId" -> getCookieData(dom.document.cookie, CookieFields.id.toString).getOrElse("")))
                    .onSuccess((_: String) => {
                      getCookieData(dom.document.cookie, CookieFields.id.toString) match {
                        case Some(memberId) => FileModalCallbacks.updateMemberFiles(memberId.toInt)
                        case None => println("EditFileModal: Deleting of case failed.")
                      }
                    })
                    .send()

                  // If the deleted case is currently opened, update current view by basically removing that case.
                  if (Case.descr.isDefined && (Case.descr.get.id == currentlySelectedCaseId))
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

    object AvailableCasesList extends OorepHtmlElement {
      def getId() = "editFileAvailableCasesList"

      def setCurrCaseFromSelection() = {
        val anchorNode = dom.document.querySelector(s"#${getId()} .active")
        currentlySelectedCaseId = anchorNode.id.toInt
        EditFileModal.AreYouSureModalCase.Header.set(anchorNode.textContent)
      }

      def update(): Unit = {
        dom.document.getElementById(getId()) match {
          case null => ;
          case elem =>
            while (elem.hasChildNodes())
              elem.removeChild(elem.firstChild)

            caseAnchors =
              currentlyAssociatedCaseHeaders match {
                case Nil =>
                  List(a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="-1", href:="#list-profile", role:="tab", "<no cases created yet>").render)
                case _ => currentlyAssociatedCaseHeaders
                  .sortBy(_._2) // date, effectively
                  .reverse
                  .map { case (caseId, caseEnrichedDescr) =>
                    a(cls:="list-group-item list-group-item-action", id:=caseId.toString(), data.toggle:="list", href:="#list-profile", role:="tab",
                      onclick:={ (event: Event) =>
                        MainModal.HtmlButtonCaseOpen.enable()
                        MainModal.HtmlButtonCaseDelete.enable()
                        currentlySelectedCaseId = caseId
                      },
                      caseEnrichedDescr)
                      .render
                  }
              }

            for (anchor <- caseAnchors)
              elem.append(anchor)
        }
      }

      def apply() = {
        div(cls := "col-12 list-group", role := "tablist", id := getId(), style := "height: 30vh; overflow-y: scroll;",
          caseAnchors
        )
      }
    }

    object HtmlButtonCaseDelete extends OorepHtmlButton {
      def getId() = "deleteFileEditFileModal"

      def apply() = {
        button(cls := "btn mb-2 mt-2 btn-secondary", id := getId(), data.toggle := "modal", data.dismiss := "modal", data.target := s"#${AreYouSureModalCase.getId()}", disabled := true,
          onclick := { (event: Event) =>
            AvailableCasesList.setCurrCaseFromSelection()
          },
          "Delete")
      }
    }

    object HtmlButtonCaseOpen extends OorepHtmlButton {
      def getId() = "openFileEditFileModal"

      def apply() = {
        button(cls := "btn btn-primary mb-2 mt-2 ml-2", id := getId(), data.toggle := "modal", data.dismiss := "modal", disabled := true,
          onclick := { (event: Event) =>
            AvailableCasesList.setCurrCaseFromSelection()

            HttpRequest2("sec/case")
              .withQueryParameters(("caseId", currentlySelectedCaseId.toString()))
              .onSuccess((response: String) => {
                parse(response) match {
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
              })
              .send()
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
            fileName_fileId match {
              case (fileName, fileId) if fileId.forall(_.isDigit) =>
                HttpRequest2("sec/update_file_description")
                  .withHeaders((HeaderFields.csrfToken.toString(), getDocumentCsrfCookie().getOrElse("")))
                  .put(
                    ("filedescr" -> FileDescriptionTextArea.getTextValue().trim()),
                    ("fileId" -> fileId))
              case _ => ;
            }
            disable()
            CloseButton.click()
          },
          "Save")
      }
    }

    object Header extends OorepHtmlElement {
      def getId() = EditFileModal.MainModal.getId() + "_sckj34kljhdsKJGjk34hg&h4rdfdfNBGHJG"
      def set(newHeader: String) = getNode().get.asInstanceOf[html.Heading].innerText = newHeader
      def apply() = h5(cls := "modal-title", id := getId())
    }

    def apply() = {
      div(cls := "modal fade", tabindex := "-1", role := "dialog", id := getId(),
        onshow := { (event: Event) => unselectCases() },
        div(cls := "modal-dialog modal-dialog-centered", role := "document", style := "min-width: 80%; overflow-y: initial;",
          div(cls := "modal-content",
            div(cls := "modal-header",
              Header(),
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

  private var fileName_fileId = ("", "")
  private var currentFileDescription: Option[String] = None
  private var currentlyAssociatedCaseHeaders: List[(Int, String)] = List.empty
  private var currentlySelectedCaseId = -1
  private var caseAnchors: List[html.Anchor] = List.empty

  private def unselectCases(): Unit = {
    currentlySelectedCaseId = -1
    for (anchr <- caseAnchors)
      anchr.classList.remove("active")
  }

  def update(fileName: String, fileId: String): Unit = {
    if (!fileId.forall(_.isDigit))
      return

    fileName_fileId = (fileName, fileId)

    // Reset UI and some data
    unselectCases()
    currentFileDescription = None
    MainModal.HtmlButtonCaseOpen.disable()
    MainModal.HtmlButtonCaseDelete.disable()

    // Callback function for http request below...
    def updateModal(response: String) = {
      document.body.style.cursor = "default"
      MainModal.Header.set(fileName)

      parse(response) match {
        case Right(json) => {
          json.hcursor.as[Seq[(String, Option[Int], Option[String])]] match {
            case Right(results) =>
              currentlyAssociatedCaseHeaders =
                results.toList.map(result =>
                  result match {
                    case (_, Some(caseId), Some(caseEnrichedHdr)) => Some(caseId, caseEnrichedHdr)
                    case _ => None
                  }
                ).flatten

              if (results.length > 0)
                currentFileDescription = Some(results.head._1)
              MainModal.FileDescriptionTextArea.setTextValue(currentFileDescription.getOrElse(""))
              MainModal.AvailableCasesList.update()
            case Left(err) =>
              println("Decoding of cases' ids and headers failed: " + err + "; " + response)
          }
        }
        case Left(msg) =>
          println(s"INFO: No file information received as there probably aren't any (or an error occurred): $msg")
          currentlyAssociatedCaseHeaders = List()
          MainModal.FileDescriptionTextArea.setTextValue(currentFileDescription.getOrElse(""))
      }
    }

    // The call-back is defined right above this if-condition!
    if (fileName.length() > 0 && fileId.forall(_.isDigit)) {
      HttpRequest2("sec/file_overview")
        .withQueryParameters("fileId" -> fileId)
        .onSuccess((response: String) => updateModal(response))
        .send()
    }
  }

  def apply() = {
    div(
      AreYouSureModalCase(), MainModal()
    )
  }

}
