package org.multics.baueran.frep.backend

import java.io.Closeable
import javax.sql.DataSource

import io.getquill._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.evolutions.Evolutions
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.inject.{Injector, NewInstanceInjector, SimpleInjector}
import play.api.routing.Router
import play.api.routing.sird._
import models.{Users}

abstract class AppLoader extends ApplicationLoader {
//  override def load(context: Context) =
//    new BuiltInComponentsFromContext(context) with DBComponents with HikariCPComponents {
//      lazy val db = new PostgresJdbcContext[SnakeCase](dbApi.database("default").dataSource.asInstanceOf[DataSource with Closeable])
//
//    }.application
}
