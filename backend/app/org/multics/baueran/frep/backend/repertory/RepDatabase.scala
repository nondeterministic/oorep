package org.multics.baueran.frep.backend.repertory

import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs._

import java.io.{ File }
import scala.collection.mutable
import scala.io.Source
import io.circe.parser._

object RepDatabase {
  private var repertories = mutable.HashMap[String, Repertory]()
  private var availableRepertoriesAbbrevs = mutable.HashSet[String]()

  /**
    * @return list of repertory abbreviations in repertory directory.
    */
  def availableRepertories(): List[Info] = {
    def loadInfo(abbrev: String) = {
      val lines = Source.fromFile(localRepPath() + abbrev + "_info.json").getLines.map(_.trim).mkString(" ")
      parse(lines) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[Info] match {
            case Right(content) => println("Info loaded: " + content.abbrev); Some(content)
            case Left(error) => println("Info parsing of JSON failed: wrong data?" + error); None
          }
        }
        case Left(error) => println("Info parsing failed: no JSON-input? " + error); None
      }
    }

    val folder = new File(localRepPath())
    val arrayOfFiles = folder.listFiles()
    var repInfos = mutable.Set[Info]()

    for (file <- arrayOfFiles)
      if (!file.isDirectory)
        availableRepertoriesAbbrevs += file.getName.substring(0, file.getName.indexOf('_'))

    for (repAbbrev <- availableRepertoriesAbbrevs.toList) {
      loadInfo(repAbbrev) match {
        case Some(info) => repInfos += info
        case None => ;
      }
    }

    repInfos.toList
  }

  def loadedRepertories(): List[String] = repertories.keys.toList

  def loadRepertory(abbrev: String) = {
    if (availableRepertoriesAbbrevs.contains(abbrev)) {
      repertories.put(abbrev, Repertory.loadFrom(localRepPath(), abbrev))
      println(s"Server: repertory $abbrev loaded.")
    }
  }

  def repertory(abbrev: String): Option[Repertory] = {
    if (availableRepertoriesAbbrevs.contains(abbrev) && !repertories.contains(abbrev))
      loadRepertory(abbrev)
    else if (!availableRepertoriesAbbrevs.contains(abbrev))
      None

    repertories.get(abbrev)
  }
}
