package org.multics.baueran.frep.shared

package object Defs {
  def serverUrl() = "http://localhost:9000"
  def relHtmlPath() = serverUrl() + "/assets/html/"

  // Home laptop:
  def localRepPath() = "/home/baueran/Development/Scala/frep/project/backend/public/repertories/"
  def localHTMLPath() = "/home/baueran/Development/Scala/frep/project/backend/public/html/"

  def maxNumberOfResults = 100

  // Work laptop:
  // def localRepPath() = "/home/bauera/Development/oorep/backend/public/repertories/"
  // def localHTMLPath() = "/home/bauera/Development/oorep/backend/public/html/"
}
