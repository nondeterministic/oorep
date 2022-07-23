package org.multics.baueran.frep.frontend.public.base

import org.multics.baueran.frep.shared.MainUtil
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel
import org.multics.baueran.frep.shared.frontend.{LoadingSpinner, MainView, CaseModals}

@JSExportTopLevel("Main")
object Main extends MainUtil {

  def main(args: Array[String]): Unit = {
    loadJavaScriptDependencies()

    // This is the static page which is shown when JS is disabled
    if (dom.document.getElementById("temporary_content") != null)
      dom.document.body.removeChild(dom.document.getElementById("temporary_content"))

    dom.document.body.appendChild(CaseModals.Repertorisation().render)

    if (dom.document.getElementById("static_content") == null) {
      val loadingSpinner = new LoadingSpinner("content")
      loadingSpinner.add()
      MainView.init(loadingSpinner)

      // Both /?show...-calls call their own render() functions via html-page embedded JS
      if (dom.window.location.toString.contains("/show")) {
        dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "none")
      }
      // /?change_password mustn't execute the main OOREP application. So, do nothing!
      else if (dom.window.location.toString.contains("/change_password?")) {
        ;
      }
      // Static content must not also show the repertorisation view
      else {
        dom.document.getElementById("content").appendChild(MainView().render)
      }
    }

    if (dom.document.getElementById("nav_bar") != null)
      showNavBar()

    if (dom.document.getElementById("cookiePopup") != null)
      showCookieDialog()
  }

  // See MainUtil trait!
  override def updateDataStructuresFromBackendData(): Unit = {
    MainView.updateDataStructuresFromBackendData()
  }

}
