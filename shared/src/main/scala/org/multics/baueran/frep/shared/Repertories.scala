package org.multics.baueran.frep.shared

import scala.collection.mutable

class Repertories() {
  private val _remediesMap = mutable.HashMap[String, (InfoExtended, Remedies)]()

  def isEmpty() = _remediesMap.isEmpty

  def put(repertoryInfo: InfoExtended, repertoryRemedies: Remedies): Unit = {
    if (repertoryInfo.remedyIds.forall(repertoryRemedies.get(_) != None))
      _remediesMap.put(repertoryInfo.info.abbrev, (repertoryInfo, repertoryRemedies))
    else
      println("Repertories: put() failed due to inconsistent remedy data.")
  }

  def getRemedies(abbrev: String) = {
    _remediesMap.get(abbrev) match {
      case Some((infos, remedies)) => remedies.getAll()
      case None => List()
    }
  }

  def getInfos() = _remediesMap.values.map(_._1).toList
}
