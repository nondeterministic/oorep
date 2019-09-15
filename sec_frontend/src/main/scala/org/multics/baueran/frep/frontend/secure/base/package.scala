package org.multics.baueran.frep.frontend.secure

import org.multics.baueran.frep.shared.Defs.CookieFields
import org.scalajs.dom

package object base {

  def deleteCookies() = {
    val cookieNames = List(CookieFields.email.toString, CookieFields.hash.toString, CookieFields.id.toString)
    dom.document.cookie = "PLAY_SESSION=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT"
    cookieNames.foreach(cookieName =>
      dom.document.cookie = s"${cookieName}=; path=/; expires='Thu, 01 Jan 1970 00:00:01 GMT"
    )
  }

}
