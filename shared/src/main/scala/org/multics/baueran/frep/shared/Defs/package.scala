package org.multics.baueran.frep.shared

package object Defs {

  object ResourceAccessLvl extends Enumeration {
    type ResourceAccessLvl = Value
    val Default = Value("Default")
    val Public = Value("Public")
    val Protected = Value("Protected")
    val Private = Value("Private")
  }
  import ResourceAccessLvl._

  def smallRepertoriesMaxSize = 550
  def maxNumberOfResultsPerPage = 100
  def maxNumberOfResultsPerMMPage = 50
  def maxLengthOfSymptoms = 200
  def maxNumberOfSymptoms = 20

  // Do not rename csrfCookie unless you know what you're doing!
  object CookieFields extends Enumeration {
    type CookieFields = Value
    val id, csrfCookie, cookiePopupAccepted, theme = Value
  }

}
