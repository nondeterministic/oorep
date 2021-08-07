package org.multics.baueran.frep.backend.dao

import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.backend.db
import Defs.{ResourceAccessLvl, maxNumberOfResultsPerPage, maxNumberOfSymptoms, smallRepertoriesMaxSize}

import scala.collection.mutable
import io.getquill.Query

import scala.collection.mutable.ArrayBuffer

class RepertoryDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val Logger = play.api.Logger(this.getClass)

  // The following declarations make the backend somewhat more stateful but not stateful
  // in the sense that they store client information, which would absolutely break the
  // REST-philosophy.  Instead, these vars are a form of caching to avoid uneccessary
  // database queries and should scale, survive restarts, etc.
  //
  // The only thing that these variables don't do is update on database changes without
  // a restart of the backend.  But a database change always requires restarting of the
  // backend, so it's no practical problem.

  private var _remedies: List[Remedy] = List()
  private val _repertories = new Repertories()
  private var _extendedInfosLoggedIn: List[InfoExtended] = List()
  private var _extendedInfosAnonymous: List[InfoExtended] = List()
  private val _numberOfRubricsInRep = new mutable.HashMap[String, Int]()

  private def getNumberOfRubrics(abbrev: String): Int = {
    _numberOfRubricsInRep.get(abbrev) match {
      case Some(numberOfRubrics) => numberOfRubrics
      case _ => {
        val numberOfRubrics = run {
          quote {
            query[Rubric].filter(_.abbrev == lift(abbrev)).map(_.id)
          }.size
        }
        if (numberOfRubrics > 0)
          _numberOfRubricsInRep.put(abbrev, numberOfRubrics.toInt)
        else
          Logger.error(s"ERROR: number of rubrics 0 for abbrev '${abbrev}'. This is a bug, in that malformed abbrev information should not have made it this far into the backend.")

        numberOfRubrics.toInt
      }
    }
  }

  def getRemedies(): List[Remedy] = {
    if (_remedies.length == 0) {
      Logger.info("RepetoryDao: Getting all remedies from DB...")
      _remedies = run(quote(query[Remedy].filter(_ => true)))
    }

    _remedies
  }

  private def getRemedies(abbrev: String): List[Remedy] = {
    val cleanAbbrev = abbrev.replaceAll("[^0-9A-Za-z\\-]", "")

    if (cleanAbbrev.length == 0 || cleanAbbrev.length > 15)
      return List()

    _repertories.getRemedies(cleanAbbrev) match {
      case Nil => {
        Logger.info("RepertoryDao: Getting abbrev-remedies from DB...")
        val remedies = {
          run {
            quote {
              infix"""select remedy.id, remedy.nameabbrev, remedy.namelong, remedy.namealt
                     from remedy
                        join repertoriesandremedies on abbrev='#${cleanAbbrev}' and remedy.id = any(remedy_ids::int[])"""
                .as[Query[Remedy]]
            }
          }
        }

        (_extendedInfosAnonymous ::: _extendedInfosLoggedIn).find(_.info.abbrev == cleanAbbrev) match {
          case Some(extInfo) => _repertories.put(extInfo, new Remedies(remedies))
          case None => Logger.error("RepertoryDao: tried to add repertory remedies to data structure, but couldn't find extended repertory info.")
        }

        remedies
      }
      case remedies => {
        Logger.info("RepertoryDao: Getting abbrev-remedies from RAM...")
        remedies
      }
    }
  }

  def getRepsAndRemedies(isUserLoggedIn: Boolean): List[InfoExtended] = {

    // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // There is also this helpful view here in the DB which we query here and which needs REFRESHING,
    // when the DB data changes (e.g., when new repertory is added or rubrics modified manually):
    //
    // (TODO: Can we automatically trigger such a refresh within PostgreSQL, say, tied to modifications  in table 'rubricremedy' or multiple tables?)
    //
    // CREATE MATERIALIZED VIEW repertoriesAndRemedies AS SELECT abbrev, array_agg(id) AS remedy_ids
    //    FROM (SELECT DISTINCT remedy.id, rubricremedy.abbrev
    //             FROM remedy
    //             JOIN rubricremedy ON remedyid=remedy.id
    //             JOIN info ON rubricremedy.abbrev=info.abbrev AND access<>'Protected'
    //                ORDER BY remedy.id) AS x GROUP BY (abbrev);
    //
    // ===> or rather, the new, udpated version, here:
    //
    // CREATE MATERIALIZED VIEW repertoriesandremedies AS SELECT info.abbrev, title, languag, authorlastname, authorfirstname, yearr, publisher, license, edition, access, displaytitle,
    //                                                           ARRAY_AGG(DISTINCT remedy.id) AS remedy_ids // TODO: Delete: ', CARDINALITY(ARRAY_AGG(DISTINCT rubricremedy.rubricid)) AS nonemptyrubrics'
    //                                                       FROM info
    //                                                       JOIN rubricremedy ON rubricremedy.abbrev=info.abbrev
    //                                                       JOIN remedy ON remedy.id=rubricremedy.remedyid
    //                                                          GROUP BY(info.abbrev, title, languag, authorlastname, authorfirstname, yearr, publisher, license, edition, access, displaytitle);
    // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    case class TmpRepsAndRemedies(abbrev: String, title: String, languag: String,
                                  authorLastName: Option[String], authorFirstName: Option[String],
                                  yearr: Option[Int], publisher: Option[String], license: Option[String],
                                  edition: Option[String], access: String, displaytitle: Option[String],
                                  remedy_ids: List[Int])

    object TmpRepsAndRemedies {
      import io.circe.{Decoder, Encoder}
      import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
      implicit val myDecoderTmpRepsSandRemedies: Decoder[TmpRepsAndRemedies] = deriveDecoder[TmpRepsAndRemedies]
      implicit val myEncoderTmpRepsSandRemedies: Encoder[TmpRepsAndRemedies] = deriveEncoder[TmpRepsAndRemedies]
    }

    val tmpResults = run {
      quote {
        infix"""SELECT * FROM repertoriesandremedies"""
          .as[Query[TmpRepsAndRemedies]]
      }
    }.map { res =>
      InfoExtended(
        Info(res.abbrev, res.title, res.languag, res.authorLastName, res.authorFirstName, res.yearr, res.publisher, res.license, res.edition, ResourceAccessLvl.withName(res.access), res.displaytitle),
        res.remedy_ids
      )
    }

    if (isUserLoggedIn) {
      if (_extendedInfosLoggedIn.length == 0)
        _extendedInfosLoggedIn = tmpResults.filter(_.info.access != ResourceAccessLvl.Protected)
      _extendedInfosLoggedIn
    } else {
      if (_extendedInfosAnonymous.length == 0)
        _extendedInfosAnonymous = tmpResults.filter(extendedInfo => extendedInfo.info.access == ResourceAccessLvl.Public || extendedInfo.info.access == ResourceAccessLvl.Default)
      _extendedInfosAnonymous
    }
  }

  def queryRepertory(abbrevFromMenu: String, searchTerms: SearchTerms, page: Int, remedyString: String, minWeight: Int, getRemedies: Boolean): Option[(ResultsCaseRubrics, List[ResultsRemedyStats])] = {
    val abbrev = abbrevFromMenu.replaceAll("[^0-9A-Za-z\\-]", "")

    // Determining entered remedy is a bit more work than a simple declaration...
    val remedies = this.getRemedies(abbrev)
    val remedyList = new Remedies(remedies)
    val remedyEntered = remedyList.getRemedyEntered(remedyString) match {
      case RemedyEntered(Some(id), _) => remedies.find(_.id == id)
      case _ => None
    }

    if (remedyString.length > 0 && remedyEntered == None) {
      Logger.error(s"ERROR: Search for '${searchTerms.symptom}' aborted: remedy ${remedyString} not found in repertory.")
      return None
    }

    if (searchTerms.positive.length == 0 && remedyString.length == 0) {
      Logger.error(s"ERROR: Search for '${searchTerms.symptom}' aborted: no positive search terms or remedy entered.")
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
    val approximateSearchTerm = {
      if (searchTerms.exactPositiveOnly.length > 0)
        "%" + searchTerms.exactPositiveOnly.head.replaceAll("[\\*]", "%").toLowerCase + "%"
      else if (searchTerms.positive.length > 0)
        "%" + searchTerms.positive.head.replaceAll("[^A-Za-z0-9 äÄÜüÖöß\\-]", "").toLowerCase + "%"
      else
        ""
    }

    // We determine the total number of a repertory's rubrics, because small repertories will be presented in
    // full, when the user sends a query - irrespective of the entered search term (cf. Tyler's cold repertory)
    val totalNumberOfRepertoryRubrics = getNumberOfRubrics(abbrev)

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
                 JOIN remedy AS rem ON rem.id=rr.remedyid AND rub.abbrev=rr.abbrev
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
            liftQuery(tmpRubricRemedies.map(_.remedyId)).contains(r.id)
          )
      }
    }

    def getWeightedRemedies(rubric: Rubric) =
      tmpRubricRemedies.filter(_.rubricId == rubric.id).map(rr => WeightedRemedy(tmpRemedies.filter(_.id == rr.remedyId).head, rr.weight))

    // Compute the to be returned results...
    val caseRubrics = tmpRubricsTruncated.map(rubric => CaseRubric(rubric, abbrev, 1, None, getWeightedRemedies(rubric)))
    val returnTotalNumberOfPages = math.ceil(tmpRubricsAll.size.toDouble / maxNumberOfResultsPerPage.toDouble).toInt
    Logger.info(s"queryRepertory(abbrev: ${abbrev}, symptom: ${searchTerms.symptom}, page: ${page}, remedy: ${remedyString}, weight: ${minWeight}, getRemedies: $getRemedies) found ${tmpRubricsAll.size} case rubrics.")
    Some((ResultsCaseRubrics(totalNumberOfRepertoryRubrics, tmpRubricsAll.size, returnTotalNumberOfPages, page, caseRubrics), remedyStats.toList))
  }

}
