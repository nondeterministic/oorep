package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import io.circe.syntax._
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.multics.baueran.frep.shared.{Caze, FIle}
import org.multics.baueran.frep.shared.frontend.{Case, getCookieData}
import org.multics.baueran.frep.shared.sec_frontend.FileModalCallbacks.updateMemberFiles
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all._
import rx.Var
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._
import org.querki.jquery.$
import org.scalajs.dom.html.Anchor
import scalatags.Text.TypedTag
import scala.math.{min, max}

import scala.util.{Failure, Success, Try}

object EditFileModal {

  val fileName = Var("")
  private val cases: Var[List[Caze]] = Var(List())
  private val casesHeight = Rx(max(200, min(100, cases().length * 30)))

  private def computeCaseAnchors() = Rx {
    if (cases().length > 1)
      cases().map(c =>
        a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", c.header).render)
    else
      List(a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", "<no files created yet>").render)
  }

  private def mainModal() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="editFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", Rx(fileName())),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",

            div(cls:="form-group mb-2",
              div(cls:="mb-3",
                label(`for`:="fileDescr", "Description"),
                textarea(cls:="form-control", id:="fileDescr", rows:="8", placeholder:="A more verbose description of the file")
              ),
              div(cls:="form-row",
                div(cls:="col"),
                div(cls:="col-2",
                  button(cls:="btn mb-2 mr-2", id:="saveFileDescrEditFileModal", data.toggle:="modal", data.dismiss:="modal", disabled:=true, "Save"),
                  button(cls:="btn mb-2", data.dismiss:="modal", "Cancel")
                ),
                div(cls:="col")
              )
            ),

            div(cls:="border-top my-3"),

            div(cls:="form-group",
              div(
                label(`for`:="editFileAvailableFilesList", "Cases"),
                div(cls:="list-group", role:="tablist", id:="editFileAvailableCasesList", style:=Rx("height: " + casesHeight().toString() + "px; overflow-y: scroll;"),
                  computeCaseAnchors())
              ),
              div(cls:="form-row",
                div(cls:="col"),
                div(cls:="col-2",
                  button(cls:="btn mb-2 mr-2", id:="openFileEditFileModal", data.toggle:="modal", data.dismiss:="modal", disabled:=true, "Open"),
                  button(cls:="btn mb-2", id:="deleteFileEditFileModal", data.dismiss:="modal", disabled:=true, "Delete")
                ),
                div(cls:="col")
              )
            )

          )
        )
      )
    )
  }

  def getCasesForFile(fileHeader: String) = {
    def updateCases(response: Try[SimpleHttpResponse]) = {
      response match {
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[Caze]] match {
                case Right(cs) => cases() = cs; println("Gotten " + cs.toString())
                case Left(t) => println("Decoding of available cases failed: " + t)
              }
            }
            case Left(_) => println("Parsing of available cases failed (is it JSON?).")
          }
        }
        case error: Failure[SimpleHttpResponse] => println("ERROR: " + error.get.body)
      }
    }

    getCookieData(dom.document.cookie, "oorep_member_id") match {
      case Some(memberId) => {
        HttpRequest(serverUrl() + "/availableCasesForFile")
          .withQueryParameters("memberId" -> memberId, "fileId" -> fileHeader)
          .withCrossDomainCookies(true)
          .send()
          .onComplete((r: Try[SimpleHttpResponse]) => updateCases(r))
      }
      case None => println("WARNING: getCasesForFile() failed. Could not get memberID from cookie."); -1
    }
  }

  def apply() = div(mainModal())

}
