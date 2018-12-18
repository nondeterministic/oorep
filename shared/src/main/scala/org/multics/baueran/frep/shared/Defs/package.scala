package org.multics.baueran.frep.shared

package object Defs {
  def serverUrl() = "http://localhost:9000"
  def localRepPath() = "/home/baueran/Development/Scala/frep/project/backend/public/repertories/"
  def localHTMLPath() = "/home/baueran/Development/Scala/frep/project/backend/public/html/"

  object AppMode extends Enumeration {
    type AppMode = Value
    val Public = Value("Public")
    val Secure = Value("Secure")
  }
}
