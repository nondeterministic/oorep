package org.multics.baueran.frep.shared

package object frontend {

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
