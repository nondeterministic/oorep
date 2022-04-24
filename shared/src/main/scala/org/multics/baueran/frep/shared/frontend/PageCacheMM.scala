package org.multics.baueran.frep.shared.frontend

import org.multics.baueran.frep.shared.{MMAllSearchResults, ResultsCaseRubrics, ResultsRemedyStats}

import scala.collection.mutable

case class CachePageMM(abbrev: String,
                       symptom: String,
                       remedy: Option[String],
                       page: Int,
                       content: MMAllSearchResults)
{
  def sameMetaDataAs(thatCachePage: CachePageMM): Boolean =
    abbrev == thatCachePage.abbrev &&
      symptom == thatCachePage.symptom &&
      remedy == thatCachePage.remedy &&
      page == thatCachePage.page &&
      content.results.forall(thatCachePage.content.results.contains(_))
}

class PageCacheMM {
  private val _cacheMaxSize = 15
  private val _cache = mutable.ArrayBuffer[CachePageMM]()

  def addPage(page: CachePageMM): Unit = {
    // If page is already in the cache, push it merely to front of queue
    for (i <- 0 to _cache.length - 1) {
      _cache(i) match {
        case cachedPage: CachePageMM => {
          if (page.sameMetaDataAs(cachedPage)) {
            _cache.remove(i, 1)
            _cache.addOne(cachedPage)
            println(s"## Moved element (page ${page.page}) to front of queue: #${_cache.length}")
            return
          }
        }
      }
    }

    if (_cache.length >= _cacheMaxSize) {
      val oldPage = _cache(0).page // TODO: Remove this later in production code. Only for the println!
      _cache.remove(0,1)
      _cache.trimToSize()
      println(s"## Removed first element (page ${oldPage}) from full queue: #${_cache.length}")
    }

    _cache.addOne(page)
    println(s"## Added element (${page.abbrev}, ${page.symptom}, ${page.remedy}, p. ${page.page}) to queue: #${_cache.length}")
  }

  def getPage(abbrev: String,
              symptomQuery: String,
              page: Int,
              remedyQuery: Option[String]): Option[CachePageMM] = {
    for (i <- 0 to _cache.length - 1) {
      _cache(i) match {
        case cachedPage: CachePageMM => {
          if (abbrev == cachedPage.abbrev && symptomQuery == cachedPage.symptom && remedyQuery == cachedPage.remedy && page == cachedPage.page) {
            // Update cache before returning result
            _cache.remove(i, 1)
            _cache.addOne(cachedPage)

            println(s"## Getting result page ${cachedPage.page} from cache and moving it to front.")
            return Some(cachedPage)
          }
        }
      }
    }

    println("## Page NOT in cache.")
    None
  }

//  def getRemedies(abbrev: String,
//                  symptomQuery: String,
//                  remedyQuery: Option[String],
//                  remedyMinWeight: Int): List[ResultsRemedyStats] = {
//    for (i <- 0 to _cache.length - 1) {
//      _cache(i) match {
//        case CachePageMM(abr, symQu, remQu, _, remedies) => {
//          if (abr == abbrev && symQu == symptomQuery && remQu == remedyQuery) {
//            println(s"## Getting ${remedies.length} remedies from cache.")
//            return remedies
//          }
//        }
//      }
//    }
//
//    List.empty
//  }

  def length() = _cache.length

  def latest() = {
    if (length() > 0)
      Some(_cache.last)
    else
      None
  }
}
