package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.{Caze, ChapterRemedy, FIle}
// import io.circe.syntax._
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
    run(select).map(dbFileToFIle(_)).filter { case Some(_) => true }
  }

  /**
    * A dbFile can only be converted to a FIle by looking up the case IDs in the DB.
    * Hence, this method is rather inefficient and should be AVOIDED if possible!
    */

  private def dbFileToFIle(file: dbFile): Option[FIle] = {
    get(file.header, file.member_id) match {
      case singleDbFile :: Nil => {
        val cazeDao = new CazeDao(dbContext)
        val cazes = singleDbFile.case_ids.map(cazeDao.get(_)).flatten
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
          .map(caze => cazeDao.get(caze.header, caze.member_id)).flatten
          .map(_.id)

      if (cazeIds.length > 0)
        Some(dbFile(0, file.header, file.member_id, file.date, file.description, cazeIds))
      else
        None
    }
    else
      Some(dbFile(0, file.header, file.member_id, file.date, file.description, List.empty))
  }

  def insert(f: FIle) = {
    fileToDBFile(f) match {
      case Some(newDBFile) => run(quote { tableFile.insert(lift(newDBFile)).returning(_.id) })
      case _ => Logger.error("FileDao: insert() failed. Failed to convert FIle " + f.toString()); -1
    }
  }

  def addCaseToFile(caze: Caze, fileheader: String) = {
    val cazeDao = new CazeDao(dbContext)

    cazeDao.replace(caze)
    // For every Caze we need to retrieve the actual dbCaze from the DB first...
    val dbCaze = cazeDao.get(caze.header, caze.member_id).head

    // Now, we need the case IDs for the file with header, fileheader, and member ID, caze.member_id...
    val tmp_case_ids = get(fileheader, caze.member_id) match {
      case file::Nil => file.case_ids
      case _ => List.empty
    }

    // Delete case from all files, as a case can only have one parent...
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
  }

}
