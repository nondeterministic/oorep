package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.frontend.{ getCookieData, Case, serverUrl }
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all._
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._
import org.querki.jquery.$

object OpenFileModal extends FileModal {

  private def requestFileDeletion() = {
    getCookieData(dom.document.cookie, CookieFields.id.toString) match {
      case Some(memberId) => {
        HttpRequest(serverUrl() + "/del_file_and_cases")
          .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
          .post(MultiPartBody(
            "fileId" -> PlainTextBody(selected_file_id.now.getOrElse(-1).toString)))
          .onComplete({ case _ =>
            FileModalCallbacks.updateMemberFiles(memberId.toInt)
          })

        // If the file had currently a cose opened in the case view,
        // remove it from screen to avoid weird database behaviour,
        // in case the user then modifies the case...
        if (Case.getCurrOpenFileId() == selected_file_id.now) {
          Case.updateCurrOpenFile(None)
          Case.rmCaseDiv()
        }
      }
      case None => ; // TODO: Display error modal?!
    }
  }

  private def areYouSureModal() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="openFileModalAreYouSure",
      div(cls:="modal-dialog", role:="document",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", Rx("Really delete file " + selected_file_header().getOrElse("") + "?")),
            button(`type`:="button", cls:="close", data.dismiss:="modal", aria.label:="Close", span(aria.hidden:="true", "\u00d7"))
          ),
          div(cls:="modal-body",
            p("Deleting this file will also delete all of its cases!")
          ),
          div(cls:="modal-footer",
            button(`type`:="button", cls:="btn btn-secondary", data.dismiss:="modal", "Cancel"),
            button(`type`:="button", cls:="btn btn-primary", data.dismiss:="modal", onclick:= { (event: Event) => requestFileDeletion() }, "Delete")
          )
        )
      )
    )
  }

  private def mainModal() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="openFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Select file"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="form-group",
              div(cls:="list-group", role:="tablist", id:="openFileAvailableFilesList", style:="height: 250px; overflow-y: scroll;", Rx(files()))
            ),
            div(cls:="form-group",
              button(data.dismiss:="modal", cls:="btn mb-2", "Cancel"),
              button(cls:="btn mb-2", id:="deleteFileOpenFileModal", data.toggle:="modal", data.dismiss:="modal", data.target:="#openFileModalAreYouSure", disabled:=true,
                "Delete"
              ),
              button(cls:="btn btn-primary mb-2", id:="submitOpenFileModal", `type`:="button", disabled:=true,
                data.toggle:="modal", data.dismiss:="modal", data.target:="#editFileModal",
                onclick:={(event: Event) =>
                  $("body").css("cursor", "wait")
                  EditFileModal.fileName_fileId() = (selected_file_header.now.getOrElse("SOMETHING WENT WRONG"), selected_file_id.now.getOrElse(-1).toString)
                  Case.updateCurrOpenFile(selected_file_id.now)
                  EditFileModal.fileName_fileId.recalc()
                },
                "Open"
              )

            )
          )
        )
      )
    )
  }

  def enableButtons() = {
    $("#deleteFileOpenFileModal").removeAttr("disabled")
    $("#submitOpenFileModal").removeAttr("disabled")
  }

  def disableButtons() = {
    $("#submitOpenFileModal").attr("disabled", true)
    $("#deleteFileOpenFileModal").attr("disabled", true)
  }

  def apply() = div(areYouSureModal(), mainModal())
}
