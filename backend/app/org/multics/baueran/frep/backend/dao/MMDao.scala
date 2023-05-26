package org.multics.baueran.frep.backend.dao

import io.getquill.Query
import org.multics.baueran.frep.shared.{HitsPerRemedy, MMAllSearchResults, MMAndRemedyIds, MMChapter, MMInfo, MMSearchResult, MMSection, Member, MyDate, Remedies, Remedy, RemedyEntered, SearchTerms}
import org.multics.baueran.frep.backend.db
import org.multics.baueran.frep.shared.Defs.{ResourceAccessLvl, maxNumberOfResultsPerMMPage, maxNumberOfSymptoms}

class MMDao(dbContext: db.db.DBContext) {

  import dbContext._

  private val Logger = play.api.Logger(this.getClass)

  private def getRemediesForMM(abbrev: String): List[Remedy] = {
    val cleanedAbbrev = abbrev.replaceAll("[^0-9A-Za-z\\-]", "")

    // Long mm-abbrevs smell like an SQL-injection attempt...
    if (cleanedAbbrev.length == 0 || abbrev.length() > 15)
      return List()

    run {
      sql"""SELECT remedy.id, remedy.nameabbrev, remedy.namelong, remedy.namealt
                 FROM remedy
                    JOIN mmchapter ON remedy_id=remedy.id
                    JOIN mminfo ON mminfo_id=mminfo.id AND mminfo.abbrev='#${cleanedAbbrev}'
                    ORDER BY nameabbrev"""
        .as[Query[Remedy]]
    }
  }

  /**
    *
    * Return all the stored materia medica information, and their remedy names.
    *
    * (This is probably the ugliest (but not so nasty or hard to understand) code in OOREP.
    * I couldn't for the life of it, work out how to use a Query[MMsAndRemedies] directly.
    * So a translator class TmpMMsAndRemedies was necessary - and needs to be adapted, in
    * case the content of the other tables changes, of course.)
    */
  def getMMsAndRemedies(loggedInMember: Option[Member]): List[MMAndRemedyIds] = {
    case class TmpMMsAndRemedies(id: Int,
                              abbrev: String,
                              lang: Option[String],
                              fulltitle: Option[String],
                              authorlastname: Option[String],
                              authorfirstname: Option[String],
                              publisher: Option[String],
                              yearr: Int,
                              license: Option[String],
                              access: String,
                              displaytitle: Option[String],
                              remedy_ids: List[Int])

    object TmpMMsAndRemedies {
      import io.circe.{Decoder, Encoder}
      import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
      implicit val myDecoderTmpMMSandRemedies: Decoder[TmpMMsAndRemedies] = deriveDecoder[TmpMMsAndRemedies]
      implicit val myEncoderTmpMMSandRemedies: Encoder[TmpMMsAndRemedies] = deriveEncoder[TmpMMsAndRemedies]
    }

    val tmpResults = run {
      quote {
        sql"""SELECT mminfo.id, mminfo.abbrev, mminfo.lang, mminfo.fulltitle, mminfo.authorlastname, mminfo.authorfirstname,
                       mminfo.publisher, mminfo.yearr, mminfo.license, mminfo.access, mminfo.displaytitle, ARRAY_AGG(remedy_id) remedy_ids
                 FROM mmchapter
                    JOIN mminfo ON mmchapter.mminfo_id = mminfo.id GROUP BY (mminfo.abbrev, mminfo.access)"""
          .as[Query[TmpMMsAndRemedies]]
      }
    }.map(r =>
      MMAndRemedyIds(
        MMInfo(
          r.id, r.abbrev, r.lang, r.fulltitle, r.authorlastname, r.authorfirstname, r.publisher, r.yearr, r.license, r.access, r.displaytitle
        ),
        r.remedy_ids)
    )

    loggedInMember match {
      case Some(member) =>
        if (member.access.getOrElse("") == ResourceAccessLvl.Private.toString)
          tmpResults
        else if (member.access.getOrElse("") == ResourceAccessLvl.Protected.toString)
          tmpResults.filter(result => result.mminfo.access != ResourceAccessLvl.Private.toString)
        else
          tmpResults.filter(result => result.mminfo.access == ResourceAccessLvl.Public.toString || result.mminfo.access == ResourceAccessLvl.Default.toString)
      case None =>
        tmpResults.filter(result => result.mminfo.access == ResourceAccessLvl.Public.toString || result.mminfo.access == ResourceAccessLvl.Default.toString)
    }
  }

