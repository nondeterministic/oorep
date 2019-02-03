package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Caze(header: String,
                member_id: Int,
                date: String,
                description: String,
                results: List[CaseRubric]) {

//  def canEqual(a: Any) = {
//    a.isInstanceOf[Caze]
//  }
//
//  override def equals(that: Any): Boolean =
//    that match {
//      case that: Caze => that.canEqual(this) && this.hashCode == that.hashCode
//      case _ => false
//    }
//
//  override def hashCode: Int = {
//    val prime = 31
//    var result = 1
//    result = prime * result + member_id + results.length;
//    result = prime * result + (if (header == null) 0 else (header + description + member_id.toString()).hashCode)
//    return result
//  }

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
