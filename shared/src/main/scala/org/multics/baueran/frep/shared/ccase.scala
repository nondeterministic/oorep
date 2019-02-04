package org.multics.baueran.frep.shared

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe._, io.circe.parser._
import io.circe.syntax._

case class WeightedRemedy(remedy: Remedy, weight: Int) {

  override def equals(that: Any) = {
    that match {
      case w: WeightedRemedy => w.weight == weight && w.remedy == remedy
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + weight + remedy.hashCode()
    result = prime * result +
      (if (remedy == null) weight.toString.hashCode
      else (weight.toString + remedy.toString).hashCode)
    return result
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
            case Right(result) => Some(Remedy(result.id, result.nameAbbrev, result.nameLong))
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
                      weightedRemedies: List[WeightedRemedy])
{
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

  override def equals(that: Any) = {
    that match {
      case c: CaseRubric => c.rubric == rubric && c.repertoryAbbrev == repertoryAbbrev &&
        c.rubricWeight == rubricWeight &&
        c.weightedRemedies == weightedRemedies
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + rubric.toString().hashCode + repertoryAbbrev.hashCode + rubricWeight + weightedRemedies.toString().hashCode
    result = prime * result + rubric.toString().hashCode + repertoryAbbrev.hashCode + rubricWeight + weightedRemedies.toString().hashCode
    return result
  }

}

object CaseRubric {
  implicit val caseRubricEncoder: Encoder[CaseRubric] = deriveEncoder[CaseRubric]
  implicit val caseRubricDecoder: Decoder[CaseRubric] = deriveDecoder[CaseRubric]
}
