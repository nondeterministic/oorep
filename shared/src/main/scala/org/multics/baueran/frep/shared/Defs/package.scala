package org.multics.baueran.frep.shared

package object Defs {
  def serverUrl() = "http://localhost:9000"
  def relHtmlPath() = serverUrl() + "/assets/html/"
  def localRepPath() = "/home/baueran/Development/Scala/oorep/project/backend/public/repertories/"
  def maxNumberOfResults = 100

  object CookieFields extends Enumeration {
    type CookieFields = Value
    val email, hash, id = Value
  }
}
