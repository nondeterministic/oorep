package org.multics.baueran.frep.shared.sec_frontend

import org.multics.baueran.frep.shared.{MyDate, dbFile}
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all.{cls, _}

// ----------------------------------------------------------------------------------------------------------------------------------------
// Common class to be used by modal dialogs which host a list of a user's files.
// ----------------------------------------------------------------------------------------------------------------------------------------

abstract class FileModal(idPrefix: String) {

  private val headersClass = "col-6"

  var selected_file_header: Option[String] = None
  var selected_file_id: Option[Int] = None
  private var divs: List[dom.html.Div] = List.empty
  private var sortAscending = true
  private val modalBodyFileSelectionId = idPrefix + "FileModal_addToFileAvailableFilesList"

  def unselectAll() = {
    selected_file_id = None
    selected_file_header = None
    for (d <- divs)
      d.classList.remove("active")
  }

  def empty(): Unit = {
    val elem = dom.document.getElementById(modalBodyFileSelectionId)
    while (elem.hasChildNodes())
      elem.removeChild(elem.firstChild)
    divs = List.empty
  }

  /**
    *
    * @return a list of file headers, stored in this FileModal
    */
  def headers(): List[String] = {
    divs.map(_.getElementsByClassName(headersClass) match {
      case null => return List.empty
      case hdrs => hdrs.item(0).asInstanceOf[dom.html.Div].innerHTML
    })
  }

  def appendItem(dbFile: dbFile, onClick: (dom.Event) => Unit): Unit = {
    val newDiv =
      div(cls := "list-group-item list-group-item-action", name := "fileModalRow", data.toggle := "list", href := "#list-profile", role := "tab", onclick := onClick,
        div(cls := "row",
          div(cls := headersClass, dbFile.header),
          // INFO: `d-none d-md-block` hides these columns on small-ish screen sizes
          div(cls := "d-none d-md-block col-3", s"${new MyDate(dbFile.date).toHumanReadable()}"),
          div(cls := "d-none d-md-block col-3", s"${dbFile.case_ids.length}")
        )
      )

    divs = newDiv.render :: divs
    dom.document.getElementById(modalBodyFileSelectionId).appendChild(newDiv.render)
  }

  // Append all the files to the files-div; usually called before modal is shown, e.g., by means of onshow() event handler.
  def updateData(): Unit = {
    val elem = dom.document.getElementById(modalBodyFileSelectionId)
    while (elem.hasChildNodes())
      elem.removeChild(elem.firstChild)

    for (d <- divs)
      dom.document.getElementById(modalBodyFileSelectionId).asInstanceOf[dom.html.Div].appendChild(d)
  }

  def modalBodyFileSelection() = {
    div(cls := "form-group",
      div(cls := "d-none d-md-block list-group-item rounded-bottom bg-light text-dark", style := "margin-bottom:5px;",
        div(cls := "row",
          div(cls := headersClass,
            a(href:="#", onclick:={ (event: Event) => {
              if (divs.length > 0) {
                if (sortAscending)
                  divs = divs.sortBy(_.getElementsByClassName(headersClass).item(0).asInstanceOf[dom.html.Div].innerHTML)
                else
                  divs = divs.sortBy(_.getElementsByClassName(headersClass).item(0).asInstanceOf[dom.html.Div].innerHTML).reverse
                sortAscending = !sortAscending
              }
            } }, span(cls := "oi oi-elevator", title := "Sort by creation date", aria.hidden := "true")),
            b(style:="margin-left:10px;", "Name")
          ),
          div(cls := "col-3",
            a(href := "#", onclick := { (event: Event) => {
              if (divs.length > 0) {
                if (sortAscending)
                  divs = divs.sortBy(_.getElementsByClassName("col-3").item(0).asInstanceOf[dom.html.Div].innerHTML)
                else
                  divs = divs.sortBy(_.getElementsByClassName("col-3").item(0).asInstanceOf[dom.html.Div].innerHTML).reverse
                sortAscending = !sortAscending
              }
            } }, span(cls := "oi oi-elevator", title := "Sort by creation date", aria.hidden := "true")),
            b(style:="margin-left:10px;", "Created")
          ),
          div(cls := "col-3",
            a(href := "#", onclick := { (event: Event) => {
              if (divs.length > 0) {
                if (sortAscending)
                  divs = divs.sortBy(_.getElementsByClassName("col-3").item(1).asInstanceOf[dom.html.Div].innerHTML.toInt)
                else
                  divs = divs.sortBy(_.getElementsByClassName("col-3").item(1).asInstanceOf[dom.html.Div].innerHTML.toInt).reverse
                sortAscending = !sortAscending
              }
            } }, span(cls := "oi oi-elevator", title := "Sort by creation date", aria.hidden := "true")),
            b(style:="margin-left:10px;", "Cases")
          )
        )
      ),
      div(cls := "list-group", role := "tablist", id := modalBodyFileSelectionId, style := "height: 250px; overflow-y: scroll;",
        divs
      )
    )
  }

}
