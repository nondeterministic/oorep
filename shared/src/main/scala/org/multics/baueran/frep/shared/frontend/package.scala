package org.multics.baueran.frep.shared

import org.scalajs.dom
import scala.util.boundary, boundary.break

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
    boundary:
      cookie.split(";").map(_.trim).foreach({ c =>
        c.split("=").map(_.trim).toList match {
          case name :: argument :: Nil => if (name.toLowerCase() == elementName.toLowerCase()) break(Some(argument))
          case _ => ; // Do nothing
        }
      })
      None
  }

}
