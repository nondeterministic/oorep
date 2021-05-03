package org.multics.baueran.frep.frontend.public.base

import org.multics.baueran.frep.shared.MainUtil
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel
import scalatags.JsDom.all._
import org.multics.baueran.frep.shared.frontend.{Case, LoadingSpinner, Repertorise, serverUrl}

@JSExportTopLevel("Main")
object Main extends MainUtil {

  def main(args: Array[String]): Unit = {
    loadJavaScriptDependencies()

    if (dom.document.getElementById("temporary_content") != null)
      dom.document.body.removeChild(dom.document.getElementById("temporary_content"))

    dom.document.body.appendChild(Case.analysisModalDialogHTML().render)

    if (dom.document.getElementById("static_content") == null) {
      val loadingSpinner = new LoadingSpinner("content")
      loadingSpinner.add()
      Repertorise.init(loadingSpinner)

      // /?show=... calls its own Repertorise().render via html-page embedded JS
      if (dom.window.location.toString.contains("/show?")) {
        dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "none")
      }
      // /?change_password mustn't execute the main OOREP application. So, do nothing!
      else if (dom.window.location.toString.contains("/change_password?")) {
        ;
      }
      // Static content must not also show the repertorisation view
      else {
        dom.document.getElementById("content").appendChild(Repertorise().render)
      }
    }

    if (dom.document.getElementById("nav_bar") != null)
      showNavBar()

    if (dom.document.getElementById("cookiePopup") != null)
      showCookieDialog()
  }
}
