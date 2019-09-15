package org.multics.baueran.frep.shared

package object Defs {
  def localRepPath() = "/home/baueran/Development/Scala/oorep/project/backend/public/repertories/"
  def maxNumberOfResults = 150

  object CookieFields extends Enumeration {
    type CookieFields = Value
    val email, hash, id = Value
  }
}
