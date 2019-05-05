package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.syntax._
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.multics.baueran.frep.shared.frontend.{Case, getCookieData}
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

object OpenFileModal {

  val selected_file_id = Var("") // Set outside in Callbacks.scala!

  private def requestFileDeletion() = {
    getCookieData(dom.document.cookie, "oorep_member_id") match {
      case Some(memberId) =>
        println("Sending: " + selected_file_id.now + memberId.toString)
        HttpRequest(serverUrl() + "/delfile")
          .post(MultiPartBody(
            "fileheader" -> PlainTextBody(selected_file_id.now),
            "memberId" -> PlainTextBody(memberId.toString)))
          .onComplete({ case _ => println("TODO: Update data structures in modals!")})
      case None => ; // TODO: Display error modal?!
    }
  }

  private def areYouSureModal() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="openFileModalAreYouSure",
      div(cls:="modal-dialog", role:="document",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", Rx("Really delete file " + selected_file_id() + "?")), // reallyDeleteVarString),
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
              div(cls:="list-group", role:="tablist", id:="openFileAvailableFilesList", style:="height: 250px; overflow-y: scroll;",
                a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", "<no files created yet>"))
            ),
            div(cls:="form-group",
              button(data.dismiss:="modal", cls:="btn mb-2", "Cancel"),
              button(cls:="btn mb-2", id:="deleteFileOpenFileModal", data.toggle:="modal", data.dismiss:="modal", data.target:="#openFileModalAreYouSure", disabled:=true,
                "Delete"
              ),
              button(cls:="btn btn-primary mb-2", id:="submitOpenFileModal", `type`:="button", disabled:=true,
                data.toggle:="modal", data.dismiss:="modal", data.target:="#editFileModal",
                onclick:={(event: Event) =>
                  EditFileModal.fileName() = selected_file_id.now
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
    println("Enabling")
    $("#deleteFileOpenFileModal").removeAttr("disabled")
    $("#submitOpenFileModal").removeAttr("disabled")
  }

  def disableButtons() = {
    println("Disabling")
    $("#submitOpenFileModal").attr("disabled", true)
    $("#deleteFileOpenFileModal").attr("disabled", true)
  }

  def empty() = {
    $("#openFileAvailableFilesList").empty()
  }

  def appendItem(listItem: Anchor) = {
    $("#openFileAvailableFilesList").append(listItem)
  }

  def apply() = div(areYouSureModal(), mainModal())
}
