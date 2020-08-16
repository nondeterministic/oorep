package org.multics.baueran.frep.shared

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe._
import io.circe.parser._
import io.circe.syntax._

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

  implicit val keyRemedyDecoder = new KeyDecoder[Remedy] {
    def apply(key: String): Option[Remedy] = {
      parse(key) match {
        case Right(json) => {
          val cursor = json.hcursor
          cursor.as[Remedy] match {
            case Right(result) => Some(Remedy(result.abbrev, result.id, result.nameAbbrev, result.nameLong))
            case Left(_) => None
          }
        }
        case Left(_) => None
      }
    }
  }

  implicit val keyRemedyEncoder = new KeyEncoder[Remedy] {
    override def apply(remedy: Remedy): String = {
      remedy.asJson.toString()
    }
  }

  implicit val wrEncoder: Encoder[WeightedRemedy] = deriveEncoder[WeightedRemedy]
  implicit val wrDecoder: Decoder[WeightedRemedy] = deriveDecoder[WeightedRemedy]

}

case class CaseRubric(rubric: Rubric,
                      repertoryAbbrev: String,
                      var rubricWeight: Int,
                      var rubricLabel: Option[String],
                      weightedRemedies: List[WeightedRemedy])
{
  object VarHandling extends Enumeration {
    type VarHandling = Value
    val Equal, NotEqual, Ignore = Value
  }
  import VarHandling._

  def canEqual(a: Any) = a.isInstanceOf[CaseRubric]

  def containsRemedyAbbrev(remedyAbbrev: String) = {
    weightedRemedies.filter(_.remedy.nameAbbrev == remedyAbbrev).size > 0
  }

  def getRemedyWeight(remedyAbbrev: String): Integer = {
    val remedy = weightedRemedies.filter(_.remedy.nameAbbrev == remedyAbbrev)
    if (remedy.size > 0)
      remedy.head.weight
    else
      0
  }

  private def checkEquality(handlingOfVars: VarHandling, that: Any): Boolean = {
    that match {
      case that: CaseRubric =>
        this.canEqual(that) &&
          that.repertoryAbbrev == this.repertoryAbbrev &&
          (handlingOfVars match {
            case Equal => s"${that.rubricWeight}".toLong == s"${this.rubricWeight}".toLong  &&
              that.rubricLabel.getOrElse("").toLowerCase.reverse == this.rubricLabel.getOrElse("").toLowerCase.reverse
            case NotEqual => s"${that.rubricWeight}".toLong != s"${this.rubricWeight}".toLong  ||
              that.rubricLabel.getOrElse("").toLowerCase.reverse != this.rubricLabel.getOrElse("").toLowerCase.reverse
            case _ => true
          }) &&
          that.rubric == this.rubric &&
          that.weightedRemedies.length == this.weightedRemedies.length &&
          that.weightedRemedies.sortWith(_.remedy.nameAbbrev > _.remedy.nameAbbrev)
            .zip(this.weightedRemedies.sortWith(_.remedy.nameAbbrev > _.remedy.nameAbbrev))
            .filter{ case (a: WeightedRemedy, b: WeightedRemedy) => a == b }.length == this.weightedRemedies.length
      case _ => false
    }
  }

  override def equals(that: Any) = checkEquality(Equal, that)

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
      rubric.hashCode() +
      (if (repertoryAbbrev == null) 0 else repertoryAbbrev.hashCode()) +
      rubricWeight +
      weightedRemedies.map(_.hashCode()).fold(0)(_ + _)
    result * prime
  }

}

object CaseRubric {
  implicit val encoder: Encoder[CaseRubric] = deriveEncoder[CaseRubric]
  implicit val decoder: Decoder[CaseRubric] = deriveDecoder[CaseRubric]

  def decode(jsonCaseRubric: String) = {
    io.circe.parser.parse(jsonCaseRubric) match {
      case Right(json) => json.hcursor.as[CaseRubric] match {
        case Right(c) => Some(c)
        case Left(err) => None
      }
      case Left(err) => None
    }
  }

  def decodeList(jsonCaseRubric: String) = {
    io.circe.parser.parse(jsonCaseRubric) match {
      case Right(json) => json.hcursor.as[List[CaseRubric]] match {
        case Right(c) => Some(c)
        case Left(err) => None
      }
      case Left(err) => None
    }
  }
}

/**
  * This class is merely used/necessary in order to transmit lookup results of a repertory
  */
case class ResultsCaseRubrics(totalNumberOfResults: Int, totalNumberOfPages: Int, currPage: Int, results: List[CaseRubric])
object ResultsCaseRubrics {
  implicit val encoder: Encoder[ResultsCaseRubrics] = deriveEncoder[ResultsCaseRubrics]
  implicit val decoder: Decoder[ResultsCaseRubrics] = deriveDecoder[ResultsCaseRubrics]
}
