package org.multics.baueran.frep.shared

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.{CookieFields}
import org.multics.baueran.frep.shared.frontend.{RemedyFormat, apiPrefix, serverUrl}
import org.multics.baueran.frep.shared.frontend.RemedyFormat._
import org.scalajs.dom
import scalatags.JsDom.all._

import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Success
import scala.util.matching.Regex

class BetterString(val s: String) {
  def shorten = if (s.length <= 66) s else s.substring(0,62) + "..."
  def shorten(length: Int) = if (s.length <= length) s else s.substring(0, math.abs(length - 3)) + "..."
}

class BetterCaseRubric(val cr: CaseRubric) {

  def getFormattedRemedyNames(format: RemedyFormat) = {
    cr.weightedRemedies.toList.sortBy(_.remedy.nameAbbrev).map {
      case WeightedRemedy(r, w) =>
        val remedyName = if (format == RemedyFormat.Abbreviated) r.nameAbbrev else r.nameLong

        if (w == 0) // 0-valued, e.g., repertory bogboen
          span(s"(${remedyName})")
        else if (w == 2)
          b(remedyName)
        else if (w == 3)
          b(remedyName.toUpperCase())
        else if (w == 4)
          u(b(remedyName.toUpperCase()))
        else if (w >= 5)
          b(style:="text-decoration-line: underline; text-decoration-style: double;", remedyName.toUpperCase())
        else
          span(remedyName) // 1-valued - the normal case
    }
  }

}

class SearchTerms(val symptom: String) {

  private def exactSearchStringsOnly(rawSymptom: String): List[String] = {
    val pattern = ("((?:\\-?)\"[^\"]+\")").r
    pattern.findAllIn(rawSymptom).toList  // Contains both -"strong pain" and "weak muscle"
  }

  private def unexactSearchStringsOnly(rawSymptom: String): List[String] = {
    val exactSymptoms = exactSearchStringsOnly(rawSymptom)

    def removeExactSymptoms(input: String, exactsSymptoms: List[String]): String = {
      if (exactsSymptoms.length == 0)
        input
      else
        removeExactSymptoms(input.replaceAll(Regex.quote(exactsSymptoms.head), ""), exactsSymptoms.tail)
    }

    val unexactSymptoms =
      removeExactSymptoms(rawSymptom, exactSymptoms)
        .trim                                                                    // Remove trailing spaces
        .replaceAll(",", " ")                               // Remove commas
        .replaceAll(" +", " ")                              // Remove double spaces
        .replaceAll("[^A-Za-z0-9 äÄÜüÖöß\\-\\*]", "")       // Remove all but alphanum-, wildcard-, minus-symbols
        .split(" ")                                                      // Get list of search strings
        .filter(_.length > 0)
        .distinct
        .toList

    unexactSymptoms
  }

  // Positive and negative search terms
  // (That somewhat weird filter for length > 0 in the end is to avoid the empty string creep in the search terms lists...)
  val exactPositiveOnly = exactSearchStringsOnly(symptom).filter(!_.startsWith("-")).map(_.replace("\"", "")).filter(_.trim.length > 0)
  val exactNegativeOnly = exactSearchStringsOnly(symptom).filter(_.startsWith("-")).map(_.substring(1)).map(_.replace("\"", "")).filter(_.trim.length > 0)
  val unexactPositiveOnly = unexactSearchStringsOnly(symptom).filter(!_.startsWith("-")).filter(_.trim.length > 0)
  val unexactNegativeOnly = unexactSearchStringsOnly(symptom).filter(_.startsWith("-")).map(_.substring(1)).filter(_.trim.length > 0)
  val positive = unexactPositiveOnly ::: exactPositiveOnly
  val negative = unexactNegativeOnly ::: exactNegativeOnly

