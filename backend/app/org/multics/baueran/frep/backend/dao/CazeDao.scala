package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.{BetterString, CaseRubric, Caze}
import io.getquill.{ActionReturning, Query, Update}

class CazeDao(dbContext: db.db.DBContext) {

  import dbContext._

  private case class RawCaze(id: Int, header: String, member_id: Int, date_ : String, description: String, results: List[Int])

  private val Logger = play.api.Logger(this.getClass)
  private val tableCaze = quote { querySchema[Caze]("Caze", _.date -> "date_") }

  def insert(c: Caze): Int = {
    val cResultDao = new CazeResultDao(dbContext)

    Logger.debug("CazeDao: INSERT(): inserting case " + c.toString() + "...")

    transaction {
      // Insert the case rubrics in terms of case results and store their IDs (ultimately, as a string)
      val caseResultIds = cResultDao.insert(c)

      // Insert actual case without case result IDs
      val insert: Quoted[ActionReturning[Caze, Int]] = quote {
        tableCaze.insert(
          _.member_id -> lift(c.member_id), _.date -> lift(c.date), _.description -> lift(c.description), _.header -> lift(c.header))
          .returningGenerated(_.id)
      }
      val newId = run(insert)

      // Insert case result IDs
      val rawQuery = quote {
        (id: Int, crs: List[Int]) =>
          infix"""UPDATE caze SET results=$crs WHERE id=$id"""
            .as[Update[Caze]]
      }
      run(rawQuery(lift(newId), lift(caseResultIds)))

      Logger.debug("CazeDao: INSERT(): finished inserting case " + c.toString() + ": case Id: " + newId)

      // Return id of newly inserted case
      newId
    }
  }

  /**
    * Get cazes from DB with IDs ids.
    *
    * @param ids List of case IDs
    * @return A list of raw (as in: as stored in the DB) cases, one for each ID.
    */
  // TODO: Return not List of basic types, but List[RawCaze]!
  private def getRaw(ids: List[Int]): List[RawCaze] = {
    if (ids.length == 0)
      List()
    else {
      val rawQuery = quote {
        // Notice the '#' in front for dynamic infix queries! It is, sort of, the alternative to lift(...).
        infix"""SELECT id, header, member_id, date_, description, results FROM caze WHERE id IN (#${ids.mkString(", ")})"""
          .as[Query[RawCaze]]
      }

      run(rawQuery)
    }
  }

  def getResultIds(id: Int) = getRaw(List(id)).map(_.results).flatten

  def get(ids: List[Int]): List[Caze] = {
    if (ids.length > 0) {
      val cResultDao = new CazeResultDao(dbContext)

      getRaw(ids) match {
        case Nil => List()
        case rawCases => rawCases.map { case RawCaze(id, header, memberId, date, descr, resultIds) =>
          Caze(id, header, memberId, date, descr, cResultDao.get(resultIds))
        }
      }
    }
    else
      List()
  }

  def get(id: Int): Either[String, Caze] = {
    val cResultDao = new CazeResultDao(dbContext)

    getRaw(List(id)) match {
      case RawCaze(cId, header, memberId, date, descr, resultIds) :: Nil =>
        Right(Caze(cId, header, memberId, date, descr, cResultDao.get(resultIds)))
      case _ =>
        val errorMsg = s"CazeDao: get($id) failed."
        Logger.error(errorMsg)
        Left(errorMsg)
    }
  }

  def getMemberId(Id: Int): Either[String, Int] = {
    run(quote {
      tableCaze.filter(_.id == lift(Id)).map(_.member_id)
    }) match {
      case memberId :: Nil =>
        Right(memberId)
      case _ =>
        Left(s"getMemberId($Id) failed. No such case in DB?")
    }
  }

  /**
    * Deletes not only a case but also the reference to it in the corresponding file(s), and the case results.
    *
    * Returns the list of files to which case was associated before deletion.
    */

  def delete(id: Int) = {
    val fileDao = new FileDao(dbContext)
    val crDao = new CazeResultDao(dbContext)
    val correspondingFiles = fileDao.getFilesWithCase(id)
    var result: List[Int] = List()

    // Delete cases from file(s)
    Logger.debug(s"CazeDao: DELETE($id): removing case from file first.")
    correspondingFiles.foreach(file => fileDao.removeCaseFromFile(id, file.id))

    // Delete associated case results
    Logger.debug(s"CazeDao: DELETE($id): deleting associated case results.")
    getRaw(List(id)) match {
      case c :: Nil =>
        c.results.map(crDao.delete(_))

        // Delete case itself
        Logger.debug(s"CazeDao: DELETE($id): deleting case itself.")
        run { quote {
          tableCaze
            .filter(_.id == lift(id))
            .delete
        }}

        result = correspondingFiles.map(_.id)
      case _ =>
        Logger.error(s"CazeDao: DELETE($id) failed.")
    }

    result
  }

  /**
    * Like replace(), but does nothing if case does not ALREADY exist in DB.
    *
    * @return New case ID if case was replaced, old case ID if case was not replaced, -1 if an error occurred.
    */

