package org.multics.baueran.frep.shared.frontend

import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all.{onclick, _}

class ShareResultsModal(dialogId: String, resultsLink: () => String) {

  def updateResultsLink(update: String) = {
    dom.document.getElementById(s"${dialogId}shareResultsModalLink") match {
      case null => println(s"ERROR: ShareResultsModal ${dialogId} failed.")
      case element => element.asInstanceOf[dom.html.Input].value = update
    }
  }

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:=s"${dialogId}shareResultsModal",
      div(cls:="modal-dialog", role:="document",
        div(cls:="modal-content",
          div(cls:="modal-body",
            form(
              div(cls:="form-group",
                button(`type`:="button", cls:="close", data.dismiss:="modal", aria.label:="Close", span(aria.hidden:="true", "\u00d7")),
                label(`for`:=s"${dialogId}shareResultsModalLink", "To share the search results, copy and paste this link:"),
                input(cls:="form-control", id:=s"${dialogId}shareResultsModalLink", readonly:=true, value:=resultsLink())
              )
            )
          ),
          div(cls:="modal-footer",
            button(`type`:="button", cls:="btn btn-primary",
              onclick:= { (event: Event) =>
                dom.document.getElementById(s"${dialogId}shareResultsModalLink").asInstanceOf[dom.html.Input].select()
                dom.document.execCommand("copy")
              }, aria.label:="Copy to clipboard", "Copy to clipboard")
          )
        )
      )
    )
  }

}
