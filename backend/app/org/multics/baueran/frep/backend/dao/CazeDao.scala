package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.{CaseRubric, Caze}
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
      // TODO: Add check that if c == caze, we do nothing! For this, implement Caze.equals()!
      delete(caze.header, caze.member_id)
      Logger.debug("CazeDao: replace(): Replacing " + caze.toString())
    })
    insert(c)
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

  def delete(header: String, member_id: Int) = {
    val delete = quote {
      tableCaze.filter(caze => caze.header == lift(header) && caze.member_id == lift(member_id)).delete
    }
    run(delete)
  }
}
