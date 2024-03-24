package org.multics.baueran.frep.shared

import scala.collection.mutable
import scala.util.boundary, boundary.break

case class RemedyEntered(id: Option[Int], fullname: Option[String])

class Remedies(remedies: List[Remedy]) {
  private val _remediesMap = mutable.HashMap[Int, Remedy]()

  remedies.foreach(remedy => _remediesMap.addOne((remedy.id, remedy)))

  def isEmpty() = _remediesMap.isEmpty

  def get(remedyId: Int) = _remediesMap.get(remedyId)

  def getAll() = _remediesMap.values.toList

  private def doSearch(enteredRemedyString: String): RemedyEntered = {
    val enteredRemedyStringLower = enteredRemedyString.trim.toLowerCase.replace("  ", " ")

    if (enteredRemedyStringLower.length < 3) {
      return RemedyEntered(None, None)
    }

    // Is there an exact abbreviation match?
    _remediesMap.values.filter(_.nameAbbrev.toLowerCase.replaceAll("[^A-Za-z\\-]", "") == enteredRemedyStringLower.replaceAll("[^A-Za-z\\-]", "")) match {
      case remedy :: Nil => return RemedyEntered(Some(remedy.id), Some(remedy.nameLong))
      case _ => ;
    }

    // Is there a remedy-fullname match?
    _remediesMap.values.find(_.nameLong.toLowerCase == enteredRemedyStringLower) match {
      case Some(remedy) => return RemedyEntered(Some(remedy.id), None)
      case None => ;
    }

    // Is there a remedy-altname match?
    _remediesMap.values.foreach{ remedy =>
      boundary:
        if (remedy.namealt.exists(_.toLowerCase == enteredRemedyStringLower))
          break(RemedyEntered(Some(remedy.id), None))
    }

    // Is there a remedy-fullname partial match?
    if (_remediesMap.values.exists(_.nameLong.toLowerCase.startsWith(enteredRemedyStringLower))) {
      return RemedyEntered(None, Some(enteredRemedyStringLower.trim.replace("  ", " ")))
    }

    // Is there a remedy-altname partial match?
    _remediesMap.values.foreach{ remedy =>
      boundary:
        if (remedy.namealt.exists(_.toLowerCase.startsWith(enteredRemedyStringLower)))
          break(RemedyEntered(None, Some(enteredRemedyStringLower.trim.replace("  ", " "))))
    }

    // No match found :-(
    RemedyEntered(None, None)
  }

  /**
    * String can look like "Arsenicum Album (Ars.) [AltName1, AltName2 & AltName3]"
    * or like "Arsenicum", "Ars.", "Ars", "AltName1", "Altname2 & Altname3", etc.
    *
    * We either match it straight to a remedy in which case we return its ID,
    * or we find remedies that begin with @enteredRemedyString (or whose altnames
    * do), in which case we return the (beginning of the) fullname of the remedy
    * that matches.
    *
    * @param enteredRemedyString
    * @return
    */

  def getRemedyEntered(enteredRemedyString: String): RemedyEntered = {
    val enteredRemedyStringLower = enteredRemedyString.trim.toLowerCase.replace("  ", " ")

    if (enteredRemedyStringLower.length < 3)
      return RemedyEntered(None, None)

    // An abbreviation in brackets was entered, e.g. "(Nat-s.)"
    if (enteredRemedyStringLower.contains("(") || enteredRemedyStringLower.contains(")")) {
      val pattern = (".+\\(([^\\)]+)\\).*").r
      enteredRemedyStringLower match {
        case pattern(abbrev) =>
          doSearch(abbrev) match {
            case RemedyEntered(None, None) => ;
            case RemedyEntered(a,b) => return RemedyEntered(a,b)
          }
        case _ => ;
      }
    }

    // Altname(s) in brackets were entered, e.g., "[AltName1, AltName2 & AltName3]"
    if (enteredRemedyStringLower.contains("[") || enteredRemedyStringLower.contains("]")) {
      val pattern = (".+\\[([^\\]]+)\\].*").r
      boundary:
        enteredRemedyStringLower match {
          case pattern(altnames) =>
            altnames.split(',').map(_.trim).map(doSearch(_)).foreach{ case RemedyEntered(id, fullname) =>
              if (id != None || fullname != None)
                break(RemedyEntered(id, fullname))
            }
          case _ => ;
        }
    }

    // No altnames or abbrev recognisable in input, try matching the whole entered string...
    doSearch(enteredRemedyStringLower)
  }

}
