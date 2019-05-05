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

object EditFileModal {

  val fileName = Var("")
  private val casesHeight = Var(200)

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
                  button(cls:="btn mb-2 mr-2", id:="deleteFileEditFileModal", data.toggle:="modal", data.dismiss:="modal", disabled:=true, "Save"),
                  button(cls:="btn mb-2", data.dismiss:="modal", "Cancel")
                ),
                div(cls:="col")
              )
            ),

            div(cls:="border-top my-3"),

            div(cls:="form-group",
              div(
                label(`for`:="editFileAvailableFilesList", "Cases"),
                div(cls:="list-group", role:="tablist", id:="editFileAvailableFilesList", style:=Rx("height: " + casesHeight().toString() + "px; overflow-y: scroll;"),
                  a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", "<no files created yet>"))
              ),
              div(cls:="form-row",
                div(cls:="col"),
                div(cls:="col-2",
                  button(cls:="btn mb-2 mr-2", id:="deleteFileEditFileModal", data.toggle:="modal", data.dismiss:="modal", disabled:=true, "Open"),
                  button(cls:="btn mb-2", data.dismiss:="modal", "Delete")
                ),
                div(cls:="col")
              )
            )

          )
        )
      )
    )
  }

  def apply() = div(mainModal())

}
