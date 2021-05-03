package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.FIle
import org.multics.baueran.frep.shared.frontend.{Case, apiPrefix, getCookieData, serverUrl, Notify}
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.PlainTextBody
import fr.hmil.roshttp.response.SimpleHttpResponse
import scalatags.JsDom.all.{id, input, _}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement
import monix.execution.Scheduler.Implicits.global

import scala.scalajs.js
import scala.util.{Failure, Success}
import io.circe.syntax._

object NewFileModal {

  private val theForm = form(id:="new_file_form",
    div(cls:="form-group",
      label(`for`:="fileHeader", "ID"),
      input(cls:="form-control",
        id:="fileHeader",
        placeholder:="A simple, unique file identifier")
    ),
    div(cls:="form-group",
      label(`for`:="fileDescr", "Description"),
      textarea(cls:="form-control", id:="fileDescr", rows:="3", placeholder:="A more verbose description of the file")
    ),
    div(cls:="d-flex flex-row-reverse",
      button(cls:="btn btn-primary mb-2", style:="margin-left:8px;", `type`:="submit", "Submit"),
      button(data.dismiss:="modal", cls:="btn mb-2 btn-secondary",
        "Cancel",
        onclick:={ (event: Event) =>
          event.preventDefault()
          dom.document.getElementById("fileHeader").asInstanceOf[HTMLInputElement].value = ""
          dom.document.getElementById("fileDescr").asInstanceOf[HTMLInputElement].value = ""
        }),
    )
  )

  def onSubmit = (event: Event) => {
    event.preventDefault()

    val header = dom.document.getElementById("fileHeader").asInstanceOf[HTMLInputElement]
    val descr = dom.document.getElementById("fileDescr").asInstanceOf[HTMLInputElement]
    val memberId = getCookieData(dom.document.cookie, CookieFields.id.toString) match {
      case Some(id) => id.toInt
      case None => -1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
    }
    val currFIle = Some(FIle(None, header.value, memberId, (new js.Date()).toISOString(), descr.value, List.empty))

    HttpRequest(s"${serverUrl()}/${apiPrefix()}/sec/save_file")
      .withHeader("Csrf-Token", getCookieData(dom.document.cookie, CookieFields.csrfCookie.toString).getOrElse(""))
      .post(PlainTextBody(currFIle.get.asJson.toString()))
      .onComplete({
        case _: Success[SimpleHttpResponse] => {
          Case.updateCaseViewAndDataStructures()
          header.value = ""
          descr.value = ""
          js.eval("$('#newFileModal').modal('hide');")
        }
        case _ => {
          if (Notify.noAlertsVisible())
            new Notify("tempFeedbackAlert", "Saving file failed. Make sure file ID is unique!")
        }
      })
  }

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="newFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Create a new file"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              theForm
            )
          )
        )
      )
    )
  }
}
