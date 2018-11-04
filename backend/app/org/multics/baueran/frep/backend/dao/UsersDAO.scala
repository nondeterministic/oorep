package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.backend.models.Users
import org.multics.baueran.frep.backend.db

class UsersDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableUsers = quote { querySchema[Users]("users", _.user_id -> "user_id") }

  def insert(u: Users) = {
    val insert = quote {
      tableUsers.insert(lift(u))
    }
    run(insert)
  }

  def get(id: Long) = { // : Future[Option[User]] = {
    val select = quote {
      tableUsers.filter(_.user_id == lift(id))
    }
    run(select)
  }

}
