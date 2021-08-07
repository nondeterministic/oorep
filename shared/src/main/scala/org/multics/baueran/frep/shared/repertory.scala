package org.multics.baueran.frep.shared

import org.multics.baueran.frep.shared.Defs.ResourceAccessLvl
import ResourceAccessLvl._

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.HCursor

// ------------------------------------------------------------------------------------------------------------------
case class Info(abbrev: String, title: String, languag: String,
                authorLastName: Option[String], authorFirstName: Option[String],
                yearr: Option[Int], publisher: Option[String], license: Option[String],
                edition: Option[String], access: ResourceAccessLvl, displaytitle: Option[String])

object Info {
  implicit val infoDecoder: Decoder[Info] = new Decoder[Info] {
    final def apply(c: HCursor): Decoder.Result[Info] = {
      val abbrev = c.downField("abbrev").as[String].getOrElse("ERROR")
      val title = c.downField("title").as[String].getOrElse("ERROR")
      val language = c.downField("language").as[String].getOrElse("ERROR")
      val authorLastName = c.downField("authorLastName").as[String] match {
        case Right(name) => Some(name)
        case Left(_) => None
      }
      val authorFirstName = c.downField("authorFirstName").as[String] match {
        case Right(name) => Some(name)
        case Left(_) => None
      }
      val yearr = c.downField("year").as[Int] match {
        case Right(yearr) => Some(yearr)
        case Left(_) => None
      }
      val publisher = c.downField("publisher").as[String] match {
        case Right(publisher) => Some(publisher)
        case Left(_) => None
      }
      val license = c.downField("license").as[String] match {
        case Right(license) => Some(license)
        case Left(_) => None
      }
      val edition = c.downField("edition").as[String] match {
        case Right(edition) => Some(edition)
        case Left(_) => None
      }
      val access= c.downField("access").as[String] match {
        case Right(access) => ResourceAccessLvl.withName(access)
        case Left(_) => ResourceAccessLvl.Private // In case of doubt: make it private!
      }
      val displaytitle = c.downField("displaytitle").as[String] match {
        case Right(displaytitle) => Some(displaytitle)
        case Left(_) => None
      }

      val result = Info(abbrev, title, language,
        authorLastName,
        authorFirstName,
        yearr,
        publisher,
        license,
        edition,
        access,
        displaytitle)
      Right(result)
    }
  }

  implicit val infoEncoder: Encoder[Info] = new Encoder[Info] {
    def apply(i: Info): Json = Json.obj(
      ("abbrev", Json.fromString(i.abbrev)),
      ("title", Json.fromString(i.title)),
      ("language", Json.fromString(i.languag)),
      ("authorLastName", i.authorLastName match {
        case Some(lastName) => Json.fromString(lastName)
        case None => Json.Null
      }),
      ("authorFirstName", i.authorFirstName match {
        case Some(firstName) => Json.fromString(firstName)
        case None => Json.Null
      }),
      ("year", i.yearr match {
        case Some(year) => Json.fromInt(year)
        case None => Json.Null
      }),
      ("publisher", i.publisher match {
        case Some(publisher) => Json.fromString(publisher)
        case None => Json.Null
      }),
      ("license", i.license match {
        case Some(license) => Json.fromString(license)
        case None => Json.Null
      }),
      ("edition", i.edition match {
        case Some(edition) => Json.fromString(edition)
        case None => Json.Null
      }),
      ("access", Json.fromString(i.access.toString)),
      ("displaytitle", i.displaytitle match {
        case Some(displaytitle) => Json.fromString(displaytitle)
        case None => Json.Null
      })
    )
  }
}

// ------------------------------------------------------------------------------------------------------------------
case class Chapter(abbrev: String, id: Int, text: String)

object Chapter {
  implicit val chapterDecoder: Decoder[Chapter] = deriveDecoder[Chapter]
  implicit val chapterEncoder: Encoder[Chapter] = deriveEncoder[Chapter]
}

// ------------------------------------------------------------------------------------------------------------------
case class RubricRemedy(abbrev: String, rubricId: Int, remedyId: Int, weight: Int, chapterId: Int)

