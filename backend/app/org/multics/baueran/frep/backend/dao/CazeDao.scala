package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.*
import backend.db
import shared.{BetterString, Caze, CazeRubric, CazeSubRubric, Rubric, WeightedRemedy}
import backend.dao.RepertoryDao
import io.getquill.*
import io.circe.{Decoder, *}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

class CazeDao(dbContext: db.db.DBContext) {

  //  case class CazeRubric(id: Int,
  //                        cazeId: Int,
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

  // case class CazeSubRubric(id: Int, rubric: Rubric, weightedRemedies: List[WeightedRemedy]) {
  case class PersistentCazeSubRubric(id: Int, cazeRubricId: Int, abbrev: String, rubricId: Int)
  object PersistentCazeSubRubric {
    implicit val csrencoder: Encoder[PersistentCazeSubRubric] = deriveEncoder[PersistentCazeSubRubric]
    implicit val csrdecoder: Decoder[PersistentCazeSubRubric] = deriveDecoder[PersistentCazeSubRubric]
  }

  import dbContext._

  val repertoryDao = RepertoryDao(dbContext)

  private val schemaCazeSubRubric = quote {
    querySchema[PersistentCazeSubRubric]("CAZESUBRUBRIC",
      _.id -> "ID",
      _.cazeRubricId -> "CAZERUBRICID",
      _.abbrev -> "ABBREV",
      _.rubricId -> "RUBRICID"
    )
  }

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

  def getWeightedRemedies(rubric: Rubric): List[WeightedRemedy] = {
    repertoryDao.getRubricRemedies(rubric.id, rubric.abbrev) match {
      case Nil => Nil
      case rubricRemedies => rubricRemedies.collect(rubricRemedy =>
        repertoryDao.getRemedy(rubricRemedy.remedyId) match {
          case Some(remedy) => WeightedRemedy(remedy, rubricRemedy.weight)
        }
      )
    }
  }

  // case class CazeSubRubric(id: Int, rubric: Rubric, weightedRemedies: List[WeightedRemedy]) {
  // case class PersistentCazeSubRubric(id: Int, cazeRubricId: Int, abbrev: String, rubricId: Int)

  def getCaseSubRubric(caseSubRubricId: Int): Option[CazeSubRubric] = {
    run(quote(schemaCazeSubRubric
      .filter(_.id == lift(caseSubRubricId))
    )) match {
      case pcsr :: Nil =>
        repertoryDao.getRubric(pcsr.rubricId, pcsr.abbrev) match {
          case Some(rubric) => 
            Some(CazeSubRubric(pcsr.id, rubric, getWeightedRemedies(rubric)))
          case None =>
            None
        }
      case _ => None
    }
  }

  def getCaseSubRubrics(caseRubricId: Int): List[CazeSubRubric] = {
    run(quote(schemaCazeSubRubric
      .filter(_.cazeRubricId == lift(caseRubricId))
    )) match {
      case Nil => Nil
      case pcsrs =>
        pcsrs.collect(pcsr =>
          repertoryDao.getRubric(pcsr.rubricId, pcsr.abbrev) match {
            case Some(rubric) =>
              CazeSubRubric(pcsr.id, rubric, getWeightedRemedies(rubric))
          }
        )
    }
  }

  // def getCaseSubRubrics(caseRubricId: Int): List[CazeSubRubric] = {
  //   Nil
  // }

  //  case class CazeRubric(id: Int,
  //                        cazeId: Int,
  //                        subRubrics: List[CazeSubRubric],
  //                        var rubricWeight: Int,
  //                        var rubricLabel: Option[String]) {

  def getCaseRubrics(caseID: Int): List[CazeRubric] = {
    run(quote(schemaCazeRubric
      .filter(_.cazeId == lift(caseID))
    )).map(pcr =>
      CazeRubric(
        pcr.id,
        pcr.cazeId,
        getCaseSubRubrics(pcr.id),
        pcr.weight,
        pcr.label)
    )
  }

  // If we get only ONE case, we're likely interested in the rubrics, too.
  // So, we pull the rubrics, too.

  def get(id: Int): Option[Caze] = {
    Logger.debug(s"CazeDao: get($id) called")
    run(quote(schemaCaze
      .filter(_.id == lift(id)))
    ) match {
      case pcaze :: Nil =>
        val caseRubrics = getCaseRubrics(pcaze.id)
        Some(Caze(pcaze.id, pcaze.header, pcaze.member_id, pcaze.date, pcaze.changed, pcaze.description, caseRubrics))
      case _ => None
    }
  }

  // If we get MULTIPLE cases, we're most likely NOT interested in their rubrics.
  // So, we don't get them, too.

  def get(case_ids: List[Int]): List[Caze] = {
    Logger.debug(s"CazeDao: get(${case_ids}) called")
    run(quote(schemaCaze
      .filter(caze => liftQuery(case_ids).contains(caze.id))
    )).map(pcaze => Caze(pcaze.id, pcaze.header, pcaze.member_id, pcaze.date, pcaze.changed, pcaze.description))
  }

  def delCaseRubrics(caseID: Int, caseRubrics: List[CazeRubric]): Int = {
    0
  }

  def addCaseSubRubrics(caseRubricId: Int, caseSubRubrics: List[CazeSubRubric]): List[Int] = {
    caseSubRubrics.map(csr =>
      run { quote {
        schemaCazeSubRubric.insert(
          _.cazeRubricId -> lift(caseRubricId),
          _.abbrev -> lift(csr.rubric.abbrev),
          _.rubricId -> lift(csr.rubric.id)
        ).returningGenerated(_.id)
      }}
    )
  }

  def addCaseRubrics(caseID: Int, caseRubrics: List[CazeRubric]): List[Int] = {
    caseRubrics.map(cr =>
      val newCaseRubricId = run { quote {
        schemaCazeRubric.insert(
          _.cazeId -> lift(caseID),
          _.weight -> lift(cr.rubricWeight),
          _.label -> lift(cr.rubricLabel)
        ).returningGenerated(_.id)
      }}

      addCaseSubRubrics(newCaseRubricId, cr.subRubrics)
      newCaseRubricId
    )
  }

  def updateCaseSubRubric(caseRubricId: Int, caseSubRubric: CazeSubRubric): Int = {
    run { quote {
      schemaCazeSubRubric
        .filter(_.id == lift(caseSubRubric.id))
        .update(
          _.cazeRubricId -> lift(caseSubRubric.id),
          _.abbrev -> lift(caseSubRubric.rubric.abbrev),
          _.rubricId -> lift(caseSubRubric.rubric.id)
        )
    }}.toInt
  }

  def updateCaseRubricUserDefinedValues(caseRubric: CazeRubric): Int = {
    run { quote {
      schemaCazeRubric
        .filter(_.id == lift(caseRubric.id))
        .update(
          _.cazeId -> lift(caseRubric.cazeId),
          _.weight -> lift(caseRubric.rubricWeight),
          _.label -> lift(caseRubric.rubricLabel)
        )
    }}.toInt
  }

  def updateCaseRubricsUserDefinedValues(caseRubrics: List[CazeRubric]): Int = {
    caseRubrics.map(updateCaseRubricUserDefinedValues(_)).length
  }

  def updateCaseDescription(cazeI: Int, casedescription: String): Int = {
    0
  }

}
