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
    val insert = quote { tableCaze.insert(lift(c)).returning(_.id) }
    run(insert)
  }

  /**
    * Like insert, but will delete the old case first if one with same ID and memberID already exists.
    */

  def replace(c: Caze) = {
    val existingCases = get(c.id, c.member_id)

    if (existingCases.length == 1) {
      if (existingCases.head != c) {
        Logger.debug("CazeDao: replace(): Replacing " + existingCases.head.toString())
        val update = quote {
          tableCaze
            .filter(cc => cc.member_id == lift(existingCases.head.member_id) && cc.id == lift(existingCases.head.id))
            .update(_.description -> lift(c.description), _.results -> lift(c.results), _.date -> lift(c.date))
        }
        Right(run(update).toInt)
      }
      else {
        Logger.debug("CazeDao: replace(): NOT replacing " + c.toString())
        Right(existingCases.head.id)
      }
    }
    else if (existingCases.length == 0) {
      Logger.debug("CazeDao: replace(): Inserting " + c.toString())
      Right(insert(c).toInt)
    }
    else {
      val errorMsg = "CazeDao: replace(): ERROR: NOT replacing " + c.toString() + " as there are more than 1 cases in the DB. This should not have happened!"
      Logger.debug(errorMsg)
      Left(errorMsg)
    }

  }

  /**
    * Get caze from DB with ID id.
    */

  def get(id: Int, member_id: Int) = {
    val select = quote { tableCaze.filter(_.id == lift(id)) }
    run(select)
  }

  /**
    * Deletes not only a case but also the reference to it in the corresponding file(s).
    */

  def delete(id: Int, member_id: Int) = {
    val fileDao = new FileDao(dbContext)

    val files = fileDao.getFilesForMember(member_id)
    val correspondingFiles = files.filter(_.cazes.filter(_.id == id).size > 0)

    transaction {
      // Delete cases from file(s)
      correspondingFiles.foreach(file => fileDao.removeCaseFromFile(member_id, file.header, id))

      // Delete case itself
      run { quote {
        tableCaze
          .filter(caze => caze.id == lift(id) && caze.member_id == lift(member_id))
          .delete
      }}
    }
  }

  /**
    * Like replace(), but does nothing if case does not ALREADY exist in DB.
    */

  def replaceIfExists(c: Caze, memberId: Int) = {
    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.

    if (get(c.id, memberId).length > 0) {
      replace(c)
      Logger.debug("CazeDao: caze: " + c.toString.shorten + " replaced in DB.")
    } else {
      Logger.debug("CazeDao: caze: " + c.toString.shorten + " not replaced as not in DB, yet.")
    }
  }

}
