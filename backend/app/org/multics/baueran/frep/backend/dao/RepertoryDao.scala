package org.multics.baueran.frep.backend.dao

import java.sql.{Connection, PreparedStatement, ResultSet}

import io.getquill
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db
import play.api.Logger

import scala.collection.mutable.ArrayBuffer

class RepertoryDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val tableInfo    = quote { querySchema[Info]("Info", _.abbrev -> "abbrev") }
  private val tableChapter = quote { querySchema[Chapter]("Chapter", _.text -> "textt") }

  def getInfo(abbrev: String) = {
    implicit val decodeRepAccess = MappedEncoding[String, RepAccess.RepAccess](RepAccess.withName(_))

    val select = quote { tableInfo.filter(_.abbrev == lift(abbrev)) }
    run(select)
  }

  def insert(info: Info) = {
    implicit val encodeRepAccess = MappedEncoding[RepAccess.RepAccess, String](_.toString())

    val insert = quote { tableInfo.insert(lift(info)) }
    run(insert)
  }

  def getAllAvailableRepertoryInfos() = {
    implicit val decodeRepAccess = MappedEncoding[String, RepAccess.RepAccess](RepAccess.withName(_))
    run(quote(tableInfo.filter(_ => true)))
  }

  def getChapter(chapterId: Int) = {
    val get = quote{ tableChapter.filter(_.id == lift(chapterId)) }
    run(get)
  }

  def insert(chapter: Chapter) = {
    val insert = quote{ tableChapter.insert(lift(chapter)) }
    run(insert)
  }

  def insert(rr: RubricRemedy) = {
    val insert = quote(query[RubricRemedy].insert(lift(rr)))
    run(insert)
  }

  def getRubricRemedy(rr: RubricRemedy) = {
    val get = quote(query[RubricRemedy].filter(rubricRemedy => {
      rubricRemedy.remedyId == lift(rr.remedyId) &&
        rubricRemedy.abbrev == lift(rr.abbrev) &&
        rubricRemedy.chapterId == lift(rr.chapterId) &&
        rubricRemedy.rubricId == lift(rr.rubricId)
    }))
    run(get)
  }

  def insert(r: Remedy) = {
    val insert = quote(query[Remedy].insert(lift(r)))
    run(insert)
  }

  def getRemedy(abbrev: String, id: Int) = {
    val get = quote(query[Remedy].filter(remedy => remedy.abbrev == lift(abbrev) && remedy.id == lift(id)))
    run(get)
  }

  def insert(cr: ChapterRemedy) = {
    val insert = quote(query[ChapterRemedy].insert(lift(cr)))
    run(insert)
  }

  def getChapterRemedy(cr: ChapterRemedy) = {
    val get = quote(query[ChapterRemedy].filter(chapterRemedy => {
      chapterRemedy.abbrev == lift(cr.abbrev) &&
        chapterRemedy.chapterId == lift(cr.chapterId) &&
        chapterRemedy.remedyId == lift(cr.remedyId)
    }))
    run(get)
  }

  def insert(r: Rubric) = {
    val insert = quote(query[Rubric].insert(lift(r)))
    run(insert)
  }

  def getRubric(r: Rubric) = {
    val get = quote(query[Rubric].filter(rubric =>
      rubric.abbrev == lift(r.abbrev) && rubric.id == lift(r.id)
    ))
    run(get)
  }

