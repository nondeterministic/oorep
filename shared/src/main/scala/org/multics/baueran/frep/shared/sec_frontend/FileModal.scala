package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.{MyDate, dbFile}
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all.{cls, _}
import rx.{Rx, Var}
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._

// ----------------------------------------------------------------------------------------------------------------------------------------
// Common class to be used by modal dialogs which host a list of a user's files.
// ----------------------------------------------------------------------------------------------------------------------------------------

abstract class FileModal {

  private val headersClass = "col-6"

  var selected_file_header: Var[Option[String]] = Var(None)
  var selected_file_id: Var[Option[Int]] = Var(None)
  private val divs: Var[List[dom.html.Div]] = Var(List.empty)
  private var sortAscending = true

  def unselectAll() = {
    selected_file_id = Var(None)
    selected_file_header = Var(None)
    for (d <- divs.now)
      d.classList.remove("active")
  }

  def empty(): Unit = {
    divs() = List.empty
  }

  /**
    *
    * @return a list of file headers, stored in this FileModal
    */
  def headers(): List[String] = {
    divs.now.map(_.getElementsByClassName(headersClass) match {
      case null => return List.empty
      case hdrs => hdrs.item(0).asInstanceOf[dom.html.Div].innerHTML
    })
  }

  def appendItem(dbFile: dbFile, onClick: (dom.Event) => Unit) = {
    Rx {
      divs() =
        div(cls := "list-group-item list-group-item-action", name := "fileModalRow", data.toggle := "list", href := "#list-profile", role := "tab", onclick := onClick,
          div(cls := "row",
            div(cls := headersClass, dbFile.header),
            // INFO: `d-none d-md-block` hides these columns on small-ish screen sizes
            div(cls := "d-none d-md-block col-3", s"${new MyDate(dbFile.date).toHumanReadable()}"),
            div(cls := "d-none d-md-block col-3", s"${dbFile.case_ids.length}")
          )
        ).render :: divs.now
    }
  }

  def modalBodyFileSelection() = {
    div(cls := "form-group",
      div(cls := "d-none d-md-block list-group-item rounded-bottom bg-light text-dark", style := "margin-bottom:5px;",
        div(cls := "row",
          div(cls := headersClass,
            a(href:="#", onclick:={ (event: Event) => {
              if (divs.now.length > 0) {
                if (sortAscending)
                  divs() = divs.now.sortBy(_.getElementsByClassName(headersClass).item(0).asInstanceOf[dom.html.Div].innerHTML)
                else
                  divs() = divs.now.sortBy(_.getElementsByClassName(headersClass).item(0).asInstanceOf[dom.html.Div].innerHTML).reverse
                sortAscending = !sortAscending
              }
            } }, span(cls := "oi oi-elevator", title := "Sort by creation date", aria.hidden := "true")),
            b(style:="margin-left:10px;", "Name")
          ),
          div(cls := "col-3",
            a(href := "#", onclick := { (event: Event) => {
              if (divs.now.length > 0) {
                if (sortAscending)
                  divs() = divs.now.sortBy(_.getElementsByClassName("col-3").item(0).asInstanceOf[dom.html.Div].innerHTML)
                else
                  divs() = divs.now.sortBy(_.getElementsByClassName("col-3").item(0).asInstanceOf[dom.html.Div].innerHTML).reverse
                sortAscending = !sortAscending
              }
            } }, span(cls := "oi oi-elevator", title := "Sort by creation date", aria.hidden := "true")),
            b(style:="margin-left:10px;", "Created")
          ),
          div(cls := "col-3",
            a(href := "#", onclick := { (event: Event) => {
              if (divs.now.length > 0) {
                if (sortAscending)
                  divs() = divs.now.sortBy(_.getElementsByClassName("col-3").item(1).asInstanceOf[dom.html.Div].innerHTML.toInt)
                else
                  divs() = divs.now.sortBy(_.getElementsByClassName("col-3").item(1).asInstanceOf[dom.html.Div].innerHTML.toInt).reverse
                sortAscending = !sortAscending
              }
            } }, span(cls := "oi oi-elevator", title := "Sort by creation date", aria.hidden := "true")),
            b(style:="margin-left:10px;", "Cases")
          )
        )
      ),
      div(cls := "list-group", role := "tablist", id := "addToFileAvailableFilesList", style := "height: 250px; overflow-y: scroll;", Rx(divs()))
    )
  }

}
