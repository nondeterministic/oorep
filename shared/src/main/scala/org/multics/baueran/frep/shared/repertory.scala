package org.multics.baueran.frep.shared

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.HCursor

// ------------------------------------------------------------------------------------------------------------------
object RepAccess extends Enumeration {
  type RepAccess = Value
  val Default = Value("Default")
  val Public = Value("Public")
  val Protected = Value("Protected")
  val Private = Value("Private")
}
import RepAccess._

// ------------------------------------------------------------------------------------------------------------------
case class Info(abbrev: String, title: String, languag: String,
                authorLastName: Option[String], authorFirstName: Option[String],
                yearr: Option[Int], publisher: Option[String], license: Option[String],
                edition: Option[String], access: RepAccess)

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
        case Right(access) => RepAccess.withName(access)
        case Left(_) => RepAccess.Private // In case of doubt: make it private!
      }

      val result = Info(abbrev, title, language,
        authorLastName,
        authorFirstName,
        yearr,
        publisher,
        license,
        edition,
        access)
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
      ("access", Json.fromString(i.access.toString))
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
case class Remedy(abbrev: String, val id: Int, val nameAbbrev: String, val nameLong: String) {
  def canEqual(a: Any) = a.isInstanceOf[Remedy]

  override def equals(that: Any) = {
    that match {
      case r: Remedy => r.canEqual(this) && r.hashCode() == this.hashCode()
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + id +
      (if (nameAbbrev == null) 0 else nameAbbrev.hashCode()) +
      (if (abbrev == null) 0 else abbrev.hashCode()) +
      (if (nameLong == null) 0 else nameLong.hashCode())
    return result
  }

}

object Remedy {
  implicit val remedyDecoder: Decoder[Remedy] = deriveDecoder[Remedy]
  implicit val remedyEncoder: Encoder[Remedy] = deriveEncoder[Remedy]
}

// ------------------------------------------------------------------------------------------------------------------
case class ChapterRemedy(abbrev: String, remedyId: Int, chapterId: Int)

object ChapterRemedy {
  implicit val cRemedyDecoder: Decoder[ChapterRemedy] = deriveDecoder[ChapterRemedy]
  implicit val cRemedyEncoder: Encoder[ChapterRemedy] = deriveEncoder[ChapterRemedy]
}

// ------------------------------------------------------------------------------------------------------------------
case class Rubric(abbrev: String, id: Int, mother: Option[Int], isMother: Option[Boolean],
                  chapterId: Int, fullPath: String, path: Option[String], textt: Option[String])
{
  def canEqual(a: Any) = a.isInstanceOf[Rubric]

  override def equals(that: Any) = {
    that match {
      case r: Rubric => r.canEqual(this) && r.hashCode() == this.hashCode()
      case _ => false
    }
  }

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
    return result
  }

  /**
   * Looks for a word, word, within some other text passage, x, where
	 * x is usually either fullPath, path or text of Rubric.
   */
  private def isWordInX(word: String, x: Option[String], caseSensitive: Boolean = false): Boolean = {
    x match {
      case None => false
      case Some(x) => {
        var wordMod = word
        var xMod = x

        if (!caseSensitive) {
          wordMod = word.toLowerCase
          xMod = x.toLowerCase
        }

        val searchSpace = xMod.replaceAll("[^A-Za-z0-9 \\-]", "").split(" ")

        if (wordMod.contains("*")) {
          // If there's no * at beginning of search term, add ^, so that "urin*" doesn't
          // match "during" (unless you want to, in which case you'd write "*urin*").
          if (!wordMod.startsWith("*"))
            wordMod = "^" + wordMod

          val searchPattern = wordMod.replaceAll("\\*", ".*").r
          searchSpace.filter(searchPattern.findFirstMatchIn(_).isDefined).length > 0
        }
        else
          searchSpace.contains(wordMod)
      }
    }
  }

  /**
   * Checks if rubric matches all words in posStrings, so long as it doesn't match a word in negStrings.
   */
  def isMatchFor(posStrings: List[String], negStrings: List[String], caseSensitive: Boolean = false): Boolean = {
    def isWordInText(word: String, caseSensitive: Boolean) = isWordInX(word, textt, caseSensitive)
    def isWordInPath(word: String, caseSensitive: Boolean) = isWordInX(word, path, caseSensitive)
    def isWordInFullPath(word: String, caseSensitive: Boolean) = isWordInX(word, Some(fullPath), caseSensitive)

    if (posStrings.length == 0)
      return false

    val isPosMatch = posStrings.map(word => 
      isWordInText(word, caseSensitive) || isWordInPath(word, caseSensitive) || isWordInFullPath(word, caseSensitive))
      .foldLeft(true) { (x, y) => x && y }

    if (negStrings.length > 0 && isPosMatch) {
      negStrings.map(word => 
        !isWordInText(word, caseSensitive) && !isWordInPath(word, caseSensitive) && !isWordInFullPath(word, caseSensitive))
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
// Do not use this class any more!  Its purpose is for testing and repertory import/conversion only.
// Repertory accesses for rubric lookups etc. should be made via RepertoryDao only!
case class Repertory(val info: Info, val chapters: Seq[Chapter], val remedies: Seq[Remedy],
                val chapterRemedies: Seq[ChapterRemedy], val rubrics: Seq[Rubric],
                val rubricRemedies: Seq[RubricRemedy]) 
{

  @deprecated("Use RepertoryDao.lookupSymptom() instead.","24-02-2019")
  def findRubrics(enteredSearchString: String, caseSensitive: Boolean = false): Seq[Rubric] = {
    val searchStrings = enteredSearchString.
                          trim.                               // Remove trailing spaces
                          replaceAll(" +", " ").              // Remove double spaces
                          replaceAll("[^A-Za-z0-9 \\-*]", "").// Remove all but alphanum-, wildcard-, minus-symbols
                          split(" ")                          // Get list of search strings

    val posSearchTerms = searchStrings.filter(!_.startsWith("-")).toList
    val negSearchTerms = searchStrings.filter(_.startsWith("-")).map(_.substring(1)).toList
    rubrics.filter(_.isMatchFor(posSearchTerms, negSearchTerms, caseSensitive))
  }

}