  /**
    * Looks for an expression (a word or a sentence), expression, within
    * some other text passage, textPassage, where x is usually either the
    * content of fullPath, path or ttext of an element of Rubric (but could
    * be any other text as well).
    */
  def isExpressionInTextPassage(expression: String, textPassage: Option[String], caseSensitive: Boolean = false): Boolean = {
    val expressionIsExact = expression.contains(' ') // An exact search term like "gums swollen" has to have a space in it, otherwise it's a 'normal' search term

    textPassage match {
      case None => false
      case Some(textPassage) => {
        var expressionMod: String = expression.replace("\"", "") // In case of exact search term, we need to strip the ""
        var textPassageMod = textPassage

        if (caseSensitive == false) {
          expressionMod = expressionMod.toLowerCase
          textPassageMod = textPassageMod.toLowerCase
        }

        if (expressionIsExact) {
          // We need to add the word boundaries (\b) on both ends, or the exact search would always implicitly
          // behave as if a "*" was at both ends and match a lot more than it should.
          val searchPattern = ("\\b" + expressionMod + "\\b").replaceAll("\\*", "[^ ]*").r
          searchPattern.findFirstMatchIn(textPassageMod) != None
        } else {
          val searchSpace: List[String] = textPassageMod.replaceAll("[^A-Za-z0-9 äüößÄÖÜ\\-]", "").split(" ").toList

          if (expressionMod.contains("*")) {
            // If there's no * at beginning of search term, add ^, so that "urin*" doesn't
            // match "during" (unless you want to, in which case you'd write "*urin*").
            if (!expressionMod.startsWith("*"))
              expressionMod = "^" + expressionMod

            val searchPattern = expressionMod.replaceAll("\\*", ".*").r
            searchSpace.filter(searchPattern.findFirstMatchIn(_).isDefined).length > 0
          }
          else
            searchSpace.contains(expressionMod)
        }
      }
    }
  }
}

class MyDate(isoDateString: String) {
  def this() = {
    this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()))
  }

  def this(javaDate: Date) = {
    this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(javaDate))
  }

  // Compute the difference in hours between this and other.
  // If other is newer, the difference is positive, otherwise negative.

  def diff(other: MyDate) = {
    val dateThis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(isoDateString)
    val dateOther = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(other.toString())

    (dateOther.getTime - dateThis.getTime) / 1000.0 / 60.0 / 60
  }

  // Age of this date in hours

  def age() = {
    diff(new MyDate())
  }

  def toHumanReadable() = isoDateString.substring(0,10)

  override def toString() = isoDateString
}

object TopLevelUtilCode {

  def deleteCookies() = {
    val cookieNames = CookieFields.values.map(_.toString)
    dom.document.cookie = "PLAY_SESSION=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT"
    cookieNames.foreach(cookieName =>
      dom.document.cookie = s"${cookieName}=; path=/; expires='Thu, 01 Jan 1970 00:00:01 GMT"
    )
  }

  def sendAcceptCookies() = {
    HttpRequest(s"${serverUrl()}/${apiPrefix()}/store_cookie")
      .withQueryParameters("name" -> CookieFields.cookiePopupAccepted.toString, "value" -> "1")
      .send()
      .onComplete({
        case _: Success[SimpleHttpResponse] =>
          dom.document.getElementById("cookiePopup").asInstanceOf[dom.html.Div].classList.remove("show")
          dom.document.getElementById("cookiePopup").asInstanceOf[dom.html.Div].style.setProperty("display", "none")
        case _ =>
          println("Error: Cookie popup not destroyed.")
      })
  }

  def loadMainPageAndJumpToAnchor(anchor: String) = {
    if (dom.document.getElementById(anchor) != null) {
      dom.document.getElementById(anchor).scrollIntoView(true)
    } else {
      dom.window.location.reload(true)
      dom.window.location.assign(s"${serverUrl()}/#${anchor}")
    }
  }

  def toggleTheme() = {
    def storeThemeInCookie(theme: String) =
      HttpRequest(s"${serverUrl()}/${apiPrefix()}/store_cookie")
        .withQueryParameters("name" -> CookieFields.theme.toString, "value" -> theme)
        .send()

    val normaltheme = dom.document.getElementById("normaltheme").asInstanceOf[dom.html.Element]
    val darktheme = dom.document.getElementById("darktheme").asInstanceOf[dom.html.Element]

    normaltheme.getAttribute("disabled") match {
      case _:String =>
        normaltheme.removeAttribute("disabled")
        darktheme.setAttribute("disabled", "disabled")
        storeThemeInCookie("normal")
      case _ =>
        darktheme.removeAttribute("disabled")
        normaltheme.setAttribute("disabled", "disabled")
        storeThemeInCookie("dark")
    }
  }

}
