package org.multics.baueran.frep.shared

import org.multics.baueran.frep.shared.Defs.CookieFields
import org.multics.baueran.frep.shared.frontend.{Repertorise, getCookieData, serverUrl}
import org.querki.jquery.$
import org.scalajs.dom
import scalatags.JsDom.all._
import monix.execution.Scheduler.Implicits.global

trait MainUtil {

  def loadJavaScriptDependencies() = {
    import scala.concurrent.Future
    import scala.concurrent.blocking

    Future {
      blocking {
        val scriptNotify = dom.document.createElement("script").asInstanceOf[dom.html.Script]
        scriptNotify.src = s"${serverUrl()}/assets/html/third-party/notify.min.js"
        scriptNotify.async = true
        dom.document.head.appendChild(scriptNotify)
      }
    }

    Future {
      blocking {
        val scriptPopper = dom.document.createElement("script").asInstanceOf[dom.html.Script]
        scriptPopper.src = s"${serverUrl()}/assets/html/third-party/popper.min.js"
        scriptPopper.async = true
        dom.document.head.appendChild(scriptPopper)
      }
    }

    Future {
      blocking {
        val scriptBootstrap = dom.document.createElement("script").asInstanceOf[dom.html.Script]
        scriptBootstrap.src = s"${serverUrl()}/assets/html/third-party/bootstrap-4.2.1/js/bootstrap.min.js"
        scriptBootstrap.async = true
        dom.document.head.appendChild(scriptBootstrap)
      }
    }

    Future {
      blocking {
        val styleOpenIconic = dom.document.createElement("link").asInstanceOf[dom.html.Link]
        styleOpenIconic.href = s"${serverUrl()}/assets/html/third-party/open-iconic-master/font/css/open-iconic-bootstrap.min.css"
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
      case None => $("#cookiePopup").addClass("show")
    }
  }

  def showNavBar() = {
    $(dom.window).scroll(() => {
      if (Repertorise._repertorisationResults.now.size == 0) {
        if ($(dom.document).scrollTop() > 150) {
          if (!dom.document.getElementById("nav_bar").asInstanceOf[dom.html.Element].classList.contains("bg-dark")) {
            $("#nav_bar").addClass("bg-dark navbar-dark shadow p-3 mb-5")
            $("#nav_bar_logo").append(a(cls:="navbar-brand py-0", style:="margin-top:8px;", href:=serverUrl(), h5(cls:="freetext", "OOREP")).render)
          }
        }
        else {
          $("#nav_bar").removeClass("bg-dark navbar-dark shadow p-3 mb-5")
          $("#nav_bar_logo").empty()
        }
      }
    })
  }

}
