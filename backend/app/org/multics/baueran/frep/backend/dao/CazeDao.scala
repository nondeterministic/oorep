package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.{BetterString, CaseRubric, Caze}
import io.circe.syntax._
import play.api.Logger

class CazeDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableCaze = quote { querySchema[Caze]("Caze", _.date -> "date_") }

  implicit val decodeRepAccess = MappedEncoding[String, List[CaseRubric]](caseRubricList =>
    io.circe.parser.parse(caseRubricList) match {
      case Right(json) =>
        val cursor = json.hcursor
        cursor.as[List[CaseRubric]] match {
          case Right(l) => l
          case Left(err) => throw new IllegalArgumentException("Error decoding List[CaseRubric]: " + caseRubricList.toString() + "; " + err)
        }
      case Left(err) => throw new IllegalArgumentException("Error parsing List[CaseRubric]: " + caseRubricList.toString() + "; " + err)
    }
  )

  implicit val encodeListCaseRubrics = MappedEncoding[List[CaseRubric], String](
    l => l.map(CaseRubric.caseRubricEncoder(_)).asJson.toString()
  )

  def insert(c: Caze) = {
    val insert = quote {
      tableCaze.insert(
        _.member_id -> lift(c.member_id), _.date -> lift(c.date), _.description -> lift(c.description), _.header -> lift(c.header), _.results -> lift(c.results))
        .returning(_.id)
    }
    Logger.debug("CazeDao: insert(): inserting case " + c.toString())
    run(insert)
  }

  /**
    * Like insert, but will delete the old case first if one with same ID and memberID already exists.
    */

  def replace(c: Caze) = {
    val existingCases = get(c.id)

    existingCases match {
      case foundCase :: Nil =>
        if (foundCase != c) {
          Logger.debug("CazeDao: replace(): Replacing " + foundCase.toString())
          val update = quote {
            tableCaze
              .filter(currDBCase => currDBCase.id == lift(foundCase.id))
              .update(_.description -> lift(c.description), _.results -> lift(c.results), _.date -> lift(c.date))
          }
          run(update)
          Right(foundCase.id)
        }
        else {
          Logger.debug("CazeDao: replace(): INFO: NOT replacing " + c.toString() + " since it's equal to a stored case.")
          Right(foundCase.id)
        }
      case Nil =>
        Logger.debug("CazeDao: replace(): Inserting " + c.toString())
        Right(insert(c))
      case _ =>
        val errorMsg = "CazeDao: replace(): ERROR: NOT replacing " + c.toString() + " as there are more than 1 cases in the DB. This should not have happened!"
        Logger.debug(errorMsg)
        Left(errorMsg)
    }
  }

  /**
    * Get caze from DB with ID id.
    */

  def get(id: Int) = {
    val select = quote { tableCaze.filter(c => c.id == lift(id)) }
    run(select)
  }

  /**
    * Deletes not only a case but also the reference to it in the corresponding file(s).
    */

  def delete(id: Int) = {
    val fileDao = new FileDao(dbContext)
    val correspondingFiles = fileDao.getFilesWithCase(id)

    transaction {
      // Delete cases from file(s)
      correspondingFiles.foreach(file => fileDao.removeCaseFromFile(id, file.id))

      // Delete case itself
      run { quote {
        tableCaze
          .filter(_.id == lift(id))
          .delete
      }}
    }
  }

  /**
    * Like replace(), but does nothing if case does not ALREADY exist in DB.
    */

  def replaceIfExists(c: Caze) = {
    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.

    if (get(c.id).length > 0) {
      replace(c)
      Logger.debug("CazeDao: replaceIfExists(): " + c.toString.shorten + " replaced an old case in DB.")
    } else {
      Logger.debug("CazeDao: replaceIfExists(): " + c.toString.shorten + " not replaced for something as case not found in DB.")
    }
  }

}
