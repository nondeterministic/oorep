package org.multics.baueran.frep.frontend.secure.base

import scala.scalajs.js.annotation.JSExportTopLevel
import org.multics.baueran.frep.shared._
import frontend.{CaseModals, LoadingSpinner, MainView, apiPrefix, serverUrl}
import TopLevelUtilCode.{deleteCookies, toggleTheme}
import sec_frontend.{AddToFileModal, EditFileModal, FileModalCallbacks, NewFileModal, OpenFileModal}

import scalatags.JsDom.all.{id, _}
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.scalajs.js.URIUtils.encodeURI

@JSExportTopLevel("MainSecure")
object Main extends MainUtil {

  private def authenticateAndPrepare(): Unit = {
    HttpRequest2("authenticate")
      .onSuccess((response: String) => {
        if (dom.document.getElementById("static_content") == null) {
          dom.document.getElementById("content").appendChild(MainView().render)
        }

        try {
          val memberId = response.toInt
          FileModalCallbacks.updateMemberFiles(memberId)
        } catch {
          case exception: Throwable =>
            dom.document.location.replace(s"${serverUrl()}/${apiPrefix()}/display_error_page?message=${encodeURI("Not authenticated or cookie expired")}")
            println("Exception: could not convert member-id '" + exception + "'. Deleting the cookies now!")
            deleteCookies()
        }
      })
      .onFailure((_) => {
        dom.document.location.replace(s"${serverUrl()}/${apiPrefix()}/display_error_page?message=${encodeURI("Not authenticated or cookie expired")}")
        deleteCookies()
      })
      .send()
  }

  def main(args: Array[String]): Unit = {
    loadJavaScriptDependencies()

    // This is the static page which is shown when JS is disabled
    if (dom.document.getElementById("temporary_content") != null)
      dom.document.body.removeChild(dom.document.getElementById("temporary_content"))

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
      else {
        // Static content must not also show the repertorisation view
        // dom.document.getElementById("content").appendChild(MainView().render)
        ;
      }
    }

    dom.document.body.appendChild(div(style := "width:100%;", id := "content_bottom").render)
    dom.document.body.appendChild(AddToFileModal().render)
    dom.document.body.appendChild(OpenFileModal().render)
    dom.document.body.appendChild(EditFileModal().render)
    dom.document.body.appendChild(NewFileModal().render)
    dom.document.body.appendChild(CaseModals.RepertorisationModal().render)
    dom.document.body.appendChild(CaseModals.EditModal().render)

    // Some event handlers for the navbar here
    dom.document.getElementById("navbar_open_file") match {
      case null => println("Could not set navbar event handler. Is the navbar loaded?")
      case anchr => anchr.asInstanceOf[dom.html.Anchor].onclick = (ev: Event) => {
        OpenFileModal.unselectAll()
      }
    }

    dom.window.addEventListener("scroll", onScroll)

    dom.document.getElementById("navbar_logout") match {
      case null => ;
      case elem => elem.addEventListener("click", (ev: dom.Event) => deleteCookies())
    }

    if (!dom.window.location.toString.contains("/show"))
      authenticateAndPrepare()
    else
      dom.document.getElementById("disclaimer_div").asInstanceOf[dom.html.Div].style.setProperty("display", "none")

    dom.document.getElementById("transparency") match {
      case null => ;
      case elem => elem.addEventListener("click", (ev: dom.Event) => toggleTheme())
    }

    // This is to handle all the index_... pages, which do something after the main script has been loaded,
    // e.g. look something up or display the password-change dialog.
    handleCallsWithURIencodedParameters()
  }

  // See MainUtil trait!
  override def updateDataStructuresFromBackendData() = {
    MainView.updateDataStructuresFromBackendData()
  }

}
