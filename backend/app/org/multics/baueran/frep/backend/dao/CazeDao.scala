package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.{CaseRubric, Caze}

class CazeDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableCaze = quote { querySchema[Caze]("Caze", _.date -> "date_") }

  implicit val decodeRepAccess = MappedEncoding[String, List[CaseRubric]](
    io.circe.parser.parse(_) match {
      case Right(json) =>
        val cursor = json.hcursor
        cursor.as[List[CaseRubric]] match {
          case Right(l) => l
          case _ => List[CaseRubric]()
        }
      case _ => List[CaseRubric]()
    }
  )

  implicit val encodeListCaseRubrics = MappedEncoding[List[CaseRubric], String](
    Caze.caseRubricListEncoder(_).toString()
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
