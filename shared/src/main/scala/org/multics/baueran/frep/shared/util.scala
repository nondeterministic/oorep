package org.multics.baueran.frep.shared

import org.multics.baueran.frep.shared.Defs.SpecialLookupParams
import org.multics.baueran.frep.shared.frontend.RemedyFormat
import org.multics.baueran.frep.shared.frontend.RemedyFormat._
import scalatags.JsDom.all._

import scala.scalajs.js
import java.text.SimpleDateFormat
import java.util.Date

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
  def this() {
    this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()))
  }

  def this(javaDate: Date) {
    this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(javaDate))
  }

  override def toString() = isoDateString
}
