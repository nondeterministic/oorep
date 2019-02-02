package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Caze(header: String,
                member_id: Int,
                date: String,
                description: String,
                results: List[CaseRubric])

object Caze {

  implicit val caseRubricEncoder: Encoder[CaseRubric] = deriveEncoder[CaseRubric]
  implicit val caseRubricDecoder: Decoder[CaseRubric] = deriveDecoder[CaseRubric]

  implicit val decoder: Decoder[Caze] = deriveDecoder[Caze]
  implicit val encoder: Encoder[Caze] = deriveEncoder[Caze]

  def decode(jsonCaze: String) = {
    io.circe.parser.parse(jsonCaze) match {
      case Right(json) => json.hcursor.as[Caze] match {
        case Right(c) => c
        case Left(err) => throw new IllegalArgumentException("Error decoding Caze: " + jsonCaze + "; " + err)
      }
      case Left(err) => throw new IllegalArgumentException("Error parsing Caze: " + jsonCaze + "; " + err)
    }
  }

}
