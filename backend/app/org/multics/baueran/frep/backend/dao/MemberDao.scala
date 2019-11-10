package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.backend.db
import org.multics.baueran.frep.shared.Member

import java.util.Date


class MemberDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableMember = quote { querySchema[Member]("Member", _.member_id -> "member_id") }

  def insert(u: Member) = {
    val insert = quote {
      tableMember.insert(lift(u))
    }
    run(insert)
  }

  def get(id: Int) = { // : Future[Option[User]] = {
    val select = quote {
      tableMember.filter(_.member_id == lift(id))
    }
    run(select)
  }

  def getFromEmail(email: String) = {
    val select = quote{ query[Member].filter(_.email == lift(email)) }
    run(select)
  }

  def setLastSeen(memberId: Int, date: Date) = {
    run {
      quote {
        tableMember
          .filter(_.member_id == lift(memberId))
          .update(_.lastseen -> Some(lift(date)))
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
