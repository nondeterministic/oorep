package org.multics.baueran.frep.shared

package object Defs {
  def serverUrl() = "http://localhost:9000"
  def relHtmlPath() = serverUrl() + "/assets/html/"

  // Home laptop:
  def localRepPath() = "/home/baueran/Development/Scala/frep/project/backend/public/repertories/"
  def localHTMLPath() = "/home/baueran/Development/Scala/frep/project/backend/public/html/"

  def maxNumberOfResults = 150

  object CookieFields extends Enumeration {
    type CookieFields = Value
    val oorep_member_email, oorep_member_hash, oorep_member_id = Value
  }
}
