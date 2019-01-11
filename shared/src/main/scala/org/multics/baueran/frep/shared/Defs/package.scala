package org.multics.baueran.frep.shared

import org.multics.baueran.frep

package object Defs {
  def serverUrl() = "http://localhost:9000"

  // Home laptop:
  def localRepPath() = "/home/baueran/Development/Scala/frep/project/backend/public/repertories/"
  def localHTMLPath() = "/home/baueran/Development/Scala/frep/project/backend/public/html/"
  
  // Work laptop:
  // def localRepPath() = "/home/bauera/Development/oorep/backend/public/repertories/"
  // def localHTMLPath() = "/home/bauera/Development/oorep/backend/public/html/"

  object AppMode extends Enumeration {
    type AppMode = Value
    val Public = Value("Public")
    val Secure = Value("Secure")
  }
}
