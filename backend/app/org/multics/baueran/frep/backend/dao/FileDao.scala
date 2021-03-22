package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import io.getquill.Update
import shared.{Caze, FIle, dbFile}

class FileDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val Logger = play.api.Logger(this.getClass)
  private val tableFile = quote {
    querySchema[dbFile]("File", _.date -> "date_")
  }

  def getFIle(id: Int) = {
    try {
      Logger.debug(s"FileDao: getFIle(${id}): getting file with id ${id}")
      run ( quote {
        tableFile.filter(_.id == lift(id))
      }).map { case dbFile =>
        val cazeDao = new CazeDao(dbContext)
        val cazes: List[Caze] = cazeDao.get(dbFile.case_ids)
        FIle(Some(dbFile.id), dbFile.header, dbFile.member_id, dbFile.date, dbFile.description, cazes)
      }
    } catch {
      case exception: Throwable =>
        Logger.warn(s"FileDao: getFIle(${id}) == List(): ${exception.getStackTrace.map(_.toString).mkString("\n")}")
        List()
    }
  }

  def getDbFilesForMember(member_id: Int): List[dbFile] = {
    try {
      Logger.debug(s"FileDao: getDbFilesForMember($member_id)")
      run ( quote {
        tableFile.filter(_.member_id == lift(member_id))
      })
    } catch {
      case exception: Throwable =>
        Logger.warn(s"FileDao: getDbFilesForMember($member_id) == List(): ${exception.getStackTrace.map(_.toString).mkString("\n")}")
        List()
    }
  }

  def getCasesFromFile(fileId: String): List[Caze] = {
    if (fileId.forall(_.isDigit)) {
      getFIle(fileId.toInt) match {
        case file :: Nil => file.cazes
        case _ => List()
      }
    }
    else {
      Logger.warn(s"FileDao: getCasesFromFile($fileId) failed.")
      List()
    }
  }

  /**
    * Return files which contain a case with case-ID caseId.
    * (Should only ever be only really, i.e., list of length 1.)
    */

  def getFilesWithCase(caseId: Int): List[dbFile] = {
    run ( quote {
      tableFile.filter(_.case_ids.contains(lift(caseId)))
    }) // UNCOMMENT, if FIles should be returned instead of dbFiles: .map(dbFileToFIle(_)).map { case Some(file) => file }
  }

  private def fileToDBFile(file: FIle): dbFile = {
    dbFile(file.dbId.getOrElse(-1), file.header, file.member_id, file.date, file.description, file.cazes.map(_.id))
  }

  def insert(f: FIle): Int = {
    val newDBFile: dbFile = fileToDBFile(f)

    run { quote {
      // Do not add id values, as id auto-increments:
      tableFile.insert(
        _.header -> lift(newDBFile.header),
        _.member_id -> lift(newDBFile.member_id),
        _.date -> lift(newDBFile.date),
        _.description -> lift(newDBFile.description),
        _.case_ids -> lift(newDBFile.case_ids))
        .returningGenerated(_.id)
    }}
  }

  /**
    * Note: Does not create a case, if case doesn't already exist.
    *
    * @return True on success, false otherwise. Case needs to be already existing in DB!
    */

  def addCaseIdToFile(cazeId: Int, fileId: Int): Boolean = {
    val cazeDao = new CazeDao(dbContext)

    transaction {
      cazeDao.get(cazeId) match {
        case Right(foundCase) =>
          // Delete case ID from all files first, as a case can only have one parent file...
          for (file <- getFilesWithCase(foundCase.id)) {
            run(quote {
              tableFile
                .filter(_.id == lift(file.id))
                .update(f => f.case_ids -> lift(file.case_ids.filter(_ != foundCase.id)))
            })
          }

          // Add case id to file
          val rawQuery = quote {
            (localId: Int, localCazeId: Int) =>
              infix"""UPDATE file SET case_ids=case_ids || $localCazeId WHERE id=$localId"""
                .as[Update[Caze]]
          }
          if (run(rawQuery(lift(fileId), lift(foundCase.id))) > 0) {
            Logger.debug(s"FileDao: ADDCASEIDTOFILE($cazeId, $fileId) succeeded.")
            return true
          }
        case _ => ;
      }
    }

    Logger.error(s"FileDao: ADDCASEIDTOFILE($cazeId, $fileId) failed.")
    false
  }

  /**
    * According to Java docs, the returning result set here is a Long, n, that indicates
    * which row the cursor is at in the DB after the operation.  (Pretty useless, if you
    * ask me.)
    */

  def removeCaseFromFile(caseId: Int, fileId: Int) = {
    transaction {
      run {
        quote {
          tableFile
            .filter(_.id == lift(fileId))
        }
      } match {
        case f :: Nil => {
          val newCaseIds = f.case_ids.filter(_ != caseId)
          Right(
            run(quote(
              tableFile
                .filter(file => file.id == lift(f.id) && file.member_id == lift(f.member_id))
                .update(_.case_ids -> lift(newCaseIds)))).toInt
          )
        }
        case _ => Left(s"FileDao: REMOVECASEFROMFILE() failed for fileId ${fileId}, caseId ${caseId}")
      }
    }
  }

  def delFileAndAllCases(fileId: Int) = {
    val cazeIds = run {
      quote {
        tableFile
          .filter(_.id == lift(fileId))
      }
    }.flatMap(_.case_ids)

    val cazeDao = new CazeDao(dbContext)

    transaction {
      cazeIds.foreach(cazeDao.delete(_))
      Logger.debug(s"FileDao: DELFILE($fileId).")

      run {
        quote {
          tableFile
            .filter(_.id == lift(fileId))
            .delete
        }
      }
    }
  }

  def changeDescription(fileId: Int, newDescription: String): Long = {
    val changedRows = run { quote {
      tableFile
        .filter(_.id == lift(fileId))
        .update(_.description -> lift(newDescription))
    }}
    changedRows
  }

}
