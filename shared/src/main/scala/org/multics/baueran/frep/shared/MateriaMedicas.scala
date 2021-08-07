package org.multics.baueran.frep.shared

import scala.collection.mutable

case class MateriaMedicas(mmAndRemedies: List[(MMInfo, List[Remedy])]) {
  private val _mms = mutable.HashMap[String, (MMInfo, List[Remedy])]()
  mmAndRemedies.foreach{ case (mminfo, remedies) => _mms.addOne(mminfo.abbrev, (mminfo, remedies)) }

  // Get MM named 'abbrev'
  def get(abbrev: String) = {
    _mms.get(abbrev)
  }

  // Get all MMs
  def getAll() = {
    _mms.keys.map(_mms.get(_)).flatten.toList
  }

  // Get all MMs except that one named 'abbrev'
  def getAllBut(abbrev: String) = {
    _mms.keys.map(_mms.get(_)).flatten.filter{ case (mminfo, remedies) => mminfo.abbrev != abbrev }.toList
  }

  def size() = {
    _mms.size
  }
}
