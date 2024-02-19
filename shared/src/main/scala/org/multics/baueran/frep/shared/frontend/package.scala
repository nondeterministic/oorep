package org.multics.baueran.frep.shared

import org.scalajs.dom

package object frontend {

  def serverUrl() = {
    dom.document.location.protocol + "//" + dom.document.location.hostname +
      (if (dom.document.location.port.length > 0)
        ":" + dom.document.location.port
      else
        "")
  }

  def apiPrefix() = "api"

  def getCookieData(cookie: String, elementName: String): Option[String] = {
    cookie.split(";").map(_.trim).foreach({ c: String =>
      c.split("=").map(_.trim).toList match {
        case name :: argument :: Nil => if (name.toLowerCase() == elementName.toLowerCase()) return Some(argument)
        case _ => ; // Do nothing
      }
    })
    None
  }

}
