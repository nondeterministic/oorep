package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.{Caze, FIle}
import play.api.Logger

case class dbFile(id: Int,
                  header: String,
                  member_id: Int,
                  date: String,
                  description: String,
                  case_ids: List[Int])

class FileDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableFile = quote {
    querySchema[dbFile]("File", _.date -> "date_")
  }

  private def getRaw(id: Int) = {
    val select = quote {
      tableFile.filter(_.id == lift(id))
    }
    run(select)
  }

  def get(id: Int) = {
    try {
      Logger.info(s"FileDao: get(): getting file with id ${id}")
      run ( quote {
        tableFile.filter(_.id == lift(id))
      }).map(dbFileToFIle(_)).map { case Some(file) => file }
    } catch {
      case exception: Throwable =>
        Logger.warn(s"FileDao: get(): get(${id}) == {}: ${exception}")
        List()
    }
  }

  def getFilesForMember(member_id: Int) = {
    try {
      Logger.info(s"FileDao: getFilesForMember(): getting files for member with id ${member_id}")
      run ( quote {
        tableFile.filter(_.member_id == lift(member_id))
      }).map(dbFileToFIle(_)).map { case Some(file) => file }
    } catch {
      case exception: Throwable =>
        Logger.warn(s"FileDao: getFilesForMember(): getFilesForMember(${member_id}) == {}: ${exception}")
        List()
    }
  }

  def getCasesFromFile(fileId: String): List[Caze] = {
    if (fileId.forall(_.isDigit)) {
      get(fileId.toInt) match {
        case file :: Nil => file.cazes
        case _ => List()
      }
    }
    else {
      Logger.warn(s"FileDao: getCasesFromFile() failed: fileId == ${fileId}")
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

  /**
    * A dbFile can only be converted to a FIle by looking up the case IDs in the DB.
    * Hence, this method is rather inefficient and should be AVOIDED if possible!
    */

  private def dbFileToFIle(file: dbFile): Option[FIle] = {
    getRaw(file.id) match {
      case singleDbFile :: Nil => {
        val cazeDao = new CazeDao(dbContext)
        val cazes = singleDbFile.case_ids.map(cazeDao.get(_)).flatten
        Some(FIle(Some(file.id), singleDbFile.header, singleDbFile.member_id, singleDbFile.date, singleDbFile.description, cazes))
      }
      case _ => {
        Logger.warn(s"FileDao: dbFileToFile(${file.toString()}) failed: returned None; file not in DB?")
        None
      }
    }
  }

  private def fileToDBFile(file: FIle): Option[dbFile] = {
    if (file.cazes.length > 0) {
      val cazeDao = new CazeDao(dbContext)
      val cazeIds =
        file.cazes
          .map(caze => cazeDao.get(caze.id)).flatten
          .map(_.id)

      if (cazeIds.length > 0)
        Some(dbFile(file.dbId.getOrElse(0), file.header, file.member_id, file.date, file.description, cazeIds))
      else
        None
    }
    else
      Some(dbFile(file.dbId.getOrElse(0), file.header, file.member_id, file.date, file.description, List.empty))
  }

  def insert(f: FIle): Either[String, Int] = {
    fileToDBFile(f) match {
      case Some(newDBFile) =>
        Right(run { quote {
          // Do not add id values, as id auto-increments:
          tableFile.insert(
            _.header -> lift(newDBFile.header),
            _.member_id -> lift(newDBFile.member_id),
            _.date -> lift(newDBFile.date),
            _.description -> lift(newDBFile.description),
            _.case_ids -> lift(newDBFile.case_ids))
            .returning(_.id)
        }})
      case _ =>
        val err = "FileDao: insert() failed. Failed to convert FIle " + f.toString()
        Logger.error(err)
        Left(err)
    }
  }

  /**
    * @return Case-ID as stored in the database for the (new) caze.
    */

  def addCaseToFile(caze: Caze, fileId: Int) = {
    val cazeDao = new CazeDao(dbContext)
    Logger.debug(s"FileDao: addCaseToFile(): about to add case ${caze} to file with ID ${fileId}")

    transaction {
      cazeDao.replace(caze) match {
        case Right(newId) =>
          Logger.debug(s"FileDao: addCaseToFile(): replace(caze) returned ${newId}")

          // For every Caze we need to retrieve the actual dbCaze from the DB first...
          val dbCaze = cazeDao.get(newId).head

          // Now, we need the case IDs for the file with header, fileheader, and member ID, caze.member_id...
          val tmp_case_ids = getRaw(fileId) match {
            case file :: Nil => file.case_ids
            case _ => List.empty
          }

          // Delete case from all files, as a case can only have one parent file...
          for (file <- getFilesWithCase(dbCaze.id)) {
            run(quote {
              tableFile
                .filter(_.id == lift(file.id))
                .update(f => f.case_ids -> lift(file.case_ids.filter(_ != dbCaze.id)))
            })
          }

          // Insert case to new parent file...
          run(quote {
            tableFile
              .filter(f => f.member_id == lift(caze.member_id) && f.id == lift(fileId)) // TODO: Think, we can ommit member_id in filter! CHECK & KILL!
              .update(_.case_ids -> lift((dbCaze.id :: tmp_case_ids).distinct))
          })

          Right(newId)
        case Left(err) =>
          val errorMsg = "FileDao: addCaseToFile() failed: " + err
          Logger.error(errorMsg)
          Left(errorMsg)
      }
    }
  }

  /**
    * According to Java docs, the returning result set here is a Long, n, that indicates
    * which row the cursor is at in the DB after the operation.  (Pretty useless, if you
    * ask me.)
    */

  def removeCaseFromFile(caseId: Int, fileId: Int) = {
    run {
      quote {
        tableFile
          .filter(_.id == lift(fileId))
      }
    } match {
      case f::Nil => {
        val newCaseIds = f.case_ids.filter(_ != caseId)
        Right(
          run(quote(
            tableFile
              .filter(file => file.id == lift(f.id) && file.member_id == lift(f.member_id))
              .update(_.case_ids -> lift(newCaseIds)))).toInt
        )
      }
      case _ => Left(s"FileDao: removeCaseFromFile failed for fileId ${fileId}, caseId ${caseId}")
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
      Logger.debug(s"FileDao: delFile(): deleting file with ID '${fileId}'.")

      run {
        quote {
          tableFile
            .filter(_.id == lift(fileId))
            .delete
        }
      }
    }
  }


  def changeDescription(fileId: Int, newDescription: String) = run { quote {
    tableFile
      .filter(_.id == lift(fileId))
      .update(_.description -> lift(newDescription))
    }
  }

}
