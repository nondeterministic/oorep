package org.multics.baueran.frep.shared

import io.circe.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*

import scala.util.control.Breaks.{break, breakable}

import org.multics.baueran.frep.shared.HttpRequest2

case class WeightedRemedy(remedy: Remedy, weight: Int) {
  def canEqual(a: Any) = a.isInstanceOf[WeightedRemedy]

  override def equals(that: Any): Boolean = {
    that match {
      case that: WeightedRemedy =>
        this.canEqual(that) &&
          that.remedy == this.remedy &&
          that.weight == this.weight
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 37
    var result = 1
    result = prime * result +
      (if (remedy == null) 0 else remedy.hashCode()) +
      weight
    result = prime * result +
      (if (remedy == null) 0 else remedy.hashCode()) +
      weight
    result * 11
  }
}

object WeightedRemedy {

  // TODO: This seems a bit stupid: I provide a custom keyDe/Encoder, which
  // uses the derived Remedy de/encoder.  Is this really necessary?

  implicit val keyRemedyDecoder: KeyDecoder[Remedy] = new KeyDecoder[Remedy] {
    def apply(key: String): Option[Remedy] = {
      parse(key) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[Remedy] match {
            case Right(result) => Some(Remedy(result.id, result.nameAbbrev, result.nameLong, result.namealt))
            case Left(_) => None
          }
        }
        case Left(_) => None
      }
    }
  }

  implicit val keyRemedyEncoder: KeyEncoder[Remedy] = new KeyEncoder[Remedy] {
    override def apply(remedy: Remedy): String = {
      remedy.asJson.toString()
    }
  }

  implicit val wrEncoder: Encoder[WeightedRemedy] = deriveEncoder[WeightedRemedy]
  implicit val wrDecoder: Decoder[WeightedRemedy] = deriveDecoder[WeightedRemedy]

}

case class CazeSubRubric(id: Int, rubric: Rubric, weightedRemedies: List[WeightedRemedy]) {

  def containsRemedyAbbrev(remedyAbbrev: String): Boolean =
    weightedRemedies.exists(_.remedy.nameAbbrev == remedyAbbrev)

  def getRemedyWeight(remedyAbbrev: String): Int = {
    weightedRemedies.filter(_.remedy.nameAbbrev == remedyAbbrev) match {
      case remedy :: Nil => remedy.weight
      case _ => 0
    }
  }

  def checkEquality(that: Any): Boolean = {
    that match {
      case that: CazeSubRubric => rubric.equals(that) && (weightedRemedies diff that.weightedRemedies).isEmpty
      case _ => false
    }
  }

}

object CazeSubRubric {

  implicit val csrencoder: Encoder[CazeSubRubric] = deriveEncoder[CazeSubRubric]
  implicit val csrdecoder: Decoder[CazeSubRubric] = deriveDecoder[CazeSubRubric]

}

case class CazeRubricJsonHelper(cazeRubricId: Int, cazeId: Int, subRubrics: List[(Int, String, Int)])

object CazeRubricJsonHelper {
  implicit val cazeRubricJsonHelperDecoder: Decoder[CazeRubricJsonHelper] = deriveDecoder[CazeRubricJsonHelper]
  implicit val cazeRubricJsonHelperEncoder: Encoder[CazeRubricJsonHelper] = deriveEncoder[CazeRubricJsonHelper]
}

