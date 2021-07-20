package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db
import Defs.{SpecialLookupParams, maxNumberOfResultsPerPage, smallRepertoriesMaxSize, maxNumberOfSymptoms}
import io.getquill.Query

import scala.collection.mutable.ArrayBuffer


class RepertoryDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val Logger = play.api.Logger(this.getClass)
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

  def getAllAvailableRemediesForLoggedInUsers() = {
    val rawQuery = quote {
      infix"""SELECT nameabbrev, namelong, STRING_AGG(remedy.abbrev, ',') AS repertories FROM remedy JOIN info ON info.abbrev = remedy.abbrev AND (access !='Protected') GROUP BY (nameabbrev, namelong)"""
        .as[Query[RemedyAndItsRepertories]]
    }
    run(rawQuery)
  }

  def getAllAvailableRemediesForAnonymousUsers() = {
    val rawQuery = quote {
      infix"""SELECT nameabbrev, namelong, STRING_AGG(remedy.abbrev, ',') AS repertories FROM remedy JOIN info ON info.abbrev = remedy.abbrev AND (access = 'Public' or access = 'Default') GROUP BY (nameabbrev, namelong)"""
        .as[Query[RemedyAndItsRepertories]]
    }
    run(rawQuery)
  }

  private def getAllAvailableRepertoryInfos() = {
    implicit val decodeRepAccess = MappedEncoding[String, RepAccess.RepAccess](RepAccess.withName(_))
    run(quote(tableInfo.filter(_ => true)))
  }

  def getAllAvailableRepertoryInfosForAnonymousUsers() = {
    implicit val decodeRepAccess = MappedEncoding[String, RepAccess.RepAccess](RepAccess.withName(_))
    run(quote(tableInfo.filter(_ => true)))
      .filter(rep => rep.access == RepAccess.Default || rep.access == RepAccess.Public)
  }

  def getAllAvailableRepertoryInfosForLoggedInUsers() = {
    implicit val decodeRepAccess = MappedEncoding[String, RepAccess.RepAccess](RepAccess.withName(_))
    run(quote(tableInfo.filter(_ => true)))
      .filter(_.access != RepAccess.Protected)
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

  def getRubricRemedy(rr: RubricRemedy): List[RubricRemedy] = {
    val get = quote(query[RubricRemedy].filter(rubricRemedy => {
      rubricRemedy.remedyId == lift(rr.remedyId) &&
        rubricRemedy.abbrev == lift(rr.abbrev) &&
        rubricRemedy.chapterId == lift(rr.chapterId) &&
        rubricRemedy.rubricId == lift(rr.rubricId)
    }))
    run(get)
  }

  def getNumberOfRubrics(abbrev: String) = {
    val countSelect = quote { query[Rubric].filter(_.abbrev == lift(abbrev)).map(_.id) }
    val numberOfRubrics = run(countSelect.size)
    numberOfRubrics
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

  def getRubric(abbrev: String, id: Int) = {
    val get = quote(query[Rubric].filter(rubric =>
      rubric.abbrev == lift(abbrev) && rubric.id == lift(id)
    ))
    run(get)
  }

  def getAllRemedies(abbrev: String) = {
    val getAllRemedies =
      quote(query[Remedy]
        .filter(remedy =>
          remedy.abbrev == lift(abbrev)))
    run(getAllRemedies)
  }

  // Extract abbrev from search string, submittedSymptomString, if user supplied "rep:" (see SpecialLookupParams).
  // Otherwise submittedAbbrevString is returned
  private def extractAbbrevFromInput(submittedAbbrevString: String, submittedSymptomString: String): String = {
    if (submittedSymptomString.contains(s"${SpecialLookupParams.Repertory}:")) {
      val abbrevPattern = (""".*""" + SpecialLookupParams.Repertory + """:([\w\-_]+).*""").r
      val abbrevPattern(abbrevFound) = submittedSymptomString

      // Check that repertory exists, otherwise use the one from the drop-down menu
      // (this is also important so that we don't do 'crooked' DB lookups later!)
      if (getAllAvailableRepertoryInfos().filter{ case ri => ri.abbrev == abbrevFound }.size == 1)
        abbrevFound
      else
        submittedAbbrevString
    }
    else
      submittedAbbrevString
  }

  // Extract user supplied remedy from submittedSymptomString. Otherwise, None is returned.
  // The user may have entered Nat-S., nat-s., nat-s, or nats.  We check, if remedy can be uniquely
  // identified, and if so, we return Some(Remedy), otherwise None
  private def extractRemedyFromInput(abbrev: String, remedyStringAsSubmittedByTheUser: String): Option[Remedy] = {
    // First, get list of all remedies
    val allRemedies = getAllRemedies(abbrev.trim)

    // See, if we can find exact match
    for (remedy <- allRemedies)
      if (remedy.nameAbbrev.toLowerCase() == remedyStringAsSubmittedByTheUser.trim.toLowerCase())
        return Some(remedy)

    // See, if we can find match when normalising input
    for (remedy <- allRemedies)
      if (remedy.nameAbbrev.replaceAll("[^A-Za-z]", "").toLowerCase == remedyStringAsSubmittedByTheUser.trim.replaceAll("[^A-Za-z]", "").toLowerCase)
        return Some(remedy)

    None
  }

  def queryRepertory(abbrevFromMenu: String, symptom: String, page: Int, remedyString: String, minWeight: Int, getRemedies: Boolean): Option[(ResultsCaseRubrics, List[ResultsRemedyStats])] = {
    val searchTerms = new SearchTerms(symptom)
    val abbrev = extractAbbrevFromInput(abbrevFromMenu, symptom)
    val remedyEntered = extractRemedyFromInput(abbrev, remedyString)

    if (remedyString.length > 0 && remedyEntered == None) {
      Logger.error(s"ERROR: Search for '$symptom' aborted: remedy ${remedyString} not found in repertory.")
      return None
    }

    if (searchTerms.positive.length == 0 && remedyString.length == 0) {
      Logger.error(s"ERROR: Search for '$symptom' aborted: no positive search terms or remedy entered.")
      return None
    }
    else if (searchTerms.positive.length + searchTerms.negative.length >= maxNumberOfSymptoms) {
      Logger.error(s"ERROR: Cannot enter more than $maxNumberOfSymptoms symptoms.")
      return None
    }
    else if (searchTerms.positive.length == 0 && searchTerms.negative.length > 0 && remedyEntered != None) {
      Logger.error(s"ERROR: Cannot search with just negative search terms.")
      return None
    }
    else if (searchTerms.positive.filter(_.length > 2).length == 0 && remedyEntered == None) {
      Logger.error(s"ERROR: Search term(s) too short.")
      return None
    }

    // Use of approximateSearchTerm is an oversimplification to narrow down the first
    // DB-lookup, which otherwise would return ALWAYS the entire repertory upon a symptom-search.
    val approximateSearchTerm = if (searchTerms.positive.length > 0)
      "%" + searchTerms.positive.head.replaceAll("[^A-Za-z0-9 äÄÜüÖöß\\-]", "").toLowerCase + "%"
    else
      ""

    // We determine the total number of a rubric's rubrics, because small repertories will be presented in
    // full, when the user sends a query - irrespective of the entered search term (cf. Tyler's cold repertory)
    val totalNumberOfRepertoryRubrics = getNumberOfRubrics(abbrev).toInt

    var tmpRubricsAll =
      remedyEntered match {
        // User has NOT provided a remedy in search to restrict it
        case None => {
          // Only when weight > 0, do we do a join with remedies.  Otherwise we also want empty rubrics,
          // i.e., those that have no remedy and therefore no weights (which is the else-case of this
          // if-statement).
          if (minWeight > 0) {
            // The following corresponds roughly to this (except for approx. search term):
            //
            //  select distinct rubric.abbrev, rubric.id, rubric.fullpath from rubric
            //    inner join rubricremedy on rubric.abbrev='publicum' and rubric.fullpath like '%pain%'
            //                               and rubricremedy.abbrev=rubric.abbrev and rubricremedy.weight=1 and rubricremedy.rubricid=rubric.id;
            run {
              quote {
                for {
                  rubric <- query[Rubric]
                  rubricRemedy <- query[RubricRemedy] if {
                    rubric.abbrev == lift(abbrev) &&
                      rubric.abbrev == rubricRemedy.abbrev &&
                      (rubric.fullPath.toLowerCase.like(lift(approximateSearchTerm)) ||
                        rubric.textt.getOrElse("").toLowerCase.like(lift(approximateSearchTerm)) ||
                        rubric.path.getOrElse("").toLowerCase.like(lift(approximateSearchTerm))) &&
                      rubricRemedy.weight >= lift(minWeight) &&
                      rubricRemedy.rubricId == rubric.id
                  }
                } yield (rubric)
              }.distinct.sortBy(_.fullPath)
            }.filter(_.isMatchFor(searchTerms))
          }
          else {
            run {
              quote {
                query[Rubric]
                  .filter(rubric =>
                    rubric.abbrev == lift(abbrev) &&
                      (rubric.fullPath.toLowerCase.like(lift(approximateSearchTerm)) ||
                        rubric.textt.getOrElse("").toLowerCase.like(lift(approximateSearchTerm)) ||
                        rubric.path.getOrElse("").toLowerCase.like(lift(approximateSearchTerm)))
                  )
              }
            }.filter(_.isMatchFor(searchTerms))
              .sortBy(_.fullPath)
          }
        }
        // User has provided a remedy in search to restrict it
        case Some(remedy) => {
          if (searchTerms.positive.length > 0) {
            run {
              quote {
                for {
                  rubric <- query[Rubric]
                  rubricRemedy <- query[RubricRemedy] if {
                    rubric.abbrev == lift(abbrev) &&
                      rubric.abbrev == rubricRemedy.abbrev &&
                      (rubric.fullPath.toLowerCase.like(lift(approximateSearchTerm)) ||
                        rubric.textt.getOrElse("").toLowerCase.like(lift(approximateSearchTerm)) ||
                        rubric.path.getOrElse("").toLowerCase.like(lift(approximateSearchTerm))) &&
                      rubricRemedy.weight >= lift(minWeight) &&
                      rubricRemedy.rubricId == rubric.id &&
                      rubricRemedy.remedyId == lift(remedy).id
                  }
                } yield (rubric)
              }.distinct.sortBy(_.fullPath)
            }.filter(_.isMatchFor(searchTerms))
          }
          else {
            run {
              quote {
                for {
                  rubric <- query[Rubric]
                  rubricRemedy <- query[RubricRemedy] if {
                    rubric.abbrev == lift(abbrev) &&
                      rubric.abbrev == rubricRemedy.abbrev &&
                      rubricRemedy.weight >= lift(minWeight) &&
                      rubricRemedy.rubricId == rubric.id &&
                      rubricRemedy.remedyId == lift(remedy).id
                  }
                } yield (rubric)
              }.distinct.sortBy(_.fullPath)
            }
          }
        }
      }

    // If search was not successful, we stop if repertory was of normal, big size.
    // But if repertory was small, we simply return the entire repertory instead.
    if (tmpRubricsAll.length <= 0) {
      if (totalNumberOfRepertoryRubrics > smallRepertoriesMaxSize) {
        return None
      } else {
        tmpRubricsAll = run(query[Rubric].filter(_.abbrev == lift(abbrev))).sortBy(_.fullPath)
      }
    }

    val remedyStats = new ArrayBuffer[ResultsRemedyStats]()
    if (getRemedies == true) {
      val rawQuery = quote {
        infix"""SELECT rem.nameabbrev, count(rem.nameabbrev), sum(rr.weight) AS cumulativeweight FROM rubric AS rub
                 JOIN rubricremedy AS rr ON rub.id = rr.rubricid AND rr.abbrev='#${abbrev}' AND rr.rubricid IN (#${tmpRubricsAll.map(_.id).mkString(",")})
                 JOIN remedy AS rem ON rem.id=rr.remedyid AND rem.abbrev=rub.abbrev AND rub.abbrev=rr.abbrev
                 GROUP BY rem.nameabbrev ORDER BY count DESC"""
            .as[Query[ResultsRemedyStats]]
      }
      val result = run(rawQuery)
      remedyStats.addAll(result)
    }

    val tmpRubricsTruncated =
      tmpRubricsAll
        .drop(page * maxNumberOfResultsPerPage) // Start on page <page>, where each page is maxNumberOfResultsPerPage long (so first page is obviously 0!!)
        .take(maxNumberOfResultsPerPage)

    val tmpRubricRemedies = run {
      quote {
        query[RubricRemedy]
          .filter(rr =>
            rr.abbrev == lift(abbrev)
              && liftQuery(tmpRubricsTruncated.map(_.id)).contains(rr.rubricId)
          )
      }
    }

    val tmpRemedies = run {
      quote {
        query[Remedy]
          .filter(r =>
            liftQuery(tmpRubricRemedies.map(_.remedyId)).contains(r.id) && r.abbrev == lift(abbrev)
          )
      }
    }

    def getWeightedRemedies(rubric: Rubric) =
      tmpRubricRemedies.filter(_.rubricId == rubric.id).map(rr => WeightedRemedy(tmpRemedies.filter(_.id == rr.remedyId).head, rr.weight))

    // Compute the to be returned results...
    val caseRubrics = tmpRubricsTruncated.map(rubric => CaseRubric(rubric, abbrev, 1, None, getWeightedRemedies(rubric)))
    val returnTotalNumberOfPages = math.ceil(tmpRubricsAll.size.toDouble / maxNumberOfResultsPerPage.toDouble).toInt
    Logger.info(s"queryRepertory(abbrev: ${abbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}, getRemedies: $getRemedies) found ${tmpRubricsAll.size} case rubrics.")
    Some((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, tmpRubricsAll.size, returnTotalNumberOfPages, page, caseRubrics), remedyStats.toList))
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
