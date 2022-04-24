package org.multics.baueran.frep.shared.frontend

import org.multics.baueran.frep.shared.{ResultsCaseRubrics, ResultsRemedyStats}

import scala.collection.mutable

case class CachePage(abbrev: String,
                     symptom: String,
                     remedy: Option[String],
                     minWeight: Int,
                     content: ResultsCaseRubrics,
                     remedies: List[ResultsRemedyStats])
{
  def sameMetaDataAs(page: CachePage): Boolean =
    abbrev == page.abbrev &&
      symptom == page.symptom &&
      remedy == page.remedy &&
      minWeight == page.minWeight &&
      content.currPage == page.content.currPage
}

class PageCache {
  private val _cacheMaxSize = 15
  private val _cache = mutable.ArrayBuffer[CachePage]()

  def addPage(page: CachePage): Unit = {
    // If page is already in the cache, push it merely to front of queue
    for (i <- 0 to _cache.length - 1) {
      _cache(i) match {
        case cachedPage: CachePage => {
          if (page.sameMetaDataAs(cachedPage)) {
            _cache.remove(i, 1)
            _cache.addOne(cachedPage)
            println(s"## Moved element (page ${page.content.currPage}) to front of queue: #${_cache.length}")
            return
          }
        }
      }
    }

    if (_cache.length >= _cacheMaxSize) {
      val oldPage = _cache(0).content.currPage // TODO: Remove this later in production code. Only for the println!
      _cache.remove(0,1)
      _cache.trimToSize()
      println(s"## Removed first element (page ${oldPage}) from full queue: #${_cache.length}")
    }

    _cache.addOne(page)
    println(s"## Added element (${page.abbrev}, ${page.symptom}, ${page.remedy}, w. ${page.minWeight}, p. ${page.content.currPage}) to queue: #${_cache.length}")
  }

  def getPage(abbrev: String,
              symptomQuery: String,
              remedyQuery: Option[String],
              remedyMinWeight: Int,
              page: Int): Option[CachePage] = {
    for (i <- 0 to _cache.length - 1) {
      _cache(i) match {
        case cachedPage: CachePage => {
          if (abbrev == cachedPage.abbrev && symptomQuery == cachedPage.symptom && remedyQuery == cachedPage.remedy && remedyMinWeight == cachedPage.minWeight && page == cachedPage.content.currPage) {
            // Update cache before returning result
            _cache.remove(i, 1)
            _cache.addOne(cachedPage)

            println(s"## Getting result page ${cachedPage.content.currPage} from cache and moving it to front.")
            return Some(cachedPage)
          }
        }
      }
    }

    println("## Page NOT in cache.")
    None
  }

  def getRemedies(abbrev: String,
                  symptomQuery: String,
                  remedyQuery: Option[String],
                  remedyMinWeight: Int): List[ResultsRemedyStats] = {
    for (i <- 0 to _cache.length - 1) {
      _cache(i) match {
        case CachePage(abr, symQu, remQu, remMWeight, _, remedies) => {
          if (abr == abbrev && symQu == symptomQuery && remQu == remedyQuery && remedyMinWeight == remMWeight) {
            println(s"## Getting ${remedies.length} remedies from cache.")
            return remedies
          }
        }
      }
    }

    List.empty
  }

  def size() = _cache.size

  def latest(): Option[CachePage] = {
    if (size() > 0)
      Some(_cache.last)
    else
      None
  }

}