object RubricRemedy {
  implicit val rRemedyDecoder: Decoder[RubricRemedy] = deriveDecoder[RubricRemedy]
  implicit val rRemedyEncoder: Encoder[RubricRemedy] = deriveEncoder[RubricRemedy]
}

// ------------------------------------------------------------------------------------------------------------------
case class Remedy(id: Int, nameAbbrev: String, nameLong: String, namealt: List[String]) {
  def canEqual(a: Any) = a.isInstanceOf[Remedy]

  override def equals(that: Any) = {
    that match {
      case that: Remedy =>
        this.canEqual(that) &&
          that.id == this.id &&
          that.id == this.id &&
          that.nameAbbrev == this.nameAbbrev &&
          that.nameLong == this.nameLong &&
          that.namealt.forall(na => this.namealt.exists(_ == na))
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + id +
      (if (nameAbbrev == null) 0 else nameAbbrev.hashCode()) +
      (if (nameLong == null) 0 else nameLong.hashCode()) +
      namealt.foldLeft(0)(_.hashCode() + _.hashCode)
    result
  }

}

object Remedy {
  implicit val remedyDecoder: Decoder[Remedy] = deriveDecoder[Remedy]
  implicit val remedyEncoder: Encoder[Remedy] = deriveEncoder[Remedy]
}

// ------------------------------------------------------------------------------------------------------------------
case class Rubric(abbrev: String, id: Int, mother: Option[Int], isMother: Option[Boolean],
                  chapterId: Int, fullPath: String, path: Option[String], textt: Option[String])
{
  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result +
      abbrev.hashCode() +
      id +
      (if (mother == None) 0 else mother.get) +
      (if (isMother == None) 0 else 1) +
      chapterId +
      fullPath.hashCode() +
      (if (path == None) 0 else path.get.hashCode()) +
      (if (textt == None) 0 else textt.get.hashCode())
    result * prime
  }

  def canEqual(a: Any) = a.isInstanceOf[Rubric]

  override def equals(that: Any) = {
    that match {
      case that: Rubric =>
        this.canEqual(that) &&
          that.abbrev == this.abbrev &&
          that.id == this.id &&
          that.mother == this.mother &&
          that.isMother == this.isMother &&
          that.chapterId == this.chapterId &&
          that.fullPath == this.fullPath &&
          that.path == this.path &&
          that.textt == this.textt
      case _ => false
    }
  }

  /**
   * Checks if rubric matches all words in posStrings, so long as it doesn't match a word in negStrings.
   */
  def isMatchFor(searchTerms: SearchTerms, caseSensitive: Boolean = false): Boolean = {
    def isExpressionInText(expression: String, caseSensitive: Boolean) = searchTerms.isExpressionInTextPassage(expression, textt, caseSensitive)
    def isExpressionInPath(expression: String, caseSensitive: Boolean) = searchTerms.isExpressionInTextPassage(expression, path, caseSensitive)
    def isExpressionInFullPath(expression: String, caseSensitive: Boolean) = searchTerms.isExpressionInTextPassage(expression, Some(fullPath), caseSensitive)

    if (searchTerms.positive.length == 0)
      return false

    val isPosMatch = searchTerms.positive.map(word =>
      isExpressionInText(word, caseSensitive) || isExpressionInPath(word, caseSensitive) || isExpressionInFullPath(word, caseSensitive))
      .foldLeft(true) { (x, y) => x && y }

    if (isPosMatch && searchTerms.negative.length > 0) {
      searchTerms.negative.map(word =>
        !isExpressionInText(word, caseSensitive) && !isExpressionInPath(word, caseSensitive) && !isExpressionInFullPath(word, caseSensitive))
          .foldLeft(true) { (x, y) => x && y }
    }
    else
      isPosMatch
  }

}

object Rubric {
  implicit val rubricDecoder: Decoder[Rubric] = deriveDecoder[Rubric]
  implicit val rubricEncoder: Encoder[Rubric] = deriveEncoder[Rubric]
}

// ------------------------------------------------------------------------------------------------------------------
case class InfoExtended(info: Info, remedyIds: List[Int])
object InfoExtended {
  implicit val infoExtendedDecoder: Decoder[InfoExtended] = deriveDecoder[InfoExtended]
  implicit val infoExtendedEncoder: Encoder[InfoExtended] = deriveEncoder[InfoExtended]
}
