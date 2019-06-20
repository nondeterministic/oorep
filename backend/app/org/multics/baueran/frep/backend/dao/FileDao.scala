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

  def get(header: String, member_id: Int) = {
    val select = quote {
      tableFile.filter(file => file.header == lift(header) && file.member_id == lift(member_id))
    }
    run(select)
  }

  def getFilesForMember(member_id: Int) = {
    val select = quote {
      tableFile.filter(_.member_id == lift(member_id))
    }
    run(select).map(dbFileToFIle(_)).map { case Some(file) => file }
  }

  def getCasesFromFile(fileName: String, member_id: Int) = {
    val file = getFilesForMember(member_id).find(_.header == fileName)
    val cases = file match {
      case Some(f) => f.cazes
      case None => List()
    }
    cases
  }

  /**
    * A dbFile can only be converted to a FIle by looking up the case IDs in the DB.
    * Hence, this method is rather inefficient and should be AVOIDED if possible!
    */

  private def dbFileToFIle(file: dbFile): Option[FIle] = {
    get(file.header, file.member_id) match {
      case singleDbFile :: Nil => {
        val cazeDao = new CazeDao(dbContext)
        val cazes = singleDbFile.case_ids.map(cazeDao.get(_, singleDbFile.member_id)).flatten
        Some(FIle(singleDbFile.header, singleDbFile.member_id, singleDbFile.date, singleDbFile.description, cazes))
      }
      case _ => None
    }
  }

  private def fileToDBFile(file: FIle): Option[dbFile] = {
    if (file.cazes.length > 0) {
      val cazeDao = new CazeDao(dbContext)
      val cazeIds =
        file.cazes
          .map(caze => cazeDao.get(caze.id, caze.member_id)).flatten
          .map(_.id)

      if (cazeIds.length > 0)
        Some(dbFile(0, file.header, file.member_id, file.date, file.description, cazeIds))
      else
        None
    }
    else
      Some(dbFile(0, file.header, file.member_id, file.date, file.description, List.empty))
  }

  def insert(f: FIle): Either[String, Int] = {
    fileToDBFile(f) match {
      case Some(newDBFile) =>
        Right(run(quote { tableFile.insert(lift(newDBFile)).returning(_.id) }))
      case _ =>
        val err = "FileDao: insert() failed. Failed to convert FIle " + f.toString()
        Logger.error(err)
        Left(err)
    }
  }

  def addCaseToFile(caze: Caze, fileheader: String) = {
    val cazeDao = new CazeDao(dbContext)

    transaction {
      cazeDao.replace(caze) match {
        case Right(newId) =>
          // For every Caze we need to retrieve the actual dbCaze from the DB first...
          val dbCaze = cazeDao.get(newId, caze.member_id).head

          // Now, we need the case IDs for the file with header, fileheader, and member ID, caze.member_id...
          val tmp_case_ids = get(fileheader, caze.member_id) match {
            case file :: Nil => file.case_ids
            case _ => List.empty
          }

          // Delete case from all files, as a case can only have one parent file...
          val filesWithCase: List[dbFile] = run(quote {
            tableFile
              .filter(f => f.member_id == lift(dbCaze.member_id) && f.case_ids.contains(lift(dbCaze.id)))
          })
          for (file <- filesWithCase) {
            run(quote {
              tableFile
                .filter(_.id == lift(file.id))
                .update(f => f.case_ids -> lift(file.case_ids.filter(_ != dbCaze.id)))
            })
          }

          // Insert case to new parent file...
          run(quote {
            tableFile
              .filter(f => f.member_id == lift(caze.member_id) && f.header == lift(fileheader))
              .update(_.case_ids -> lift((dbCaze.id :: tmp_case_ids).distinct))
          })
        case Left(err) =>
          Logger.error("FileDao: addCaseToFile() failed: " + err)
      }
    }
  }

  // removeCaseFromFile() is somewhat odd.  The intended code was like this:
  //
  //  def removeCaseFromFile(memberId: Int, fileName: String, caseId: Int) = run {
  //    quote {
  //      tableFile
  //        .filter(file => file.member_id == lift(memberId) && file.header == lift(fileName))
  //        .update(ff => {
  //          val caseIds = lift(ff.case_ids.filter(_ != caseId))
  //          ff.case_ids -> lift(caseIds)
  //        })
  //    }
  //  }
  //
  // but I couldn't get it either through the parser or had runtime errors, cf.:
  //
  // https://stackoverflow.com/questions/56672799/cant-parse-scala-quill-expression-to-ast

  /**
    * According to Java docs, the returning result set here is a Long, n, that indicates
    * which row the cursor is at in the DB after the operation.  (Pretty useless, if you ask me.)
    */

  def removeCaseFromFile(memberId: Int, fileName: String, caseId: Int) = {
    run {
      quote {
        tableFile
          .filter(file => file.member_id == lift(memberId) && file.header == lift(fileName))
      }
    } match {
      case f::Nil => {
        val newCaseIds = f.case_ids.filter(_ != caseId)
        Right(run(quote(tableFile.update(_.case_ids -> lift(newCaseIds)))).toInt)
      }
      case _ => Left(s"FileDao: removeCaseFromFile failed for memberId ${memberId}, fileName ${fileName}, caseId ${caseId}")
    }
  }

  /**
    * Deletes a file AND its associated cases. (Don't say, I didn't warn you!)
    */

  def delFile(fileheader: String, memberId: Int) = {
    val cazeIds = run {
      quote {
        tableFile
          .filter(f => f.header == lift(fileheader) && f.member_id == lift(memberId))
      }
    }.flatMap(_.case_ids)

    val cazeDao = new CazeDao(dbContext)

    transaction {
      cazeIds.foreach(cazeDao.delete(_, memberId))

      run {
        quote {
          tableFile
            .filter(f => f.header == lift(fileheader) && f.member_id == lift(memberId))
            .delete
        }
      }
    }
  }


  def changeDescription(fileheader: String, memberId: Int, newDescription: String) = run { quote {
    tableFile
      .filter(f => f.member_id == lift(memberId) && f.header == lift(fileheader))
      .update(_.description -> lift(newDescription))
    }
  }

}