case class CazeRubric(id: Int,
                      cazeId: Int,
                      subRubrics: List[CazeSubRubric],
                      var rubricWeight: Int,
                      var rubricLabel: Option[String])
{
  object VarHandling extends Enumeration {
    type VarHandling = Value
    val Equal, NotEqual, Ignore = Value
  }

  import VarHandling._

  // Get a unique ID (i.e. list of subrubrics) which can later be used to access the individual subrubrics of a (merged) rubric/row.
  // (The counterpart to fromJson() below in the support object)
  def toJson(): Json =
    CazeRubricJsonHelper(id, cazeId, subRubrics.map(sr => (sr.id, sr.rubric.abbrev, sr.rubric.id))).asJson

  // This is really only used in CaseSection.scala as follows:
  //     def getId() = HtmlRepresentation.getId() + "_crub_" + crub.toString()
  // to give each checkbox a unique ID.
  override def toString(): String = toJson().toString().filter(_.isDigit)

  def getAllRemedies: List[Remedy] =
    subRubrics.flatMap(_.weightedRemedies.map(_.remedy))

  def fullPath: String =
    subRubrics.map(_.rubric.fullPath).mkString(", ")

  def path: String =
    subRubrics.map(_.rubric.path).mkString(", ")

  def textt: String =
    subRubrics.map(_.rubric.textt).mkString(", ")

  def abbrev: String =
    subRubrics.map(_.rubric.abbrev).mkString(", ")

  def canEqual(a: Any): Boolean = a.isInstanceOf[CazeRubric]

  def containsRemedyAbbrev(remedyAbbrev: String): Boolean =
    subRubrics.exists(_.containsRemedyAbbrev(remedyAbbrev))

  /** Returns highest remedy weight of a remedy's occurrence in all subrubrics **/

  def getHighestRemedyWeight(remedyAbbrev: String): Int =
    subRubrics.map(_.getRemedyWeight(remedyAbbrev)).max

  def containsSubRubric(subRubricId: Int, subRubricAbbrev: String): Boolean = {
    subRubrics.exists(sr => sr.id == subRubricId && sr.rubric.abbrev == subRubricAbbrev)
  }

  private def checkEquality(handlingOfVars: VarHandling, that: Any): Boolean = {
    that match {
      case that: CazeRubric =>
        this.canEqual(that) &&
          (this.subRubrics diff that.subRubrics).isEmpty &&
          (handlingOfVars match {
            case Equal => s"${that.rubricWeight}".toLong == s"${this.rubricWeight}".toLong &&
              that.rubricLabel.getOrElse("").toLowerCase.reverse == this.rubricLabel.getOrElse("").toLowerCase.reverse
            case NotEqual => s"${that.rubricWeight}".toLong != s"${this.rubricWeight}".toLong ||
              that.rubricLabel.getOrElse("").toLowerCase.reverse != this.rubricLabel.getOrElse("").toLowerCase.reverse
            case _ => true
          })
      case _ => false
    }
  }

  override def equals(that: Any): Boolean = checkEquality(Equal, that)

  /**
    * Like equals, but without comparing weight
    */
  def equalsIgnoreWeight(that: Any): Boolean = checkEquality(Ignore, that)

  /**
    * Like equals, but weight must be unequal.  That is, all is the same except for the weight.
    */
  def equalsExceptWeight(that: Any): Boolean = checkEquality(NotEqual, that)

  override def hashCode: Int = {
    val prime = 7
    var result = 1
    result = prime * result +
      subRubrics.map(_.hashCode).sum +
      rubricWeight +
      rubricLabel.getOrElse("").hashCode
    result * prime
  }

}

object CazeRubric {

  def decode(jsonCaseRubric: String): Option[CazeRubric] = {
    io.circe.parser.parse(jsonCaseRubric) match {
      case Right(json) => json.hcursor.as[CazeRubric] match {
        case Right(c) => Some(c)
        case Left(err) => None
      }
      case Left(err) => None
    }
  }

  def decodeList(jsonCaseRubric: String): Option[List[CazeRubric]] = {
    io.circe.parser.parse(jsonCaseRubric) match {
      case Right(json) => json.hcursor.as[List[CazeRubric]] match {
        case Right(c) => Some(c)
        case Left(err) => None
      }
      case Left(err) => None
    }
  }

  implicit val crencoder: Encoder[CazeRubric] = deriveEncoder[CazeRubric]
  implicit val crdecoder: Decoder[CazeRubric] = deriveDecoder[CazeRubric]

  // The counterpart to toJson() above
  def fromJson(jsonEncodedStrings: List[String]): List[(Int, String, Int)] = {
    jsonEncodedStrings.collect(
      parse(_) match {
        case Right(json) =>
          val cursor = json.hcursor
          cursor.as[Seq[(Int, String, Int)]] match {
            case Right(Seq(results)) => {
              results
            }
          }
      }
    )
  }
}

/**
  * This class is merely used/necessary in order to transmit lookup results of a repertory
  */
case class ResultsCazeRubrics(totalNumberOfRepertoryRubrics: Int, totalNumberOfResults: Int, totalNumberOfPages: Int, currPage: Int, results: List[CazeRubric])

object ResultsCazeRubrics {
  implicit val encoder: Encoder[ResultsCazeRubrics] = deriveEncoder[ResultsCazeRubrics]
  implicit val decoder: Decoder[ResultsCazeRubrics] = deriveDecoder[ResultsCazeRubrics]
}

case class ResultsRemedyStats(nameabbrev: String, count: Int, cumulativeweight: Int)

object ResultsRemedyStats {
  implicit val encoder: Encoder[ResultsRemedyStats] = deriveEncoder[ResultsRemedyStats]
  implicit val decoder: Decoder[ResultsRemedyStats] = deriveDecoder[ResultsRemedyStats]
}

