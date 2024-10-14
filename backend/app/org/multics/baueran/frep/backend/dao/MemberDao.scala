package org.multics.baueran.frep.backend.dao

import io.getquill._
import org.multics.baueran.frep.shared.{Member, MyDate}
import org.multics.baueran.frep.backend.db

class MemberDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableMember = quote { query[Member] }

  // Stores the hash of `password` in the member table of database
  def updatePassword(password: String, id: Int) = {
    val rawInsert = quote {
      sql"""UPDATE member SET hash=(crypt('#${password}', gen_salt('bf'))) WHERE member_id=#${id}"""
        .as[Update[Member]]
    }
    run(rawInsert)
  }

  def get(id: Int): Option[Member] = {
    val select = quote {
      tableMember.filter(_.member_id == lift(id))
    }
    run(select) match {
      case member :: Nil => Some(member)
      case _ => None
    }
  }

  def getFromEmail(email: String) = {
    val select = quote{ query[Member].filter(_.email == lift(email)) }
    run(select)
  }

  def getFromUsername(username: String) = {
    val select = quote{ query[Member].filter(_.member_name == lift(username)) }
    run(select)
  }

  def setLastSeen(memberId: Int, date: MyDate) = {
    run {
      quote {
        tableMember
          .filter(_.member_id == lift(memberId))
          .update(_.lastseen -> Some(lift(date.toString())))
      }
    }
  }

  def increaseLoginCounter(memberId: Int) = {
    run {
      quote {
        tableMember
          .filter(_.member_id == lift(memberId))
          .update(member => member.numberoflogins -> (member.numberoflogins + 1))
      }
    }
  }

}
