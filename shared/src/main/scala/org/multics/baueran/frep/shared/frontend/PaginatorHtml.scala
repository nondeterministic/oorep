package org.multics.baueran.frep.shared.frontend

import org.multics.baueran.frep.shared.PaginationResult
import scalatags.JsDom
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.nav
import org.scalajs.dom.html
import org.scalajs.dom.Event

class PaginatorHtml(paginationDivName: String, pagination: PaginationResult) {

  // currPage starts at 0 for the first page of results.
  private def doToHtml(abbrev: String, symptom: String, remedyString: Option[String], minWeight: Int, callBack: (String, String, Option[Int], Option[String], Int) => Unit) = {
    var links: List[JsDom.TypedTag[html.Element]]  = List.empty
    val currPage = pagination.currPage - 1

    val minus10 =
      if (currPage >= 10)
        li(cls := "page-item", data.toggle:="tooltip", title:="-10",
          a(cls:="page-link", href:=s"#${paginationDivName}", onclick:={ (event: Event) => callBack(abbrev, symptom, Some(currPage - 10), remedyString, minWeight) }, s"<<"))
      else
        li(cls := "page-item disabled", data.toggle:="tooltip", title:="-10",
          a(cls:="page-link", href:=s"#${paginationDivName}", s"<<"))

    val plus10 =
      if (currPage < pagination.pageMax - 10)
        li(cls := "page-item", data.toggle:="tooltip", title:="+10",
          a(cls:="page-link", href:=s"#${paginationDivName}", onclick:={ (event: Event) => callBack(abbrev, symptom, Some(currPage + 10), remedyString, minWeight) }, s">>"))
      else
        li(cls := "page-item disabled", data.toggle:="tooltip", title:="+10",
          a(cls:="page-link", href:=s"#${paginationDivName}", s">>"))

    def createLinks(items: List[Int]) =
      items.sorted.map(p => {
        if (p - 1 != currPage)
          li(cls := "page-item",
            a(cls:="page-link", href:=s"#${paginationDivName}", onclick:={ (event: Event) => callBack(abbrev, symptom, Some(p - 1), remedyString, minWeight) }, s"${p}"))
        else
          li(cls := "page-item disabled",
            a(cls:="page-link", href:=s"#${paginationDivName}", b(s"${p}")))
      })

    if (pagination.right.size > 0) {
      links = createLinks(pagination.right) ::: links
      links = li(cls:="page-item disabled", a(cls:="page-link", "...")) :: links
    }

    if (pagination.middle.size > 0) {
      links = createLinks(pagination.middle) ::: links
      links = li(cls:="page-item disabled", a(cls:="page-link", "...")) :: links
    }

    if (pagination.left.size > 0)
      links = createLinks(pagination.left) ::: links

    nav(ul(cls:="pagination justify-content-center",
      minus10, links, plus10))
  }

  // Called for/by MM
  def toHtml(abbrev: String, symptom: String, remedyString: Option[String], callBack: (String, String, Option[Int], Option[String]) => Unit) = {
    def tmpCallBack(abbrev: String, symptom: String, page: Option[Int], remedyStringOpt: Option[String], IGNORE: Int): Unit = { callBack(abbrev, symptom, page, remedyStringOpt) }
    doToHtml(abbrev, symptom, remedyString, -1, tmpCallBack)
  }

  // Called for/by Rep
  def toHtml(abbrev: String, symptom: String, remedyString: Option[String], minWeight: Int, callBack: (String, String, Option[Int], Option[String], Int) => Unit) =
    doToHtml(abbrev, symptom, remedyString, minWeight, callBack)

}
