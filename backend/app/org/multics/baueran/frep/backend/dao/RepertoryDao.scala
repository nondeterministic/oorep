package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.shared
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db
import Defs.maxNumberOfResults
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

//  def getRubric(r: Rubric) = {
//    val get = quote(query[Rubric].filter(rubric =>
//      rubric.abbrev == lift(r.abbrev) && rubric.id == lift(r.id)
//    ))
//    run(get)
//  }

  def getRubric(abbrev: String, id: Int) = {
    val get = quote(query[Rubric].filter(rubric =>
      rubric.abbrev == lift(abbrev) && rubric.id == lift(id)
    ))
    run(get)
  }

  def lookupSymptom(abbrevFromMenu: String, symptom: String): List[CaseRubric] = {

    // Extract abbrev from search string, if user supplied "rep:".
    // Otherwise abbrevFromMenu is used.
    def getAbbrev(submittedAbbrevString: String, submittedSymptomString: String): String = {
      if (symptom.contains("rep:")) {
        val abbrevPattern = """.*rep:([\w\-_]+).*""".r
        val abbrevPattern(abbrev) = symptom

        // Check that repertory exists, otherwise use the one from the drop-down menu
        // (this is also important so that we don't do 'crooked' DB lookups later!)
        // TODO: Add "special repertory", "all" which will search in ALL repertories, like so: "rep:all"!
        if (getAllAvailableRepertoryInfos().filter{ case ri => ri.abbrev == abbrev }.size == 1)
          abbrev
        else
          abbrevFromMenu
      }
      else
        abbrevFromMenu
    }

    val searchTerms = new SearchTerms(symptom)
    val abbrev = getAbbrev(abbrevFromMenu, symptom)

    if (searchTerms.positive.length == 0) {
      Logger.warn(s"INFO: Search for `$symptom' aborted: no positive search terms.")
      return List()
    }

    // TODO: use of approximateSearchTerm is an oversimplification to narrow down the first
    // DB-lookup, which otherwise would return ALWAYS the entire repertory.
    // It is also very brittle with the Umlauts...
    val approximateSearchTerm = "%" + searchTerms.positive.head.replaceAll("[^A-Za-z0-9 äÄÜüÖöß\\-]", "").toLowerCase + "%"

    val tmpResults =
      run(
        quote {
          query[Rubric]
            .filter(rubric =>
              rubric.abbrev == lift(abbrev) &&
                rubric.chapterId >= 0 &&
                (rubric.fullPath.toLowerCase.like(lift(approximateSearchTerm)) ||
                  rubric.textt.getOrElse("").toLowerCase.like(lift(approximateSearchTerm)) ||
                  rubric.path.getOrElse("").toLowerCase.like(lift(approximateSearchTerm)))
            )
        }
      ).filter(_.isMatchFor(searchTerms))
        .sortBy(_.fullPath)
        .take(maxNumberOfResults)

    val results =
      run(
        quote {
          for {
            rubrics <- query[Rubric].filter(rubric => rubric.abbrev == lift(abbrev) && liftQuery(tmpResults.map(_.id)).contains(rubric.id))
            remedies <- query[Remedy].join(remedy => remedy.abbrev == rubrics.abbrev)
            rr <- query[RubricRemedy].join(r => r.remedyId == remedies.id && r.rubricId == rubrics.id && r.abbrev == rubrics.abbrev)
          } yield (rubrics, remedies, rr)
        }
      )

    def getWeightedRemedies(rubric: Rubric) = {
      results
        .filter(_._1 == rubric)
        .map { case (_, rem, rr) => WeightedRemedy(rem, rr.weight) }
    }

    results
      .map { case (rubric, _, _) => rubric }
      .distinct
      .map { rubric => CaseRubric(rubric, abbrev, 1, getWeightedRemedies(rubric)) }
      .sortBy { _.rubric.fullPath }
  }

//  def getRemediesForRubric(rubric: Rubric): Seq[(Remedy, Int)] = {
//    var result: ArrayBuffer[(Remedy, Int)] = new ArrayBuffer[(Remedy,Int)]
//    val filter = quote { query[RubricRemedy].filter(rr => rr.rubricId == lift(rubric.id) && rr.abbrev == lift(rubric.abbrev)) }
//    val remedyIdWeightTuples: Seq[(Int, Int)] = run(filter).map(rr => (rr.remedyId, rr.weight))
//
//    remedyIdWeightTuples.foreach { case (rid, rweight) =>
//      val allRemedies = quote { query[Remedy].filter(r => r.abbrev == lift(rubric.abbrev)) }
//      run(allRemedies).find(_.id == rid) match {
//        case Some(remedy) => result += ((remedy, rweight))
//        case None => Logger.warn("WARNING: RepertoryDao.getRemediesForRubric: No remedy found.")
//      }
//    }
//
//    result
//  }

  def getRemediesForRubric(rubric: Rubric): List[WeightedRemedy] = { // Seq[(Remedy, Int)] = {
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

    result.map { case (r,w) => WeightedRemedy(r, w) }.toList
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
