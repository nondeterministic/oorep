package org.multics.baueran.frep.backend.db

import io.getquill.{PostgresJdbcContext, SnakeCase}
import com.zaxxer.hikari.HikariDataSource
import play.api.db.Database
import javax.inject._

// https://tutel.me/c/programming/questions/49075152/register.html

package object db {
  // class DBContext(db: Database) extends PostgresJdbcContext(SnakeCase, db.dataSource.asInstanceOf[HikariDataSource])

  @Singleton
  class DBContext @Inject()(db: Database) extends PostgresJdbcContext(SnakeCase, db.dataSource.asInstanceOf[HikariDataSource])
}

