package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Caze(id: Int,
                header: String,
                member_id: Int,
                date: String,
                description: String,
                results: List[CaseRubric]) {

  def canEqual(a: Any) = {
    a.isInstanceOf[Caze]
  }

  // Ignore id on purpose. It is DB-generated and two same Cazes with different id should be treated as equal!
  override def equals(that: Any): Boolean =
    that match {
      case that: Caze => that.canEqual(this) &&
        that.header == header && that.member_id == member_id && that.date == date && that.description == description && that.results == results
      case _ => false
    }

  // Ignore id on purpose. It is DB-generated and two same Cazes with different id should be treated as equal!
  override def hashCode: Int = {
    val prime = 31
    var result = results.toString().hashCode
    result = prime * result + header.hashCode + member_id + date.hashCode + description.hashCode + results.toString().hashCode
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
