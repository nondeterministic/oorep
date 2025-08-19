package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.*
import backend.db
import shared.{BetterString, Caze, CazeRubric, CazeSubRubric}
import io.getquill.*
import io.circe.{Decoder, *}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

class CazeDao(dbContext: db.db.DBContext) {

  //  case class CazeRubric(id: Int,
  //                        cazId: Int,
  //                        subRubrics: List[CazeSubRubric],
  //                        var rubricWeight: Int,
  //                        var rubricLabel: Option[String]) {
  case class PersistentCazeRubric(id: Int, cazeId: Int, weight: Int, label: Option[String])
  object PersistentCazeRubric {
    implicit val crencoder: Encoder[PersistentCazeRubric] = deriveEncoder[PersistentCazeRubric]
    implicit val crdecoder: Decoder[PersistentCazeRubric] = deriveDecoder[PersistentCazeRubric]
  }

  //  case class Caze(id: Int,
  //                  header: String,
  //                  member_id: Int,
  //                  date: String,
  //                  changed: String,
  //                  description: String,
  //                  rubrics: List[CazeRubric] = Nil) {
  case class PersistentCaze(id: Int, header: String, member_id: Int, date: String, changed: String, description: String)
  object PersistentCaze {
    implicit val cencoder: Encoder[PersistentCaze] = deriveEncoder[PersistentCaze]
    implicit val cdecoder: Decoder[PersistentCaze] = deriveDecoder[PersistentCaze]
  }

  import dbContext._

  private val schemaCazeRubric = quote {
    querySchema[PersistentCazeRubric]("CAZERUBRIC",
      _.id -> "ID",
      _.cazeId -> "CAZEID",
      _.weight -> "WEIGHT",
      _.label -> "LABEL"
    )
  }

  private val schemaCaze = quote {
    querySchema[PersistentCaze]("CAZE",
      _.id -> "ID",
      _.header -> "HEADER",
      _.member_id -> "MEMBER_ID",
      _.date -> "DATE_",
      _.changed -> "CHANGED",
      _.description -> "DESCRIPTION"
    )
  }

  private val Logger = play.api.Logger(this.getClass)

  def delete(id: Int): Long = {
    Logger.debug(s"CazeDao: DELETE($id) called")
    run(quote(query[Caze]
      .filter(_.id == lift(id))
      .delete)
    )
  }

  def insert(caze: Caze): Int = {
    run { quote {
      schemaCaze.insert(
        _.id -> lift(caze.id),
        _.header -> lift(caze.header),
        _.member_id -> lift(caze.member_id),
        _.date -> lift(caze.date),
        _.changed -> lift(caze.changed),
        _.description -> lift(caze.description)
      ).returningGenerated(_.id)
    }}
  }

  def get(id: Int): Option[Caze] = {
    Logger.debug(s"CazeDao: get($id) called")
    run(quote(schemaCaze
      .filter(_.id == lift(id)))
    ) match {
      case pcaze :: Nil => Some(Caze(pcaze.id, pcaze.header, pcaze.member_id, pcaze.date, pcaze.changed, pcaze.description))
      case _ => None
    }
  }

  def get(case_ids: List[Int]): List[Caze] = {
    Logger.debug(s"CazeDao: get(${case_ids}) called")
    run(quote(schemaCaze
      .filter(caze => liftQuery(case_ids).contains(caze.id))
    )).map(pcaze => Caze(pcaze.id, pcaze.header, pcaze.member_id, pcaze.date, pcaze.changed, pcaze.description))
  }

  def delCaseRubrics(caseID: Int, caseRubrics: List[CazeRubric]): Int = {
    0
  }

  // TODO: Return a list of cazerubric-ids!
  def addCaseRubrics(caseID: Int, caseRubrics: List[CazeRubric]): List[Int] = {
    List()
  }

  def getCaseRubrics(caseID: Int): List[CazeRubric] = {
    run(quote(schemaCazeRubric
      .filter(_.cazeId == lift(caseID))
    )).map(pcr => CazeRubric(pcr.id, pcr.cazeId, Nil, pcr.weight, pcr.label))
  }

  def updateCaseRubricsUserDefinedValues(caseID: Int, caseRubrics: List[CazeRubric]): Int = {
    0
  }

  def updateCaseDescription(cazeI: Int, casedescription: String): Int = {
    0
  }
}
