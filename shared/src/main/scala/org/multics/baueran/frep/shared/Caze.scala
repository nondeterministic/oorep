package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Caze(id: Int,
                header: String,
                member_id: Int,
                date: String,
                description: String,
                results: List[CaseRubric])
{
  def canEqual(a: Any) = a.isInstanceOf[Caze]

  // Ignore id and date on purpose. Id is DB-generated and two same Cazes with different id should be treated as equal!
  override def equals(that: Any): Boolean = {
    that match {
      case that: Caze => that.canEqual(this) &&
        that.header == header &&
        that.member_id == member_id &&
        that.description == description &&
        that.results.sortWith(_.repertoryAbbrev > _.repertoryAbbrev) == results.sortWith(_.repertoryAbbrev > _.repertoryAbbrev)
      case _ => false
    }
  }

  // Ignore id and date on purpose. Id is DB-generated and two same Cazes with different id should be treated as equal!
  override def hashCode: Int = {
    val prime = 31
    var result = results.toString().hashCode
    result = prime * result +
      (if (header == null) 0 else header.hashCode()) +
      member_id +
      (if (description == null) 0 else description.hashCode()) +
      results.map(_.hashCode()).fold(0)(_ + _)
    return result
  }

}

object Caze {

  implicit val caseRubricEncoder: Encoder[CaseRubric] = deriveEncoder[CaseRubric]
  implicit val caseRubricDecoder: Decoder[CaseRubric] = deriveDecoder[CaseRubric]

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