//val preparer: (Connection) => (PreparedStatement)  = ctx.prepare(q)
//// SELECT x1.id, x1.description, x1.sku FROM Product x1 WHERE x1.id = 1
//
//// Use ugly stateful code, bracketed effects, or try-with-resources here:
//var preparedStatement: PreparedStatement = _
//var resultSet: ResultSet = _
//
//try {
//  preparedStatement = preparer(myCustomDataSource.getConnection)
//  resultSet = preparedStatement.executeQuery()
//} catch {
//  case e: Exception =>
//    // Close the preparedStatement and catch possible exceptions
//    // Close the resultSet and catch possible exceptions
//}

  def lookupSymptom2(abbrev: String, symptom: String) = {
    val searchStrings = symptom.
                          trim.                                                    // Remove trailing spaces
                          replaceAll(" +", " ").              // Remove double spaces
                          replaceAll("[^A-Za-z0-9 \\-*]", "").// Remove all but alphanum-, wildcard-, minus-symbols
                          split(" ")                                       // Get list of search strings

    val posSearchTerms = searchStrings.filter(!_.startsWith("-")).toList
    val negSearchTerms = searchStrings.filter(_.startsWith("-")).map(_.substring(1)).toList


//    val rawQuery = quote { (abbre: String, sympto: String) =>
//      // infix"""SELECT * FROM rubric WHERE abbrev='kent' AND fullpath LIKE '%splinter%'""".as[Query[Rubric]]
//      infix"""SELECT * FROM rubric WHERE abbrev=? AND fullpath LIKE ?""".as[Query[Rubric]]
//    }
//    val preparer: (Connection) => (PreparedStatement) = prepare(rawQuery(lift(abbrev), lift(symptom)))
//    var resultSet: ResultSet = null
//    try {
//      val preparedStatement = preparer(dataSource.getConnection())
//      println(preparedStatement.isPoolable)
//      preparedStatement.setString(1, "kent")
//      preparedStatement.setString(2, "%splinter%")
//      println(preparedStatement.isPoolable)
//      resultSet = preparedStatement.executeQuery()
//    } catch {
//      case e: Exception => println("SHIT HAPPENED: " + e.getMessage)
//    }
//
//    println("*********************************")
//    while (resultSet != null && resultSet.next()) {
//      println(resultSet.getString("abbrev"))
//    }
//    println("---------------------------------")
//    List() : List[Rubric]

    val get = quote(query[Rubric].filter(rubric =>
      rubric.abbrev == lift(abbrev) && rubric.chapterId >= 0
    ))
    run(get).filter(_.isMatchFor(posSearchTerms, negSearchTerms))
  }

  def lookupSymptom(abbrev: String, symptom: String): List[Rubric] = {
    val searchStrings = symptom.
      trim.                                                    // Remove trailing spaces
      replaceAll(" +", " ").              // Remove double spaces
      replaceAll("[^A-Za-z0-9 \\-*]", "").// Remove all but alphanum-, wildcard-, minus-symbols
      split(" ")                                       // Get list of search strings

    val posSearchTerms = searchStrings.filter(!_.startsWith("-")).toList
    val negSearchTerms = searchStrings.filter(_.startsWith("-")).map(_.substring(1)).toList

    println("Query 1...")
    val get = quote(query[Rubric].filter(rubric =>
      rubric.abbrev == lift(abbrev) && rubric.chapterId >= 0
    ))
    val tmpResults = run(get).filter(_.isMatchFor(posSearchTerms, negSearchTerms))

    println("Query 2...")
    val rawQueryStr =
      "SELECT rubric, remedy, rubricremedy.weight FROM rubric, rubricremedy, remedy WHERE " +
        "rubric.abbrev='kent' AND " +
        s"rubric.id IN (${tmpResults.map(_.id).mkString(", ")}) AND " +
        "rubricremedy.abbrev=rubric.abbrev AND " +
        "remedy.abbrev=rubric.abbrev AND " +
        "rubricremedy.rubricid=rubric.id AND " +
        "remedy.id=rubricremedy.remedyid"

    println("Query: " + rawQueryStr)

    val rawQuery = quote { (q: String) =>
      infix"""$q""".as[Query[Rubric]]
    }
    val t = run(rawQuery(lift(rawQueryStr)))
    println(t)

//    val preparer: (Connection) => (PreparedStatement) = prepare(rawQuery(lift(rawQueryStr)))
//    var resultSet: ResultSet = null
//    try {
//      val preparedStatement = preparer(dataSource.getConnection())
//      // preparedStatement.setString(1, "kent")
//      println("Executing raw query: " + preparedStatement.toString)
//      resultSet = preparedStatement.executeQuery()
//      println("Executed.")
//    } catch {
//      case e: Exception =>
//        println("SHIT HAPPENED: " + e.getMessage)
//        println("STACKTRACE: " + e.getStackTrace.mkString("\n"))
//    }

    tmpResults
  }

  def getRemediesForRubric(rubric: Rubric): Seq[(Remedy, Int)] = {
    var result: ArrayBuffer[(Remedy, Int)] = new ArrayBuffer[(Remedy,Int)]
    val filter = quote { query[RubricRemedy].filter(rr => rr.rubricId == lift(rubric.id) && rr.abbrev == lift(rubric.abbrev)) }
    val remedyIdWeightTuples: Seq[(Int, Int)] = run(filter).map(rr => (rr.remedyId, rr.weight))

    remedyIdWeightTuples.foreach { case (rid, rweight) =>
      val allRemedies = quote { query[Remedy].filter(r => r.abbrev == lift(rubric.abbrev)) }
      run(allRemedies).find(_.id == rid) match {
        case Some(remedy) => result += ((remedy, rweight))
        case None => Logger.warn("WARNING: RepertoryDao.getRemediesForRubric: No remedy found.")
      }
    }

    result
  }

  def insert(cr: CaseRubric) = {
    implicit val encodeCaseRubric = MappedEncoding[CaseRubric, String](_.toString())

    val insert = quote(query[CaseRubric].insert(lift(cr)))
    run(insert)
  }

  def insert(c: org.multics.baueran.frep.shared.Caze) = {
    implicit val encodeCase = MappedEncoding[org.multics.baueran.frep.shared.Caze, String](_.toString())

    val insert = quote(query[org.multics.baueran.frep.shared.Caze].insert(lift(c)))
    run(insert)
  }

}
