package org.multics.baueran.frep.shared

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.{CookieFields, SpecialLookupParams}
import org.multics.baueran.frep.shared.frontend.{RemedyFormat, apiPrefix, serverUrl}
import org.multics.baueran.frep.shared.frontend.RemedyFormat._
import org.scalajs.dom
import scalatags.JsDom.all._

import java.text.SimpleDateFormat
import java.util.Date
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Success

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

  // Extract a cleaned-up string from raw input search string, if user supplied "rep:..."
  // Otherwise, if no such special terms were entered, raw input search string is used/returned.
  private def getSymptomString(submittedSymptomString: String): String = {
    for (term <- SpecialLookupParams.values)
      if (submittedSymptomString.contains(s"${term}:"))
        return getSymptomString(submittedSymptomString.replaceAll(term + """:([\w\-_]+)""", ""))

    submittedSymptomString
  }

  private val searchStrings = getSymptomString(symptom)
    .trim                                                                    // Remove trailing spaces
    .replaceAll(" +", " ")                              // Remove double spaces
    .replaceAll("[^A-Za-z0-9 äÄÜüÖöß\\-*]", "")         // Remove all but alphanum-, wildcard-, minus-symbols
    .split(" ")                                                      // Get list of search strings
    .filter(_.length > 0)
    .distinct

  // Positive and negative search terms
  def positive = searchStrings.filter(!_.startsWith("-")).toList
  def negative = searchStrings.filter(_.startsWith("-")).map(_.substring(1)).toList

  /**
    * Looks for a word, word, within some other text passage, x, where
    * x is usually either the content of fullPath, path or ttext of an
    * element of Rubric (but could be any other text as well).
    */
  def isWordInX(word: String, x: Option[String], caseSensitive: Boolean = false): Boolean = {
    x match {
      case None => false
      case Some(x) => {
        var wordMod = word
        var xMod = x

        if (!caseSensitive) {
          wordMod = word.toLowerCase
          xMod = x.toLowerCase
        }

        val searchSpace = xMod.replaceAll("[^A-Za-z0-9 äüößÄÖÜ\\-]", "").split(" ")

        if (wordMod.contains("*")) {
          // If there's no * at beginning of search term, add ^, so that "urin*" doesn't
          // match "during" (unless you want to, in which case you'd write "*urin*").
          if (!wordMod.startsWith("*"))
            wordMod = "^" + wordMod

          val searchPattern = wordMod.replaceAll("\\*", ".*").r
          searchSpace.filter(searchPattern.findFirstMatchIn(_).isDefined).length > 0
        }
        else
          searchSpace.contains(wordMod)
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

  override def toString() = isoDateString
}

object TopLevelUtilCode {

  @JSExportTopLevel("deleteCookies")
  def deleteCookies() = {
    val cookieNames = CookieFields.values.map(_.toString)
    dom.document.cookie = "PLAY_SESSION=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT"
    cookieNames.foreach(cookieName =>
      dom.document.cookie = s"${cookieName}=; path=/; expires='Thu, 01 Jan 1970 00:00:01 GMT"
    )
  }

  @JSExportTopLevel("sendAcceptCookies")
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

  @JSExportTopLevel("loadMainPageAndJumpToAnchor")
  def loadMainPageAndJumpToAnchor(anchor: String) = {
    if (dom.document.getElementById(anchor) != null) {
      dom.document.getElementById(anchor).scrollIntoView(true)
    } else {
      dom.window.location.reload(true)
      dom.window.location.assign(s"${serverUrl()}/#${anchor}")
    }
  }

  @JSExportTopLevel("toggleTheme")
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
