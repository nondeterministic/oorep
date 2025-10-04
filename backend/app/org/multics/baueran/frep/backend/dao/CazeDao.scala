package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.*
import backend.db
import shared.{BetterString, Caze, CazeRubric, CazeSubRubric, Rubric, WeightedRemedy}
import backend.dao.RepertoryDao
import io.getquill.*
import io.circe.{Decoder, *}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import scala.annotation.targetName

class CazeDao(dbContext: db.db.DBContext) {

  // case class CazeSubRubric(id: Int, rubric: Rubric, weightedRemedies: List[WeightedRemedy]) {
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

  def delete(id: Int): Int = {
    Logger.debug(s"CazeDao: DELETE($id) called")
    run(quote(query[Caze]
      .filter(_.id == lift(id))
      .delete)
    ).toInt
  }

  def insert(caze: Caze): Int = {
    val newCazeId = run { quote {
      schemaCaze.insert(
        _.id -> lift(caze.id),
        _.header -> lift(caze.header),
        _.member_id -> lift(caze.member_id),
        _.date -> lift(caze.date),
        _.changed -> lift(caze.changed),
        _.description -> lift(caze.description)
      ).returningGenerated(_.id)
    }}

    if (addCaseRubrics(newCazeId, caze.rubrics).length == 0)
      Logger.debug(s"CazeDao: insert(...) failed to add rubrics to freshly inserted caze with ID ${newCazeId}.")

    newCazeId
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

  def getCaseRubric(caseRubricId: Int): Option[CazeRubric] = {
    run(quote(schemaCazeRubric
      .filter(_.cazeId == lift(caseRubricId))
    )) match {
      case pcr :: Nil =>
        Some(CazeRubric(
          pcr.id,
          pcr.cazeId,
          getCaseSubRubrics(pcr.id),
          pcr.weight,
          pcr.label)
        )
      case _ => None
    }
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

  // def delCaseRubric(caseRubricId: Int): Int = {
  //   run { quote {
  //     schemaCazeRubric
  //         .filter(_.id == lift(caseRubricId))
  //       .delete
  //   }}.toInt
  // }

  // Return number of deleted case sub rubrics.

  def delCaseSubRubrics(caseRubricId: Int): Int = {
    // Get case's subrubrics first.  Corresponds to
    //   select cazesubrubric.id, cazerubricid, abbrev, rubricid from cazesubrubric join cazerubric
    //          on cazerubricid = cazerubric.id and cazerubric.id = 29;
    val caseSubRubrics = run { quote {
      schemaCazeSubRubric
        .join(schemaCazeRubric).on({ case (csr, cr) => csr.cazeRubricId == cr.id && cr.id == lift(caseRubricId) })
    }}.collect { case (csr, _) => csr }

    run { quote {
      schemaCazeSubRubric
        .filter (csr => liftQuery(caseSubRubrics.map(_.id)).contains(csr.id))
        .delete
    }}.toInt
  }

  // Return number of deleted case rubrics.
  @targetName("delCaseRubrics_byObject")
  def delCaseRubrics(caseRubrics: List[CazeRubric]): Int = {
    var deletedCaseRubrics = 0

    // First attempt to delete subrubrics, then the rubrics themselves.
    transaction {
      if (caseRubrics.map(cr => delCaseSubRubrics(cr.id)).exists(_ > 0))
        deletedCaseRubrics = run { quote {
          schemaCazeRubric
            .filter(cr => liftQuery(caseRubrics.map(_.id)).contains(cr.id))
            .delete
        }}.toInt
    }

    deletedCaseRubrics
  }

  @targetName("delCaseRubrics_byID")
  def delCaseRubrics(caseRubricIds: List[Int]): Int = {
    var deletedCaseRubrics = 0

    // First attempt to delete subrubrics, then the rubrics themselves.
    transaction {
      if (caseRubricIds.map(crid => delCaseSubRubrics(crid)).exists(_ > 0))
        deletedCaseRubrics = run { quote {
          schemaCazeRubric
            .filter(cr => liftQuery(caseRubricIds).contains(cr.id))
            .delete
        }}.toInt
    }

    deletedCaseRubrics
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

  def updateCaseDescription(cazeId: Int, caseDescription: String): Int = {
    run { quote {
      schemaCaze
        .filter(_.id == lift(cazeId))
        .update(_.description -> lift(caseDescription))
    }}.toInt
  }

  // case class CazeSubRubric(id: Int, rubric: Rubric, weightedRemedies: List[WeightedRemedy]) {
  // case class PersistentCazeSubRubric(id: Int, cazeRubricId: Int, abbrev: String, rubricId: Int)

  def moveCaseSubRubric(caseSubRubric: CazeSubRubric, cazeRubricId: Int): Int = {
    run { quote {
      schemaCazeSubRubric
        .filter(_.id == lift(caseSubRubric.id))
        .update(_.cazeRubricId -> lift(cazeRubricId))
    }}.toInt
  }

  //  case class CazeRubric(id: Int,
  //                        cazeId: Int,
  //                        subRubrics: List[CazeSubRubric],
  //                        var rubricWeight: Int,
  //                        var rubricLabel: Option[String]) {

  def mergeCaseRubrics(caseRubricIds: List[Int]): Boolean = {
    if (caseRubricIds.length > 1) {
      val caseSubRubrics: List[CazeSubRubric] =
        caseRubricIds.flatMap(getCaseSubRubrics(_)) // TODO: Do we need to check for duplicates and delete them?! What if user re-adds an already added rubric?

      // Move all subrubrics into the last case rubric in caseRubricIds   &&   delete all but that last case rubric
      if ( caseSubRubrics.map(moveCaseSubRubric(_, caseRubricIds.last)).exists(_ > 0)  &&  delCaseRubrics(caseRubricIds.dropRight(1)) > 0 )
        true
      else {
        Logger.debug(s"CazeDao: mergeCaseRubrics() failed.")
        false
      }
    }
    else {
      Logger.debug(s"CazeDao: mergeCaseRubrics() needs at least two case rubrics to work.")
      false
    }
  }

}