case class Caze(id: Int,
                header: String,
                member_id: Int,
                date: String,
                changed: String,
                description: String,
                var rubrics: List[CazeRubric] = Nil)
{
  private def getRubricsFromDb(caseId: Int, memberId: Int): Unit = {
    HttpRequest2("sec/caserubrics")
      .withQueryParameters("caseId" -> caseId.toString, "memberId" -> memberId.toString)
      .onSuccess((response: String) => {
        parse(response) match {
          case Right(json) => {
            val cursor = json.hcursor
            cursor.as[List[CazeRubric]] match {
              case Right(cazerubrics) => {
                rubrics = cazerubrics
              }
              case Left(err) => println("Decoding of case failed: " + err)
            }
          }
          case Left(err) => println("Parsing of case (is it JSON?): " + err)
        }
      })
      .send()
  }

  def loadRubricsFromDb(): Unit = getRubricsFromDb(id, member_id)

  def canEqual(a: Any) = a.isInstanceOf[Caze]

  // Ignore id and date on purpose. Id is DB-generated and two same Cazes with different id should be treated as equal!
  override def equals(that: Any): Boolean = {
    that match {
      case that: Caze => this.canEqual(that) &&
        that.header == this.header &&
        that.member_id == this.member_id &&
        that.description == this.description &&
        that.rubrics.length == this.rubrics.length &&
        (that.rubrics diff this.rubrics).isEmpty
      case _ => false
    }
  }

  // Ignore id and date on purpose. Id is DB-generated and two same Cazes with different id should be treated as equal!
  override def hashCode: Int = {
    val prime = 31
    var result = rubrics.toString().hashCode
    result = prime * result +
      (if (header == null) 0 else header.hashCode()) +
      member_id +
      (if (description == null) 0 else description.hashCode()) +
      rubrics.map(_.hashCode()).fold(0)(_ + _)
    result * 23
  }

  /**
    * Returns a non-empty list of additional case rubrics not contained in *that*,
    * if *this* is a strict (i.e., equality is not enough) superset of *that*.
    *
    * Returns an empty list, if that's not the case.
    *
    */

  def isSupersetOf(that: Caze): List[CazeRubric] = {
    if (that.member_id == member_id && that.rubrics.length < rubrics.length && that.header == header && that.description == description) {
      if (that.rubrics.filter(!rubrics.contains(_)).length == 0) { // If there are no rubrics in *that* that are not contained in *this*...
        return rubrics.filter(!that.rubrics.contains(_))
      }
    }
    List()
  }

  /**
    * @return a non-empty list of case rubrics that have different weights or labels in *that* when compared to *this*' rubrics.
    *         Return an empty list otherwise (i.e., they could be equal or completely different in more ways than just weight or label).
    */

  def isEqualExceptUserDefinedValues(that: Caze): List[CazeRubric] = {
    var result: List[CazeRubric] = Nil

    breakable {
      if (that.member_id == member_id && that.rubrics.length == rubrics.length && that.header == header && that.description == description) {
        val unequalCRubricPairs =
          rubrics
            .sortBy(_.toJson().toString())
            .zip(that.rubrics.sortBy(_.toJson().toString()))
            .filter { case (a, b) => a.equalsExceptWeight(b) }

        if (unequalCRubricPairs.length > 0) {
          result = unequalCRubricPairs.map(_._2)
          break
        }
      }
    }

    result
  }

  override def toString() = s"Caze($id, $header, $member_id, $date, $description, rubrics: #${rubrics.size})"

}

object Caze {

  // def create(id: Int, header: String, member_id: Int, date: String, changed: String, description: String): Caze = {
  //   var _rubrics: List[CazeRubric] = Nil

  //   def getRubricsFromDb(caseId: Int, memberId: Int): Unit = {
  //     HttpRequest2("sec/caserubrics")
  //       .withQueryParameters("caseId" -> caseId.toString, "memberId" -> memberId.toString)
  //       .onSuccess((response: String) => {
  //         parse(response) match {
  //           case Right(json) => {
  //             val cursor = json.hcursor
  //             cursor.as[List[CazeRubric]] match {
  //               case Right(cazerubrics) => {
  //                 _rubrics = cazerubrics
  //               }
  //               case Left(err) => println("Decoding of case failed: " + err)
  //             }
  //           }
  //           case Left(err) => println("Parsing of case (is it JSON?): " + err)
  //         }
  //       })
  //       .send()
  //   }

  //   val newCaze = new Caze(id, header, member_id, date, changed, description, _rubrics)
  //   getRubricsFromDb(id, member_id)
  //   newCaze
  // }

  implicit val caseRubricEncoder: Encoder[CazeRubric] = deriveEncoder[CazeRubric]
  implicit val caseRubricDecoder: Decoder[CazeRubric] = deriveDecoder[CazeRubric]

  implicit val decoder: Decoder[Caze] = deriveDecoder[Caze]
  implicit val encoder: Encoder[Caze] = deriveEncoder[Caze]

  def decode(jsonCaze: String) = {
    io.circe.parser.parse(jsonCaze) match {
      case Right(json) => json.hcursor.as[Caze] match {
        case Right(c) => Some(c)
        case Left(err) => None
      }
      case Left(err) => None
    }
  }

}
