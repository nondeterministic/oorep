package org.multics.baueran.frep.shared.frontend

import org.multics.baueran.frep.shared.RepertoryBrowseRubric
import scala.collection.mutable
import scala.util.boundary, boundary.break

case class BrowseChapterCachePage(abbrev: String, chapterId: Int, rubrics: List[RepertoryBrowseRubric])

class RepertoryBrowseCache {
  private val _cacheMaxSize = 30
  private val _cache = mutable.ArrayBuffer[BrowseChapterCachePage]()

  def addPage(page: BrowseChapterCachePage): Unit = {
    boundary:
      for (i <- _cache.indices) {
        val cachedPage = _cache(i)
        if (cachedPage.abbrev == page.abbrev && cachedPage.chapterId == page.chapterId) {
          _cache.remove(i)
          _cache.addOne(page)
          println(s"## Moved browse chapter (${page.abbrev}, ${page.chapterId}) to front of queue: #${_cache.length}")
          break()
        }
      }

      if (_cache.length >= _cacheMaxSize) {
        val old = _cache(0)
        _cache.remove(0)
        _cache.trimToSize()
        println(s"## Removed first browse chapter (${old.abbrev}, ${old.chapterId}) from full queue: #${_cache.length}")
      }

      _cache.addOne(page)
      println(s"## Added browse chapter (${page.abbrev}, ${page.chapterId}) to queue: #${_cache.length}")
  }

  def getPage(abbrev: String, chapterId: Int): Option[BrowseChapterCachePage] = {
    boundary:
      for (i <- _cache.indices) {
        val cachedPage = _cache(i)
        if (cachedPage.abbrev == abbrev && cachedPage.chapterId == chapterId) {
          _cache.remove(i)
          _cache.addOne(cachedPage)
          println(s"## Getting browse chapter (${cachedPage.abbrev}, ${cachedPage.chapterId}) from cache and moving it to front.")
          break(Some(cachedPage))
        }
      }

      println(s"## Browse chapter (${abbrev}, ${chapterId}) NOT in cache.")
      None
  }

  def contains(abbrev: String, chapterId: Int): Boolean =
    _cache.exists(p => p.abbrev == abbrev && p.chapterId == chapterId)

  def size(): Int = _cache.length

  def nonEmpty: Boolean = _cache.nonEmpty
}
