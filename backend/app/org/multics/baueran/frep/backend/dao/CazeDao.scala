package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.{CaseRubric, Caze}
import io.circe.syntax._

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
    val insert = quote { tableCaze.insert(lift(c)) }
    run(insert)
  }

  def get(header: String, member_id: Int) = {
    val select = quote {
      tableCaze.filter(caze => caze.header == lift(header) && caze.member_id == lift(member_id))
    }
    run(select)
  }

}
