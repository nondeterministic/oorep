package org.multics.baueran.frep.shared

import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.frontend.{ChangePasswordForm, MainView, getCookieData, serverUrl}
import org.multics.baueran.frep.shared.frontend.views.materiamedica.MateriaMedicaView
import org.multics.baueran.frep.shared.frontend.views.repertory.RepertoryView
import org.scalajs.dom
import scalatags.JsDom.all._
import monix.execution.Scheduler.Implicits.global

import scala.scalajs.js.URIUtils.decodeURIComponent

trait MainUtil {

  // This is to handle all the index_... pages, which do something after the main script has been loaded,
  // e.g. look something up or display the password-change dialog.
  def handleCallsWithURIencodedParameters() = {
    val mainJSScript = dom.document.getElementById("main_script") // We don't need to check for null, this always exists or we wouldn't be here...
    mainJSScript.getAttribute("data-landing") match {
      case null => ;
      case "lookup_rep" =>
        val rep = mainJSScript.getAttribute("data-repertory")
        val sym = decodeURIComponent(mainJSScript.getAttribute("data-symptom")).replace('+', ' ')
        val page = mainJSScript.getAttribute("data-page")
        val rem = mainJSScript.getAttribute("data-remedystring")
        val weight = mainJSScript.getAttribute("data-minweight")
        RepertoryView.jsDoLookup(rep, sym, page.toInt, rem, weight.toInt)
      case "lookup_mm" =>
        val mm = mainJSScript.getAttribute("data-mm")
        val sym = decodeURIComponent(mainJSScript.getAttribute("data-symptom")).replace('+', ' ')
        val page = mainJSScript.getAttribute("data-page")
        val rem = mainJSScript.getAttribute("data-remedystring")
        val hideSections = mainJSScript.getAttribute("data-hidesections")
        MateriaMedicaView.jsDoLookup(mm, sym, page.toInt, hideSections.toBoolean, rem)
      case "change_password" =>
        val memberId = mainJSScript.getAttribute("data-memberid")
        val pcrId = mainJSScript.getAttribute("data-pcrid")
        ChangePasswordForm.show(memberId.toInt, pcrId)
      case _ => ;
    }
  }

  def loadJavaScriptDependencies() = {
    import scala.concurrent.Future
    import scala.concurrent.blocking

    Future {
      blocking {
        val scriptBootstrap = dom.document.createElement("script").asInstanceOf[dom.html.Script]
        scriptBootstrap.src = s"${serverUrl()}/assets/html/third-party/bootstrap-4.3.1/js/bootstrap.bundle.min.js"
        scriptBootstrap.async = true
        dom.document.head.appendChild(scriptBootstrap)
      }
    }

    Future {
      blocking {
        val styleOpenIconic = dom.document.createElement("link").asInstanceOf[dom.html.Link]
        styleOpenIconic.href = s"${serverUrl()}/assets/html/third-party/fonts/open-iconic-master/font/css/open-iconic-bootstrap.min.css"
        styleOpenIconic.`type` = "text/css"
        styleOpenIconic.rel = "stylesheet"
        styleOpenIconic.media = "screen,print"
        dom.document.head.appendChild(styleOpenIconic)
      }
    }
  }

  def showCookieDialog() = {
    getCookieData(dom.document.cookie, CookieFields.cookiePopupAccepted.toString) match {
      case Some(_) => ;
      case None => {
        dom.document.getElementById("cookiePopup") match {
          case null => ;
          case popup => popup.classList.add("show")
        }
      }
    }
  }

  // This will be added as an event listener to dom.window (by the Main objects)
  def onScroll(ev: dom.Event): Unit = {
    if (!MainView.someResultsHaveBeenShown()) {
      (dom.document.getElementById("nav_bar"), dom.document.getElementById("nav_bar_logo")) match {
        case (navBar, navBarLogo) if navBar != null && navBarLogo != null =>
          // https://stackoverflow.com/questions/29479366/jquery-alternative-to-scrolltop
          if (dom.window.pageYOffset > 150) {
            if (!navBar.classList.contains("bg-dark")) {
              navBar.classList.add("bg-dark")
              navBar.classList.add("navbar-dark")
              navBar.classList.add("shadow")
              navBarLogo.appendChild(a(cls := "navbar-brand py-0", style := "margin-top:8px;", href := serverUrl(), h5(cls := "freetext", "OOREP")).render)
            }
          }
          else {
            navBar.classList.remove("bg-dark")
            navBar.classList.remove("navbar-dark")
            navBar.classList.remove("shadow")
            while (navBarLogo.hasChildNodes()) navBarLogo.removeChild(navBarLogo.firstChild)
          }
        case _ =>
          println("MainUtil: Error displaying navbar.")
      }
    }
  }

  // This is like a class constructor: we want Main to get the data from the backend as soon as OOREP application has started up.
  // The second line basically calls all implementations of updateDataStructuresFromBackendData() that exist.
  def updateDataStructuresFromBackendData(): Unit
  updateDataStructuresFromBackendData()

}
