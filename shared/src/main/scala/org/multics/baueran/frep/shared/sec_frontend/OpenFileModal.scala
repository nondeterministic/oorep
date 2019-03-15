package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.syntax._
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.multics.baueran.frep.shared.frontend.{Case, getCookieData}
import org.multics.baueran.frep.shared.sec_frontend.AddToFileModal.selected_file_id
import org.multics.baueran.frep.shared.sec_frontend.Callbacks.updateMemberFiles
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all._
import rx.Var
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

object OpenFileModal {

  val selected_file_id = Var("")

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
            h5(cls:="modal-title", s"Really delete file ${selected_file_id}?"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", aria.label:="Close", span(aria.hidden:="true", "\u00d7"))
          ),
          div(cls:="modal-body",
            p("Deleting this file will also delete all of its cases!")
          ),
          div(cls:="modal-footer",
            button(`type`:="button", cls:="btn btn-secondary", data.dismiss:="modal", "Cancel"),
            button(`type`:="button", cls:="btn btn-primary",
              onclick:= { (event: Event) => requestFileDeletion() },
              "Delete")
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
                a(cls := "list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", "<no files created yet>"))
            ),
            div(cls:="form-group",
              button(data.dismiss:="modal", cls:="btn mb-2", "Cancel"),
              button(cls:="btn mb-2", data.toggle:="modal", data.dismiss:="modal", data.target:="#openFileModalAreYouSure", "Delete"),
              button(cls:="btn btn-primary mb-2", `type`:="button", id:="submitOpenFileModal", disabled:=true,
                onclick:={(event: Event) =>
                  println("TODO: Opening of " + selected_file_id)
                  // js.eval("$('#openFileModal').modal('hide');")
                },
                "Open"
              )

            )
          )
        )
      )
    )

  }

  def apply() = div(areYouSureModal(), mainModal())
}
