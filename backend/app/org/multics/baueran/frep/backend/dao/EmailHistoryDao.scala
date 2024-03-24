package org.multics.baueran.frep.backend.dao

import io.getquill._
import org.multics.baueran.frep.backend.db
import org.multics.baueran.frep.shared.{EmailHistory, MyDate}

class EmailHistoryDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableEmailHistory = quote { querySchema[EmailHistory]("EmailHistory", _.date -> "date_") }

  def insert(eh: EmailHistory) = {
    run {
      quote {
        tableEmailHistory.insert(_.date -> lift(eh.date), _.email -> lift(eh.email), _.purpose -> lift(eh.purpose))
      }
    }
  }

  def getAllOlderThan(hours: Double) = {
    run {
      quote {
        tableEmailHistory
      }
    }.filter { eh =>
      val ehDate = new MyDate(eh.date)
      ehDate.age() > hours
    }
  }

  def getAllYoungerThan(hours: Double) = {
    run {
      quote {
        tableEmailHistory
      }
    }.filter { eh =>
      val ehDate = new MyDate(eh.date)
      ehDate.age() < hours
    }
  }

  def deleteAll(ids: List[Int]) = {
    run {
      quote {
        tableEmailHistory
          .filter(eh => liftQuery(ids).contains(eh.id))
          .delete
      }
    }
  }

}
