package org.multics.baueran.frep.shared

import org.scalajs.dom

package object Defs {

  // TODO: We ended up only using rep. REMOVE rem AND min!
  object SpecialLookupParams extends Enumeration {
    type SpecialSearchParams = Value
    val Remedy = Value("rem")
    val Repertory = Value("rep")
    val MinWeight = Value("min")
  }
  import SpecialLookupParams._

  def localRepPath() = {
    sys.env.get("OOREP_REP_PATH") match {
      case Some(path) => path
      case _ => {
        System.err.println(s"ERROR: localRepPath: environment variable OOREP_REP_PATH not defined. Returning empty string to calling function.")
        ""
      }
    }
  }

  def maxNumberOfResults = 100
  def maxLengthOfSymptoms = 200
  def maxNumberOfSymptoms = 20

  // Do not rename csrfCookie unless you know what you're doing!
  object CookieFields extends Enumeration {
    type CookieFields = Value
    val id, csrfCookie, cookiePopupAccepted = Value
    // val email, salt, id, csrfCookie, cookiePopupAccepted, creationDate = Value
  }

  def deleteCookies() = {
    val cookieNames = CookieFields.values.map(_.toString)
    dom.document.cookie = "PLAY_SESSION=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT"
    cookieNames.foreach(cookieName =>
      dom.document.cookie = s"${cookieName}=; path=/; expires='Thu, 01 Jan 1970 00:00:01 GMT"
    )
  }

}
