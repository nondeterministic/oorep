package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.*
import backend.db
import shared.{BetterString, Caze, CazeRubric, CazeSubRubric}
import io.getquill.*
import io.circe.{Decoder, *}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

class CazeDao(dbContext: db.db.DBContext) {

  //  case class CazeRubric(id: Int,
  //                        subRubrics: List[CazeSubRubric],
  //                        var rubricWeight: Int,
  //                        var rubricLabel: Option[String]) {
  case class PersistentCazeRubric(id: Int, cazeId: Int, weight: Int, label: String)
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
    querySchema[PersistentCazeRubric]("CAZERUBRIC")
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

  // TODO: Return newly inserted caze-ID
  def insert(caze: Caze): Int = {
    0
  }

  def get(id: Int): Option[Caze] = {
//    Logger.debug(s"CazeDao: get($id) called")
//    run(quote(query[Caze]
//      .filter(_.id == lift(id)))
//    ) match {
//      case caze::Nil => Some(caze)
//      case _ => None
//    }

    Logger.debug(s"CazeDao: get($id) called")
    run(quote(schemaCaze
      .filter(_.id == lift(id)))
    ) match {
      case pcaze :: Nil => Some(Caze(pcaze.id, pcaze.header, pcaze.member_id, pcaze.date, pcaze.changed, pcaze.description))
      case _ => None
    }

  }

  def get(case_ids: List[Int]): List[Caze] = {
//    Logger.debug(s"CazeDao: get(${case_ids}) called")
//    run(quote(query[Caze]
//      .filter(caze => liftQuery(case_ids).contains(caze.id))
//    ))
    Nil
  }

  def delCaseRubrics(caseID: Int, caseRubrics: List[CazeRubric]): Int = {
    0
  }

  // TODO: Return a list of cazerubric-ids!
  def addCaseRubrics(caseID: Int, caseRubrics: List[CazeRubric]): List[Int] = {
    List()
  }

  def updateCaseRubricsUserDefinedValues(caseID: Int, caseRubrics: List[CazeRubric]): Int = {
    0
  }

  def updateCaseDescription(cazeI: Int, casedescription: String): Int = {
    0
  }
}
