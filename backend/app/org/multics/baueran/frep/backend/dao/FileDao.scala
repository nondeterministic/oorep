package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep._
import backend.db
import shared.FIle
import io.circe.syntax._
import play.api.Logger

case class dbFile(id: Int,
                  header: String,
                  member_id: Int,
                  date: String,
                  description: String,
                  case_ids: List[Int])

class FileDao(dbContext: db.db.DBContext) {

  import dbContext._
  import dbContext.Result

  private val tableFile = quote {
    querySchema[dbFile]("File", _.date -> "date_")
  }

  def get(header: String, member_id: Int) = {
    val select = quote {
      tableFile.filter(file => file.header == lift(header) && file.member_id == lift(member_id))
    }
    run(select)
  }

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

}
