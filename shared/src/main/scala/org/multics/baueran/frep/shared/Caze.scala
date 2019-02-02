package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Caze(header: String,
                member_id: Int,
                date: String,
                description: String,
                results: List[CaseRubric])

object Caze {

  // TODO: Do we need those because we use them in CazeDao?
  //  implicit val caseRubricListEncoder: Encoder[List[CaseRubric]] = deriveEncoder[List[CaseRubric]]
  //  implicit val caseRubricListDecoder: Decoder[List[CaseRubric]] = deriveDecoder[List[CaseRubric]]

  implicit val caseRubricEncoder: Encoder[CaseRubric] = deriveEncoder[CaseRubric]
  implicit val caseRubricDecoder: Decoder[CaseRubric] = deriveDecoder[CaseRubric]

  implicit val decoder: Decoder[Caze] = deriveDecoder[Caze]
  implicit val encoder: Encoder[Caze] = deriveEncoder[Caze]
}
