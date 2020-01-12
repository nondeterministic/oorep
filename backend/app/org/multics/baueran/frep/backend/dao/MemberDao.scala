package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.shared.{Member, MyDate}
import org.multics.baueran.frep.backend.db

class MemberDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableMember = quote { querySchema[Member]("Member", _.member_id -> "member_id") }

  def insert(u: Member) = {
    val insert = quote {
      tableMember.insert(lift(u))
    }
    run(insert)
  }

  def get(id: Int) = {
    val select = quote {
      tableMember.filter(_.member_id == lift(id))
    }
    run(select)
  }

  def getFromEmail(email: String) = {
    val select = quote{ query[Member].filter(_.email == lift(email)) }
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
