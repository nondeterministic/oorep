package org.multics.baueran.frep.backend.repertory

import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs._
import java.io.File

import scala.collection.mutable
import scala.io.Source
import io.circe.parser._
import org.multics.baueran.frep.backend.dao.RepertoryDao
import org.multics.baueran.frep.backend.db.db.DBContext

object RepDatabase {
  private var repertories = mutable.HashMap[String, Repertory]()
  private var availableRepertoriesAbbrevs = mutable.HashSet[String]()
  private var dao: RepertoryDao = null

  /**
    * Needs to be called FIRST! It makes sure, the DB is ready
    * and has all the repertory data available for future user
    * requests.
    *
    * @param dbContext
    */
  def setup(dbContext: DBContext) = {
    dao = new RepertoryDao(dbContext)

    availableRepertories().foreach(repInfo => {
      if (dao.getInfo(repInfo.abbrev).size == 0) {
        dao.insert(repInfo)


        println(s"INFO: Inserted info for ${repInfo.abbrev} in DB.")
      }
      else
        println(s"INFO: ${repInfo.abbrev} already in DB.")
    })
  }

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
            case Right(content) => println("INFO: Info loaded: " + content.abbrev); Some(content)
            case Left(error) => println("ERROR: Info parsing of JSON failed: wrong data?" + error); None
          }
        }
        case Left(error) => println("ERROR: Info parsing failed: no JSON-input? " + error); None
      }
    }

    val folder = new File(localRepPath())
    val arrayOfFiles = folder.listFiles()
    var repInfos = mutable.Set[Info]()

    if (arrayOfFiles == null || arrayOfFiles.size == 0) {
      println(s"ERROR: Loading of repertories failed. No files in ${localRepPath()}?")
      return List.empty
    }

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
      println(s"INFO: Server: repertory $abbrev loaded.")
    }
    else
      println(s"ERROR: Failed to load repertory ${abbrev} as it is not available.")
  }

  def repertory(abbrev: String): Option[Repertory] = {
    if (availableRepertoriesAbbrevs.contains(abbrev) && !repertories.contains(abbrev))
      loadRepertory(abbrev)
    else if (!availableRepertoriesAbbrevs.contains(abbrev))
      None

    repertories.get(abbrev)
  }

  def storeRepInSQL(abbrev: String) = {
    repertory(abbrev) match {
      case Some(rep) => {
        rep.chapters
      }
      case None => println("ERROR: Error storing repertory " + abbrev)
    }

  }

}

object RepDatabase2 {
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
            case Right(content) => println("INFO: Info loaded: " + content.abbrev); Some(content)
            case Left(error) => println("ERROR: Info parsing of JSON failed: wrong data?" + error); None
          }
        }
        case Left(error) => println("ERROR: Info parsing failed: no JSON-input? " + error); None
      }
    }

    val folder = new File(localRepPath())
    val arrayOfFiles = folder.listFiles()
    var repInfos = mutable.Set[Info]()

    if (arrayOfFiles == null || arrayOfFiles.size == 0) {
      println(s"ERROR: Loading of repertories failed. No files in ${localRepPath()}?")
      return List.empty
    }

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
      println(s"INFO: Server: repertory $abbrev loaded.")
    }
    else
      println(s"ERROR: Failed to load repertory ${abbrev} as it is not available.")
  }

  def repertory(abbrev: String): Option[Repertory] = {
    if (availableRepertoriesAbbrevs.contains(abbrev) && !repertories.contains(abbrev))
      loadRepertory(abbrev)
    else if (!availableRepertoriesAbbrevs.contains(abbrev))
      None

    repertories.get(abbrev)
  }

  def storeRepInSQL(abbrev: String) = {
    repertory(abbrev) match {
      case Some(rep) => {
        rep.chapters
      }
      case None => println("ERROR: Error storing repertory " + abbrev)
    }

  }

}