  def replaceIfExists(caze: Caze) = {
    implicit def stringToString(s: String) = new BetterString(s) // For 'shorten'.
    val fileDao = new FileDao(dbContext)

    get(caze.id) match {
      case Right(foundCase) =>
        fileDao.getFilesWithCase(foundCase.id) match {
          case file :: Nil =>
            if (foundCase != caze) {
              // TODO: If we put the below into a transaction, opening of a case, and then opening of a new case works; user will have lost first case in his file!
              delete(foundCase.id)
              val insertedId = insert(caze)
              fileDao.addCaseIdToFile(insertedId, file.id)
              Logger.debug(s"CazeDao: REPLACEIFEXISTS($caze): replaced: new case ID $insertedId.")
              insertedId
            }
            else {
              Logger.debug(s"CazeDao: REPLACEIFEXISTS($caze): not replaced as case hasn't changed.")
              caze.id
            }
          case other =>
            Logger.debug(s"CazeDao: REPLACEIFEXISTS($caze): failed as no unique file found to which case belongs. List of files: $other")
            -1
        }
      case Left(_) =>
        Logger.debug(s"CazeDao: REPLACEIFEXISTS($caze): not replaced.")
        caze.id
    }
  }

  /**
    * Returns list of case result IDs that were added to the case with ID case Id,
    * or empty List on error.
    */

  def addCaseRubrics(caseId: Int, caseRubrics: List[CaseRubric]) = {
    val cResultDao = new CazeResultDao(dbContext)

    getMemberId(caseId) match {
      case Right(memberId) =>
        // Insert the case rubrics in terms of case results and store their IDs (ultimately, as a string)
        val caseResultIds = cResultDao.insert(memberId, caseRubrics)

        // Add case result ids to case
        val rawQuery = quote {
          (id: Int, crs: List[Int]) =>
            infix"""UPDATE caze SET results=results || $crs WHERE id=$id"""
              .as[Update[Caze]]
        }
        val numberOfUpdates = run(rawQuery(lift(caseId), lift(caseResultIds)))

        if (numberOfUpdates > 0)
          caseResultIds
        else {
          Logger.error(s"CazeDao: ADDCASERUBRICS($caseId, #${caseRubrics.length}) failed: DB update not done.")
          List()
        }
      case Left(err) =>
        Logger.error(s"CazeDao: ADDCASERUBRICS($caseId, #${caseRubrics.length}) failed: failed to retrieve case from DB.")
        List()
    }
  }

  /**
    * Returns number of deleted case results.
    */

  def delCaseRubrics(caseId: Int, caseRubrics: List[CaseRubric]): Int = {
    val cResultDao = new CazeResultDao(dbContext)

    getRaw(List(caseId)) match {
      case caze :: Nil =>
        val deletedCaseResultIds = cResultDao.delCaseRubrics(caseId, caseRubrics)
        val leftOverCaseResultIds: List[Int] = caze.results.toSet.filterNot(deletedCaseResultIds.toSet).asInstanceOf[Set[Int]].toList
        val rawQuery = quote {
          (id: Int, crs: List[Int]) =>
            infix"""UPDATE caze SET results=$crs WHERE id=$id"""
              .as[Update[Caze]]
        }

        if (run(rawQuery(lift(caseId), lift(leftOverCaseResultIds))) > 0)
          return deletedCaseResultIds.length
      case _ =>
        Logger.warn(s"CazeDao: DELCASERUBRICS($caseId, #${caseRubrics.length}): failed.")
    }

    0
  }

  /**
    * User defined currently label and weight.  The rest is static from repertories.
    *
    * @return Number of updated case results
    */

  def updateCaseRubricsUserDefinedValues(caseId: Int, caseRubrics: List[CaseRubric]): Int = {
    val cResultDao = new CazeResultDao(dbContext)
    val caseResults = cResultDao.getCaseResults(caseId, caseRubrics)

    caseResults.map { case caseResult =>
        caseRubrics.filter(cr => cr.rubric.id == caseResult.rubricId && cr.rubric.abbrev == caseResult.abbrev) match {
          case caseRubric :: Nil =>
            if (caseRubric.rubricWeight != caseResult.weight || caseRubric.rubricLabel.getOrElse("").reverse.reverse != caseResult.label.getOrElse("").reverse.reverse) {
              cResultDao.setWeight(caseResult.id, caseRubric.rubricWeight).toInt +
                cResultDao.setLabel(caseResult.id, caseRubric.rubricLabel).toInt
            } else {
              Logger.warn(s"CazeDao: UPDATECASERUBRICSUSERDEFINEDVALUES($caseId, #${caseRubrics.length}): skipping a case as weights and label are the same.")
              0
            }
          case _ =>
            Logger.warn(s"CazeDao: UPDATECASERUBRICSUSERDEFINEDVALUES($caseId, #${caseRubrics.length}) failed: ${caseResults.mkString(", ")}.")
            0
        }
    }.foldLeft(0)(_ + _)
  }

  def updateCaseDescription(caseId: Int, caseDescription: String): Long = {
    run(quote(tableCaze
      .filter(_.id == lift(caseId))
      .update(_.description -> lift(caseDescription))
    ))
  }

}
