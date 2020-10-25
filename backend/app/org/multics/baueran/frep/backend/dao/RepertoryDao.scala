package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db
import Defs.{SpecialLookupParams, maxNumberOfResults, maxNumberOfSymptoms}
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

  /**
    * Returns list of triples [(abbrev, repertoryAccessLevel, remedyAbbrev)]
    */
  def getAllAvailableRemedies() = {
    implicit val decodeRepAccess = MappedEncoding[String, RepAccess.RepAccess](RepAccess.withName(_))

    run {
      quote {
        for {
          info <- tableInfo
          remedy <- query[Remedy] if (info.abbrev == remedy.abbrev)
        } yield (info.abbrev, info.access, remedy.nameAbbrev)
      }
    }
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

  def getRubricRemedy(rr: RubricRemedy): List[RubricRemedy] = {
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

  def queryRepertory(abbrevFromMenu: String, symptom: String, page: Int, remedyString: String, minWeight: Int): Option[(ResultsCaseRubrics, List[ResultsRemedyStats])] = {
    val searchTerms = new SearchTerms(symptom)
    val abbrev = extractAbbrevFromInput(abbrevFromMenu, symptom)
    val remedyEntered = extractRemedyFromInput(abbrev, remedyString)

    Logger.info(s"queryRepertory(abbrev: ${abbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}) called.")

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

    // Use of approximateSearchTerm is an oversimplification to narrow down the first
    // DB-lookup, which otherwise would return ALWAYS the entire repertory upon a symptom-search.
    val approximateSearchTerm = if (searchTerms.positive.length > 0)
      "%" + searchTerms.positive.head.replaceAll("[^A-Za-z0-9 äÄÜüÖöß\\-]", "").toLowerCase + "%"
    else
      ""

    val tmpRubricsAll = remedyEntered match {
      // User has NOT provided a remedy in search to restrict it
      case None => {
        // Only when weight > 1, do we do a join with remedies.  Otherwise we also want empty rubrics,
        // i.e., those that have no remedy and therefore no weights (which is the else-case of this
        // if-statement).
        if (minWeight > 1) {
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

    val remedyStats = new ArrayBuffer[ResultsRemedyStats]()
    if (page == 0) {
      val sqlString =
        "SELECT rem.nameabbrev, count(rem.nameabbrev), sum(rr.weight) from rubric as rub join rubricremedy as rr on " +
          s"rub.id = rr.rubricid and rr.abbrev='${abbrev}' and rr.rubricid in (${tmpRubricsAll.map(_.id).mkString(",")}) join remedy as rem on " +
          "rem.id=rr.remedyid and rem.abbrev=rub.abbrev and rub.abbrev=rr.abbrev " +
          "group by rem.nameabbrev order by count desc;"
      val conn = dbContext.dataSource.getConnection
      try {
        val statement = conn.createStatement()
        val resultSet = statement.executeQuery(sqlString)
        while (resultSet.next()) {
          val nameabbrev = resultSet.getString("nameabbrev")
          val count = resultSet.getInt("count")
          val sum = resultSet.getInt("sum")
          // Only add remedies which occur more than once in search result. Saves us filtering in the client.
          if (count > 1)
            remedyStats.append(ResultsRemedyStats(nameabbrev, count, sum))
        }
      } catch {
        case e: Throwable => List()
      } finally {
        if (conn != null)
          conn.close()
      }
    }

    val tmpRubricsTruncated =
      tmpRubricsAll
        .drop(page * maxNumberOfResults) // Start on page <page>, where each page is maxNumberOfResults long (so first page is obviously 0!!)
        .take(maxNumberOfResults)

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
    val returnTotalNumberOfPages = math.ceil(tmpRubricsAll.size.toDouble / maxNumberOfResults.toDouble).toInt
    Logger.info(s"queryRepertory(abbrev: ${abbrev}, symptom: ${symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}) found ${tmpRubricsAll.size} case rubrics.")
    Some((ResultsCaseRubrics(tmpRubricsAll.size, returnTotalNumberOfPages, page, caseRubrics), remedyStats.toList))
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
