package org.multics.baueran.frep.backend.dao

import io.getquill
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db

class RepertoryDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableInfo = quote { querySchema[Info]("Info", _.abbrev -> "abbrev") }

  def getInfo(abbrev: String) = { // : Future[Option[User]] = {
    implicit val decodeRepAccess = MappedEncoding[String, RepAccess.RepAccess](RepAccess.withName(_))

    val select = quote {
      tableInfo.filter(_.abbrev == lift(abbrev))
    }
    run(select)
  }

  def insert(info: Info) = {
    implicit val encodeRepAccess = MappedEncoding[RepAccess.RepAccess, String](_.toString())

    val insert = quote {
      tableInfo.insert(lift(info))
    }
    run(insert)
  }

  def insert(chapter: Chapter) = {
    val insert = quote(query[Chapter].insert(lift(chapter)))
    run(insert)
  }

  def insert(rr: RubricRemedy) = {
    val insert = quote(query[RubricRemedy].insert(lift(rr)))
    run(insert)
  }

  def insert(r: Remedy) = {
    val insert = quote(query[Remedy].insert(lift(r)))
    run(insert)
  }

  def insert(cr: ChapterRemedy) = {
    val insert = quote(query[ChapterRemedy].insert(lift(cr)))
    run(insert)
  }

  def insert(r: Rubric) = {
    val insert = quote(query[Rubric].insert(lift(r)))
    run(insert)
  }

// Why not something like this?!
//
//  def insert[T](elem: T) = {
//    val insert = quote(query[T].insert(lift(elem)))
//    run(insert)
//  }

}
