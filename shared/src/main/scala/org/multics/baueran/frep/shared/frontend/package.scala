package org.multics.baueran.frep.shared

import org.scalajs.dom

package object frontend {

  def serverUrl() = {
    val ret = dom.document.location.protocol + "//" + dom.document.location.hostname + ":" + dom.document.location.port
    println("999999999999999999999 " + ret)
    ret
  }

  def getCookieData(cookie: String, elementName: String): Option[String] = {
    cookie.split(";").map(_.trim).foreach({ c: String =>
      c.split("=").map(_.trim).toList match {
        case name :: argument :: Nil => if (name == elementName) return Some(argument)
        case _ => ; // Do nothing
      }
    })
    None
  }

}