  /**
    * We're returning the basically only the abbrevs and remedyabbrevs of those chapters, that
    * match all search terms, but not necessarily within a single section.  So this is a lot more
    * superficial than @getSectionHits().
    *
    * @see getSectionHits
    */
  def getChapterHits(abbrev: String, searchTerms: SearchTerms, remedyAbbrev: Option[String]): List[(String, String)] = {
    //    // Create a string like "content like '%poo%' or content like '%moo%' or ..."
    //    def sqlSearchTerm(searchTerms: SearchTerms, column: String, booleanConnective: String) = {
    //      searchTerms.positive.map(term =>
    //        s"$column LIKE '%${term}%'"
    //      ).mkString(s" ${booleanConnective} ")
    //    }
    //
    //    run(quote {
    //      infix"""SELECT * FROM mmchapterview WHERE #${sqlSearchTerm(searchTerms, "chapter", "OR")}"""
    //        .as[Query[MMChapterView]]
    //    }).filter(_.isMatchFor(searchTerms)).map(chapter => (chapter.abbrev, chapter.remedyabbrev))
    List()
  }

  /**
    * Return those sections that have all the search terms matching within, and not just spread around
    * like in @getChapterHits().
    *
    * @see getChapterHits
    */
  def getSectionHits(abbrev: String, searchTerms: SearchTerms, page: Int, remedyFullName: Option[String]): Option[MMAllSearchResults] = {
    val cleanedUpAbbrev = abbrev.replaceAll("[^0-9A-Za-z\\-]", "")

    // Use of approximateSearchTerm is an oversimplification to narrow down the first
    // DB-lookup, which otherwise would return ALWAYS the entire repertory upon a symptom-search.
    val approximateSearchTerm = {
      if (searchTerms.exactPositiveOnly.length > 0)
        "%" + searchTerms.exactPositiveOnly.head.replaceAll("[\\*]", "%").toLowerCase + "%"
      else if (searchTerms.positive.length > 0)
        "%" + searchTerms.positive.sortBy(_.length)(Ordering[Int].reverse).head.replaceAll("[^A-Za-z0-9 äÄÜüÖöß\\-]", "").toLowerCase + "%"
      else
        ""
    }

    if (searchTerms.positive.length == 0 && remedyFullName.getOrElse("").trim.length == 0) {
      Logger.error(s"MMDao: Search aborted: no positive search terms or remedy entered.")
      return None
    }
    else if (searchTerms.positive.length + searchTerms.negative.length >= maxNumberOfSymptoms) {
      Logger.error(s"MMDao: Cannot enter more than ${maxNumberOfSymptoms} symptoms.")
      return None
    }
    else if (searchTerms.positive.length == 0 && searchTerms.negative.length > 0 && remedyFullName != None) {
      Logger.error(s"MMDao: Cannot search with just negative search terms.")
      return None
    }
    else if (searchTerms.positive.filter(_.length > 3).length == 0 && remedyFullName.getOrElse("").trim.length == 0) {
      Logger.error(s"MMDao: Search term(s) too short.")
      return None
    }
    else if (cleanedUpAbbrev.length == 0 || abbrev.length() > 15) {
      // Long mm-abbrevs smell like an SQL-injection attempt...
      Logger.error(s"MMDao: No materia medica abbrev with that name exists: ${abbrev}.")
      return None
    }

    val mmRemedies = getRemediesForMM(cleanedUpAbbrev)

    if (mmRemedies.length == 0) {
      Logger.error(s"MMDao: Backend unexpectedly returned no remedies for ${abbrev}.")
      return None
    }

    val rawResults = new Remedies(mmRemedies).getRemedyEntered(remedyFullName.getOrElse("")) match {
      case RemedyEntered(None, None) if (remedyFullName.getOrElse("").trim.length == 0) => {
        println(s"1: no remedy name given; approx. term: ${approximateSearchTerm}")
        run(quote {
          query[MMSection]
            .join(query[MMChapter]).on({ case (s, c) => s.mmchapter_id == c.id })
            .join(query[MMInfo]).on({ case ((s, c), i) => i.id == c.mminfo_id && i.abbrev == lift(abbrev) })
            .filter { case ((s, c), i) =>
              s.content.getOrElse("").toLowerCase.like(lift(s"%${approximateSearchTerm}%")) || s.heading.getOrElse("").toLowerCase.like(lift(s"%${approximateSearchTerm}%"))
            }
        }).collect { case ((s, c), i) if s.content.getOrElse("").trim.length > 0 && s.isMatchFor(searchTerms) => (s, c) }
      }
      case RemedyEntered(Some(id), _) if (approximateSearchTerm.length > 0) => {
        println("2: specific remedy ID given: " + id)
        run(quote {
          query[MMSection]
            .join(query[MMChapter]).on({ case (s, c) => s.mmchapter_id == c.id })
            .join(query[MMInfo]).on({ case ((s, c), i) => i.id == c.mminfo_id && i.abbrev == lift(abbrev) })
            .join(query[Remedy]).on({ case (((s,c), i), r) => r.id == c.remedy_id && r.id == lift(id) })
            .filter { case (((s, c), i), r) =>
              s.content.getOrElse("").toLowerCase.like(lift(s"%${approximateSearchTerm}%")) || s.heading.getOrElse("").toLowerCase.like(lift(s"%${approximateSearchTerm}%"))
            }
        }).collect { case (((s, c), i), r) if s.content.getOrElse("").trim.length > 0 && s.isMatchFor(searchTerms) => (s, c) }
      }
      case RemedyEntered(Some(id), _) if (approximateSearchTerm.length == 0) => {
        println("3: specific remedy ID given but no search terms: " + id)
        run(quote {
          query[MMSection]
            .join(query[MMChapter]).on({ case (s, c) => s.mmchapter_id == c.id })
            .join(query[MMInfo]).on({ case ((s, c), i) => i.id == c.mminfo_id && i.abbrev == lift(abbrev) })
            .join(query[Remedy]).on({ case (((s,c), i), r) => r.id == c.remedy_id && r.id == lift(id) })
            .filter(_ => true)
        }).collect { case (((s, c), i), r) => (s, c) }
      }
      case RemedyEntered(None, Some(lowerRemedyName)) if (approximateSearchTerm.length > 0) => {
        println("4: generic remedy name given: " + lowerRemedyName)
        run(quote {
          query[MMSection]
            .join(query[MMChapter]).on({ case (s, c) => s.mmchapter_id == c.id })
            .join(query[MMInfo]).on({ case ((s, c), i) => i.id == c.mminfo_id && i.abbrev == lift(abbrev) })
            .join(query[Remedy]).on({ case (((s,c), i), r) => r.id == c.remedy_id &&
            ( r.nameLong.toLowerCase.startsWith(lift(lowerRemedyName)) ||
              sql"""lower(array_to_string(${r.namealt}, ', '))""".as[String].like(lift(s"%${lowerRemedyName}%")) ) })
            .filter { case (((s, c), i), r) =>
              s.content.getOrElse("").toLowerCase.like(lift(s"%${approximateSearchTerm}%")) || s.heading.getOrElse("").toLowerCase.like(lift(s"%${approximateSearchTerm}%"))
            }
        }).collect { case (((s, c), i), r) if s.content.getOrElse("").trim.length > 0 && s.isMatchFor(searchTerms) => (s, c) }
      }
      case RemedyEntered(None, Some(lowerRemedyName)) if (approximateSearchTerm.length == 0) => {
        println("5: generic remedy name given but no search terms: " + lowerRemedyName)

        // TODO: This should also work, in theory, but doesn't in practice. Why?
        // implicit class SearchPgArray[T](left: List[String]) {
        //   def <~~ (right: String) = quote {
        //     infix"""( lower(array_to_string(${left}, ', ')) like '%${right}%' )""".pure.as[Boolean]
        //   }
        // }

        run(quote {
          query[MMSection]
            .join(query[MMChapter]).on({ case (s, c) => s.mmchapter_id == c.id })
            .join(query[MMInfo]).on({ case ((s, c), i) => i.id == c.mminfo_id && i.abbrev == lift(abbrev) })
            .join(query[Remedy]).on({ case (((s,c), i), r) => r.id == c.remedy_id &&
            ( r.nameLong.toLowerCase.startsWith(lift(lowerRemedyName)) ||
              sql"""lower(array_to_string(${r.namealt}, ', '))""".as[String].like(lift(s"%${lowerRemedyName}%")) ) })
            .filter(_ => true)
        }).collect { case (((s, c), i), r) => (s, c)}
      }
      case other => {
        println(s"6: remedyname '${remedyFullName.getOrElse("")}' could not be found in DB")
        return None
      }
    }

    val allFoundChapters = rawResults.map{ case (_,chap) => (chap.remedy_id, chap.heading) }
      .distinct
      .sortBy(_._2)

    // This value is for statistics only, sort of:
    // So we can show which chapters/remedies have the highest number of matching sections
    val allSectionsPerChapter = rawResults.map(_._2).distinct.map { chapter =>
      val sections = rawResults.collect { case (s, c) if c.remedy_id == chapter.remedy_id => s }
      HitsPerRemedy(sections.length, chapter.remedy_id)
    }

    val remediesOfResultPage =
      allFoundChapters
        .drop(page * maxNumberOfResultsPerMMPage) // Start on page <page>, where each page is maxNumberOfResultsPerMMPage long (so first page is obviously 0!!)
        .take(maxNumberOfResultsPerMMPage)

    val searchResultsForOnePageOnly =
      remediesOfResultPage.map({ case (remedy_id, remedy_fullname) =>
        MMSearchResult(abbrev, remedy_id, remedy_fullname, rawResults.collect { case (s, c) if c.remedy_id == remedy_id => s })
      })

    Some(MMAllSearchResults(searchResultsForOnePageOnly, allSectionsPerChapter))
  }

}
