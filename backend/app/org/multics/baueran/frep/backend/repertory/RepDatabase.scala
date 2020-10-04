package org.multics.baueran.frep.backend.repertory

import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs._
import java.io.File

import scala.collection.mutable
import scala.io.Source
import io.circe.parser._
import org.multics.baueran.frep.backend.dao.RepertoryDao
import org.multics.baueran.frep.backend.db.db.DBContext
import org.multics.baueran.frep.backend.repertory.Repertory

object RepDatabase {
  private var repertories = mutable.HashMap[String, Repertory]()
  private var availableRepertoriesAbbrevs = mutable.HashSet[String]()
  private var dao: RepertoryDao = null
  private val Logger = play.api.Logger(this.getClass)

  // TODO: Is needed? implicit def repertoryToRepertory(r: Repertory) = new IoRepertory(r)

  /**
    * Needs to be called FIRST! It makes sure, the DB is ready
    * and has all the repertory data available for future user
    * requests.
    *
    * @param dbContext
    */
  def setup(dbContext: DBContext): Unit = {
    dao = new RepertoryDao(dbContext)

    def availableRepertoriesOnDisk(): List[Info] = {
      def loadInfo(abbrev: String) = {
        val lines = Source.fromFile(localRepPath() + abbrev + "_info.json").getLines.map(_.trim).mkString(" ")
        parse(lines) match {
          case Right(json) => {
            val cursor = json.hcursor
            cursor.as[Info] match {
              case Right(content) => Some(content)
              case Left(error) => Logger.error("ERROR: RepDatabase: Info parsing of JSON failed: wrong data?" + error + ", " + json); None
            }
          }
          case Left(error) => Logger.error("ERROR: RepDatabase: Info parsing failed: no JSON-input? " + error + ", " + lines); None
        }
      }

      val folder = new File(localRepPath())
      val arrayOfFiles = folder.listFiles()
      var repInfos = mutable.Set[Info]()

      if (arrayOfFiles == null || arrayOfFiles.size == 0) {
        Logger.error(s"ERROR: RepDatabase: Loading of repertories failed. No files in ${localRepPath()}?")
        return List.empty
      }

      for (file <- arrayOfFiles)
        if (!file.isDirectory && file.getName.endsWith(".json"))
          availableRepertoriesAbbrevs += file.getName.substring(0, file.getName.indexOf('_'))

      for (repAbbrev <- availableRepertoriesAbbrevs.toList) {
        loadInfo(repAbbrev) match {
          case Some(info) => repInfos += info
          case None => ;
        }
      }

      repInfos.toList
    }

    sys.env.get("OOREP_IMPORT_REPERTORIES") match {
      case Some(answer) if (answer.toUpperCase == "YES" || answer.toUpperCase == "TRUE" || answer.toUpperCase == "1") => {
        Logger.debug("Skipping repertory import as env-var OOREP_IMPORT_REPERTORIES not set to YES.")
        availableRepertoriesOnDisk().foreach(repInfo => {
          val repertory = Repertory.loadFrom(localRepPath(), repInfo.abbrev)

          if (dao.getInfo(repInfo.abbrev).size == 0) {
            Logger.debug("Inserting info into DB for " + repInfo.abbrev + ".")
            dao.insert(repInfo)
          } else {
            Logger.debug("NOT inserting info into DB for " + repInfo.abbrev + ".")
          }

          if (repertory.chapters.size > 0 && dao.getChapter(repertory.chapters.last.id).size == 0) {
            Logger.debug("Inserting chapters into DB for " + repertory.info.abbrev + ".")
            repertory.chapters.foreach(dao.insert(_))
          } else {
            Logger.debug("NOT inserting chapters into DB for " + repertory.info.abbrev + ".")
          }

          if (repertory.rubricRemedies.size > 0 && dao.getRubricRemedy(repertory.rubricRemedies.last).size == 0) {
            Logger.debug("Inserting rubric remedies into DB for " + repertory.info.abbrev + ".")
            repertory.rubricRemedies.foreach(dao.insert(_))
          } else {
            Logger.debug("NOT inserting rubric remedies into DB for " + repertory.info.abbrev + ".")
          }

          if (repertory.remedies.size > 0 && dao.getRemedy(repertory.remedies.last.abbrev, repertory.remedies.last.id).size == 0) {
            Logger.debug("Inserting remedies into DB for " + repertory.info.abbrev + ".")
            repertory.remedies.foreach(dao.insert(_))
          } else {
            Logger.debug("NOT inserting remedies into DB for " + repertory.info.abbrev + ".")
          }

          if (repertory.chapterRemedies.size > 0 && dao.getChapterRemedy(repertory.chapterRemedies.last).size == 0) {
            Logger.debug("Inserting chapter remedies into DB for " + repertory.info.abbrev + ".")
            repertory.chapterRemedies.foreach(dao.insert(_))
          } else {
            Logger.debug("NOT inserting chapter remedies into DB for " + repertory.info.abbrev + ".")
          }

          if (repertory.rubrics.size > 0 && dao.getRubric(repertory.rubrics.last.abbrev, repertory.rubrics.last.id).size == 0) {
            Logger.debug("Inserting rubrics into DB for " + repertory.info.abbrev + ".")
            repertory.rubrics.foreach(dao.insert(_))
          } else {
            Logger.debug("NOT inserting rubrics into DB for " + repertory.info.abbrev + ".")
          }

        })
      }
      case None =>
        Logger.debug("Skipping repertory import as env-var OOREP_IMPORT_REPERTORIES not set to YES.")
    }

    Logger.info("DB setup complete.")
  }

//  @deprecated("Only used for testing.","24-02-2019")
//  def repertory(abbrev: String): Option[Repertory] = {
//    def loadAndPutRepertory(abbrev: String) = {
//      if (availableRepertoriesAbbrevs.contains(abbrev)) {
//        repertories.put(abbrev, Repertory.loadFrom(localRepPath(), abbrev))
//        Logger.debug(s"INFO: RepDatabase: Server: repertory $abbrev loaded.")
//      }
//      else
//        Logger.debug(s"ERROR: RepDatabase: Failed to load repertory ${abbrev} as it is not available.")
//    }
//
//    if (availableRepertoriesAbbrevs.contains(abbrev) && !repertories.contains(abbrev))
//      loadAndPutRepertory(abbrev)
//    else if (!availableRepertoriesAbbrevs.contains(abbrev))
//      None
//
//    repertories.get(abbrev)
//  }

}
