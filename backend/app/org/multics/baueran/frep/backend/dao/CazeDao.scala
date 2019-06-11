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
    * Like insert, but will delete old case(s) first if one or more with same header and member_id already exist.
    */

  def replace(c: Caze) = {
    val existingCases = get(c.header, c.member_id)

    existingCases.foreach(caze => {
      if (caze != c) {
        Logger.debug("CazeDao: replace(): Replacing " + caze.toString())
        val update = quote{
          tableCaze
            .filter(cc => cc.member_id == lift(caze.member_id) && cc.header == lift(caze.header))
            .update(_.description -> lift(c.description), _.results -> lift(c.results), _.date -> lift(c.date))
        }
        run(update)
      }
      else
        Logger.debug("CazeDao: replace(): NOT replacing " + caze.toString())
    })

    if (existingCases.length == 0) {
      Logger.debug("CazeDao: replace(): Inserting " + c.toString())
      insert(c)
    }
  }

  def get(header: String, member_id: Int) = {
    val select = quote {
      tableCaze.filter(caze => caze.header == lift(header) && caze.member_id == lift(member_id))
    }
    run(select)
  }

  /**
    * Get caze from DB with ID id.
    */

  def get(id: Int) = {
    val select = quote { tableCaze.filter(_.id == lift(id)) }
    run(select)
  }

  /**
    * Deletes not only a case but also the reference to it in the corresponding file(s).
    */

  def delete(id: Int, member_id: Int) = {
    val fileDao = new FileDao(dbContext)

    val files = fileDao.getFilesForMember(member_id)
    val correspondingFiles = files.filter(_.cazes.contains(id))

    transaction {
      // Delete cases from file(s)
      correspondingFiles.foreach(file =>
        fileDao.removeCaseFromFile(member_id, file.header, id))

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

  def replaceIfExists(c: Caze) = {
    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.

    if (get(c.id).length > 0) {
      replace(c)
      Logger.debug("CazeDao: caze: " + c.toString.shorten + " replaced in DB.")
    } else {
      Logger.debug("CazeDao: caze: " + c.toString.shorten + " not replaced as not in DB, yet.")
    }
  }

}
