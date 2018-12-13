package org.multics.baueran.frep.shared

import scalatags.JsDom.all._

class BetterString(val s: String) {
  def shorten = if (s.length <= 66) s else s.substring(0,62) + "..."
}

class BetterCaseRubric(val cr: CaseRubric) {

  def getFormattedRemedies() = {
    cr.weightedRemedies.toList.sortBy(_._1.nameAbbrev).map {
      case (r, w) =>
        if (w == 2)
          b(r.nameAbbrev)
        else if (w == 3)
          b(r.nameAbbrev.toUpperCase())
        else if (w >= 4)
          u(b(r.nameAbbrev.toUpperCase()))
        else
          span(r.nameAbbrev)
    }
  }

  def getRawRemedies() = {
    cr.weightedRemedies.toList.sortBy(_._1.nameAbbrev).map {
      case (r, w) =>
        if (w > 1)
          span(r.nameAbbrev + " (" + w.toString() + ")")
        else
          span(r.nameAbbrev)
    }
  }

}
