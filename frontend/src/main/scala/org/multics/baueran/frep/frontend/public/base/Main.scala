package org.multics.baueran.frep.frontend.public.base

import org.multics.baueran.frep.shared.MainUtil
import org.multics.baueran.frep.shared.TopLevelUtilCode.{loadMainPageAndJumpToAnchor, sendAcceptCookies, toggleTheme}
import org.multics.baueran.frep.shared.frontend.CaseModals.RepertorisationModal.getId
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import org.multics.baueran.frep.shared.frontend.{CaseModals, LoadingSpinner, MainView}
import scalatags.JsDom.all.{cls, div, role, tabindex}

@JSExportTopLevel("Main")
object Main extends MainUtil {

  def main(args: Array[String]): Unit = {
    loadJavaScriptDependencies()

    // This is the static page which is shown when JS is disabled
    if (dom.document.getElementById("temporary_content") != null)
      dom.document.body.removeChild(dom.document.getElementById("temporary_content"))

    // //////////////////////////////////////////////////////////////////////////////////////////
    import org.scalajs.dom
    import dom.Event
    import scalatags.JsDom.all.{onkeydown, _}
    import org.scalajs.dom.{Event, html}
    import scala.scalajs.js
    import org.multics.baueran.frep.shared
    import shared.Defs.CookieFields
    dom.document.body.appendChild(div(h1("Andi Bauer")).render)
    dom.document.body.appendChild(div(h1("Andi Bauer")).render)
    // //////////////////////////////////////////////////////////////////////////////////////////

    val poo: scalatags.JsDom.TypedTag[html.Div] = div(h1("andi"))
    dom.document.body.appendChild(poo.render)
    def poo2(): scalatags.JsDom.TypedTag[html.Div] = div(h1("andi"))
    dom.document.body.appendChild(poo2().render)
    val poo3: scalatags.JsDom.TypedTag[html.Div] = CaseModals.RepertorisationModal().asInstanceOf[scalatags.JsDom.TypedTag[html.Div]]
    println("lalala1")
    println(poo3)
    println("lalal2")
    dom.document.body.appendChild(poo3.render)

    dom.document.body.appendChild(CaseModals.RepertorisationModal().render)

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

    // AOS, the animation library, is only loaded in the main landing page.
    // When you lookup something directly via link, AOS is not included,
    // and therefore an unprotected call to AOS.init() would crash.
    if (js.typeOf(AOS) != "undefined")
      AOSInit()

    dom.window.addEventListener("scroll", onScroll)

    dom.document.getElementById("cookiePopup") match {
      case null => ;
      case _ => showCookieDialog()
    }

    dom.document.getElementById("accept_cookies_button") match {
      case null => ;
      case elem => elem.addEventListener("click", (ev: dom.Event) => sendAcceptCookies())
    }

    dom.document.getElementById("about_link") match {
      case null => ;
      case elem => elem.addEventListener("click", (ev: dom.Event) => loadMainPageAndJumpToAnchor("about"))
    }

    dom.document.getElementById("features_link") match {
      case null => ;
      case elem => elem.addEventListener("click", (ev: dom.Event) => loadMainPageAndJumpToAnchor("features"))
    }

    dom.document.getElementById("transparency") match {
      case null => ;
      case elem => elem.addEventListener("click", (ev: dom.Event) => toggleTheme())
    }

    // This is to handle all the index_... pages, which do something after the main script has been loaded,
    // e.g. look something up or display the password-change dialog.
    handleCallsWithURIencodedParameters()
  }

  // See MainUtil trait!
  override def updateDataStructuresFromBackendData(): Unit = {
    MainView.updateDataStructuresFromBackendData()
  }

}
