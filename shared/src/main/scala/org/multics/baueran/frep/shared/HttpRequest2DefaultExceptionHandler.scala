package org.multics.baueran.frep.shared

import org.scalajs.dom
import org.multics.baueran.frep.shared.TopLevelUtilCode.deleteCustomCookies
import org.multics.baueran.frep.shared.frontend.{MainView, Notify, apiPrefix, serverUrl, Case}

class HttpRequest2DefaultExceptionHandler(apiURI: String, responseCode: Int = -1) {

  // This will ensure the app stays responsive after a network transmission error
  // A bit clumsy, as this is some kind of side-effect, but OK for now...

  def apply(): Unit = {
    if (Notify.noAlertsVisible())
      new Notify("tempFeedbackAlert", "ERROR: Network error. Maybe try again?!")
      dom.document.body.classList.remove("wait")

      println(s"HttpRequest2: http-response code: ${responseCode}")

      // If something goes wrong we delete all BUT the Play session
      // cookie, so the user can keep using the app, but not with
      // any data of a logged in user / from a stale cookie.

      if (apiURI.contains("/sec/")) {
        deleteCustomCookies()
        dom.document.getElementById("nav_bar").replaceWith(NavBarAnon().render)

        // If we log the user out effectively, we also need to hide all but the Repertorise-button
        // if a case is currently open.
        if (Case.size() > 0) {
          Case.AddToFileButton.hide()
          Case.OpenNewCaseButton.hide()
          Case.CloneCaseButton.hide()
          Case.CloseCaseButton.hide()
          Case.EditDescrButton.hide()
        }
      }
  }

}