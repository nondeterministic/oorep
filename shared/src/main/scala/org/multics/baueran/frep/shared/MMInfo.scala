package org.multics.baueran.frep.shared

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor}
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all._

import scala.collection.mutable

case class MMInfo(id: Int,
                  abbrev: String,
                  lang: Option[String],
                  fulltitle: Option[String],
                  authorlastname: Option[String],
                  authorfirstname: Option[String],
                  publisher: Option[String],
                  yearr: Int,
                  license: Option[String],
                  access: String,
                  displaytitle: Option[String])

object MMInfo {
  implicit val myDecoderMMInfo: Decoder[MMInfo] = deriveDecoder[MMInfo]
  implicit val myEncoderMMInfo: Encoder[MMInfo] = deriveEncoder[MMInfo]
}

case class MMAndRemedyIds(mminfo: MMInfo, remedy_ids: List[Int])

object MMAndRemedyIds {
  implicit val myDecoderMMSandRemedies: Decoder[MMAndRemedyIds] = deriveDecoder[MMAndRemedyIds]
  implicit val myEncoderMMSandRemedies: Encoder[MMAndRemedyIds] = deriveEncoder[MMAndRemedyIds]
}

case class MMSearchResult(abbrev: String, remedy_id: Int, remedy_fullname: String, result_sections: List[MMSection]) {

  def render(prefix: String, hideSections: Boolean, symptomString: String, materiaMedicas: MateriaMedicas, doLookup: (String, String, Option[Int], Option[String]) => Unit) = {

    def getTopMostSection() = result_sections.find(sec => sec.parent_sec_id == None)
    def getChildren(currSec: MMSection) = {
      result_sections.filter {
        _.parent_sec_id match {
          case Some(parentId) => parentId == currSec.id
          case None => false
        }
      }.sortBy(_.id)
    }

    def renderEntireChapterFromRoot(currSec: MMSection, indent: Int): scalatags.JsDom.TypedTag[dom.html.Div] = {
      val children = getChildren(currSec)

      if (children.length > 0) {
        if (indent == 0 && currSec.content != None)
          div(
            currSec.renderContent(),
            children.map(renderEntireChapterFromRoot(_, indent + 1))
          )
        else if (indent == 0 && currSec.content == None)
          div(
            children.map(renderEntireChapterFromRoot(_, indent + 1))
          )
        else
          div(
            currSec.render(prefix, hideSections, symptomString, indent),
            div(name:=s"parent_${currSec.id}", cls:=s"collapse ${if (hideSections) "hide" else "show"}", `type`:=s"${prefix}_mm_section_span",
              div(style:="padding-left:20px;", children.map(renderEntireChapterFromRoot(_, indent + 1)))
            )
          )
      }
      else
        currSec.render(prefix, hideSections, symptomString, indent)
    }

    val remedy = materiaMedicas.get(abbrev) match {
      case Some((_, remedies)) =>
        remedies.find(_.id == remedy_id)
      case None =>
        println(s"MMInfo: var. remedy in render(...'${remedy_fullname}'...) is None; this is a bug and should never have happened.")
        None
    }

    val remedyAltNamesAndLinksToAltMMs = {

      val linksToOtherMMs = {
        materiaMedicas.getAllBut(abbrev)
          .sortBy { case (mminfo, mmremedies) => mminfo.abbrev }
          .map {
            case (mminfo, mmremedies) if (remedy != None && mmremedies.exists(_.nameAbbrev == remedy.get.nameAbbrev)) =>
              Some(a(href:="#", onclick:={ (_: Event) => doLookup(mminfo.abbrev, "", None, Some(remedy.get.nameAbbrev)) }, mminfo.abbrev))
            case _ =>
              None
          }
          .flatten
      }

      val remedyAltNameStrng = {
        remedy match {
          case Some(remedy) =>
            if (remedy.nameLong.toLowerCase != remedy_fullname.toLowerCase) {
              if (remedy.namealt.length > 0) {
                val altNames = remedy.namealt.filter(_.toLowerCase != remedy_fullname.toLowerCase)

                if (altNames.length > 0)
                  "[alt. " + remedy.nameLong + ", " + altNames.mkString(", ")
                else
                  "[alt. " + remedy.nameLong
              }
              else
                "[alt. " + remedy.nameLong
            }
            else {
              if (remedy.namealt.length > 0)
                "[alt. " + remedy.namealt.mkString(", ")
              else
                ""
            }
          case None => ""
        }
      }

      if (remedyAltNameStrng.length > 0 && linksToOtherMMs.length > 1)
        span(remedyAltNameStrng + " | see also ", linksToOtherMMs.tail.foldLeft(span(linksToOtherMMs.head))(span(_, ", ", _)), span("]"))
      else if (remedyAltNameStrng.length > 0 && linksToOtherMMs.length == 1)
        span(remedyAltNameStrng + " | see also ", span(linksToOtherMMs.head), span("]"))
      else if (remedyAltNameStrng.length > 0 && linksToOtherMMs.length == 0)
        span(remedyAltNameStrng + "]")
      else if (remedyAltNameStrng.length == 0 && linksToOtherMMs.length > 1)
        span("[see also ", linksToOtherMMs.tail.foldLeft(span(linksToOtherMMs.head))(span(_, ", ", _)), span("]"))
      else if (remedyAltNameStrng.length == 0 && linksToOtherMMs.length == 1)
        span("[see also ", span(linksToOtherMMs.head), span("]"))
      else
        span()
    }

    div(cls:="card", style:="margin-bottom:20px;",
      div(cls:="card-header",
        div(cls:="container-fluid",
          div(cls:="row",
            div(cls := "col", style := "padding:0; margin:0;",
              remedy match {
                case Some(remedy) =>
                  b(a(href := "#", onclick := { (_: Event) => doLookup(abbrev, "", None, Some(remedy.nameAbbrev)) }, s"""${remedy_fullname.toUpperCase} (${remedy.nameAbbrev})"""))
                case None =>
                  b(a(href := "#", onclick := { (_: Event) => doLookup(abbrev, "", None, Some(remedy_fullname)) }, s"""${remedy_fullname.toUpperCase}"""))
              }
            ),
            div(cls := "col-2",
              " "
            ),
            div(cls := "col", style := "text-align:right; padding:0; margin:0;",
              remedyAltNamesAndLinksToAltMMs
            )
          )
        )
      ),
      div(cls:="card-body",
        // Some symptom search...
        if (getTopMostSection() == None) {
          result_sections.sortBy(_.id).map(_.render(prefix, hideSections, symptomString, 0))
        }
        // Displaying a remedy chapter in its entirety...
        else
          renderEntireChapterFromRoot(getTopMostSection().get, 0)
      )
    )
  }

  override def equals(that: Any): Boolean = {
    that match {
      case that: MMSearchResult => {
        that.isInstanceOf[MMSearchResult] &&
          that.abbrev == abbrev &&
          that.remedy_id == remedy_id &&
          that.remedy_fullname == remedy_fullname &&
          that.result_sections.forall(thatSec =>
            result_sections.exists(_ == thatSec)
          )
      }
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 31
    (prime * abbrev.hashCode + remedy_id + result_sections.foldLeft(0)(_ + _.id) + remedy_fullname.hashCode) * prime
  }

}

object MMSearchResult {
  implicit val myDecoder: Decoder[MMSearchResult] = deriveDecoder[MMSearchResult]
  implicit val myEncoder: Encoder[MMSearchResult] = deriveEncoder[MMSearchResult]
}

case class HitsPerRemedy(hits: Int, remedyId: Int)

object HitsPerRemedy {
  implicit val myDecoder: Decoder[HitsPerRemedy] = deriveDecoder[HitsPerRemedy]
  implicit val myEncoder: Encoder[HitsPerRemedy] = deriveEncoder[HitsPerRemedy]
}

case class MMAllSearchResults(results: List[MMSearchResult], numberOfMatchingSectionsPerChapter: List[HitsPerRemedy]) {

  override def equals(that: Any): Boolean = {
    that match {
      case that: MMAllSearchResults => {
        that.isInstanceOf[MMAllSearchResults] &&
          that.results.forall(result => results.exists(_ == result)) &&
          that.numberOfMatchingSectionsPerChapter.forall(tuple => numberOfMatchingSectionsPerChapter.exists(_ == tuple))
      }
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 57
    (prime * results.foldLeft(1)(_ + _.hashCode) + numberOfMatchingSectionsPerChapter.foldLeft(1)((number, hitsPerChapter) => number + hitsPerChapter.hits + hitsPerChapter.remedyId) * prime)
  }

}

object MMAllSearchResults {
  implicit val myDecoder: Decoder[MMAllSearchResults] = deriveDecoder[MMAllSearchResults]
  implicit val myEncoder: Encoder[MMAllSearchResults] = deriveEncoder[MMAllSearchResults]
}


case class MMChapter(id: Int,
                     mminfo_id: Int,
                     heading: String,
                     remedy_id: Int)
