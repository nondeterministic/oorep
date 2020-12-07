package org.multics.baueran.frep.shared.frontend

import org.querki.jquery.$
import org.scalajs.dom
import scalatags.JsDom.all._

class LoadingSpinner(parentId: String) {

  def add() = {
    $(s"#${parentId}").append(
      div(id:="loading", style:="text-align: center; position:fixed; top:50%; left:50%;",
        p(img(src:=s"${serverUrl()}/assets/html/img/ajax-loader.gif")),
        p("OOREP loading...")
      ).render
    )
  }

  def remove() = {
    dom.document.getElementById("loading") match {
      case null => ;
      case loading => dom.document.getElementById(parentId).removeChild(loading.asInstanceOf[dom.html.Element])
    }
  }

  def isVisible() = dom.document.getElementById("loading") != null

}
