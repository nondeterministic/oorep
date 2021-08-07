package org.multics.baueran.frep.backend.dao

import scala.collection.mutable.HashMap
import org.multics.baueran.frep.backend.db
import org.multics.baueran.frep.shared.{CaseRubric, Caze, CazeResult, Remedy, Rubric, RubricRemedy, WeightedRemedy}

class CazeResultDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val Logger = play.api.Logger(this.getClass)

  // The id of -1 is obviously bogus, and insert() needs to ignore it and let the DB count it up!
  private def cazeToCazeResults(c: Caze): List[CazeResult] = {
    c.results.map(cr => CazeResult(-1, c.member_id, cr.rubric.abbrev, cr.rubric.id, cr.rubricWeight, cr.rubricLabel))
  }

  // Do not insert id, it is a serial number!
  def insert(c: CazeResult): Int = {
    val insert = quote {
      query[CazeResult].insert(
        _.weight -> lift(c.weight),
        _.rubricId -> lift(c.rubricId),
        _.abbrev -> lift(c.abbrev),
        _.member_id -> lift(c.member_id),
        _.label -> lift(c.label)
      ).returningGenerated(_.id)
    }
    val result = run(insert)
    Logger.debug("CazeResultDao: INSERT(): inserted " + c.toString() + " with result " + result)
    result
  }

  /**
    * Inserts a cases case rubrics in terms of case results into DB.
    *
    * Returns list of inserted IDs.
    */

  def insert(c: Caze): List[Int] = {
    Logger.debug("CazeResultDao: INSERT(): inserting results of " + c.toString() + "...")
    cazeToCazeResults(c).map(insert(_))
  }

  /**
    * Inserts list of case rubrics in terms of case results into DB.
    *
    * Returns list of inserted IDs.
    */

  def insert(memberId: Int, crs: List[CaseRubric]): List[Int] = {
    Logger.debug(s"CazeResultDao: INSERT($memberId, #${crs.length}): inserting case rubrics.")
    crs.map(cr => CazeResult(-1, memberId, cr.rubric.abbrev, cr.rubric.id, cr.rubricWeight, cr.rubricLabel))
      .map(insert(_))
  }

  def delete(id: Int) = {
    Logger.debug(s"CazeResultDao: DELETE($id) called")
    run(quote(query[CazeResult]
      .filter(_.id == lift(id))
      .delete)
    )
  }

  def delete(ids: List[Int]) = {
    Logger.debug(s"CazeResultDao: DELETE($ids) called")
    run(quote(query[CazeResult]
      .filter(cr => liftQuery(ids).contains(cr.id))
      .delete)
    )
  }

  def setWeight(id: Int, weight: Int): Long = {
    Logger.debug(s"CazeResultDao: SETWEIGHT($id, $weight) called")
    run(quote(query[CazeResult]
      .filter(_.id == lift(id))
      .update(_.weight -> lift(weight)))
    )
  }

  def setLabel(id: Int, label: Option[String]) = {
    Logger.debug(s"CazeResultDao: SETLABEL($id, ${label.toString}) called")
    run(quote(query[CazeResult]
      .filter(_.id == lift(id))
      .update(_.label -> lift(label)))
    )
  }

  /**
    * Kinda like
    *
    * SELECT nameabbrev FROM remedy JOIN rubricremedy ON remedy.abbrev=rubricremedy.abbrev AND remedy.id=rubricremedy.remedyid
    *                               JOIN cazeresult ON rubricremedy.rubricid=cazeresult.rubricid AND rubricremedy.abbrev=cazeresult.abbrev
    *                                                 AND cazeresult.rubricid=61832 AND cazeresult.abbrev='publicum';
    *
    * I said: kinda!
    */

  def get(caseResultIds: List[Int]): List[CaseRubric] = {
    val dbResults = run {
      quote {
        for {
          caseResults    <- query[CazeResult].filter(cr => liftQuery(caseResultIds).contains(cr.id))
          rubricRemedies <- query[RubricRemedy].filter(rubricR => rubricR.abbrev == caseResults.abbrev && rubricR.rubricId == caseResults.rubricId)
          rubrics        <- query[Rubric].join(rubric => rubric.abbrev == rubricRemedies.abbrev && rubric.id == rubricRemedies.rubricId)
          remedies       <- query[Remedy].join(rem => rem.id == rubricRemedies.remedyId)
        } yield (remedies, rubricRemedies, rubrics, caseResults)
      }
    }

    val weightedRemedies = HashMap.empty[CazeResult, List[WeightedRemedy]]
    dbResults.map { case (remedy, rubricRemedy, _, caseResult) =>
      if (weightedRemedies.contains(caseResult))
        weightedRemedies.put(caseResult, WeightedRemedy(remedy, rubricRemedy.weight) :: weightedRemedies.get(caseResult).get)
      else
        weightedRemedies.put(caseResult, List(WeightedRemedy(remedy, rubricRemedy.weight)))
    }

    val caseRubrics = dbResults.map { case (_, rubricRemedy, rubric, caseResult) =>
      CaseRubric(rubric, rubric.abbrev, s"${caseResult.weight}".toInt,
        { caseResult.label match {
          case Some(l) => Some(l.reverse.reverse)
          case _ => None
        }},
        weightedRemedies.get(caseResult).getOrElse(List.empty))
    }.distinct

    caseRubrics
  }

  def getCaseResults(caseId: Int, caseRubrics: List[CaseRubric]): List[CazeResult] = {
    val cazeDao = new CazeDao(dbContext)
    val caseResultIds = cazeDao.getResultIds(caseId)

    run { quote { query[CazeResult]
      .filter(cr => liftQuery(caseRubrics.map(_.repertoryAbbrev)).contains(cr.abbrev) &&
        liftQuery(caseRubrics.map(_.rubric.id)).contains(cr.rubricId) &&
        liftQuery(caseResultIds).contains(cr.id)
      )
    }}
  }

  /**
    * Deletes the case results corresponding to the case rubrics argument.
    *
    * Does **NOT** update the associated case!
    *
    * Returns list of IDs of the deleted case results,
    * or empty when error occurred.
    */

  def delCaseRubrics(caseId: Int, caseRubrics: List[CaseRubric]): List[Int] = {
    val caseResults = getCaseResults(caseId, caseRubrics)

    if (caseResults.length > 0) {
      run {
        quote {
          query[CazeResult]
            .filter(cr => liftQuery(caseResults.map(_.id)).contains(cr.id))
            .delete
        }
      }

      return caseResults.map(_.id)
    }

    List()
  }

}
