package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.backend.models.Member
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

  def get(id: Long) = { // : Future[Option[User]] = {
    val select = quote {
      tableMember.filter(_.member_id == lift(id))
    }
    run(select)
  }

}

//class MemberDao(dbContext: db.db.DBContext) {
//
//  import dbContext._
//
//  private val tableUsers = quote { querySchema[Users]("users", _.user_id -> "user_id") }
//
//  def insert(u: Users) = {
//    val insert = quote {
//      tableUsers.insert(lift(u))
//    }
//    run(insert)
//  }
//
//  def get(id: Long) = { // : Future[Option[User]] = {
//    val select = quote {
//      tableUsers.filter(_.user_id == lift(id))
//    }
//    run(select)
//  }
//
//}
