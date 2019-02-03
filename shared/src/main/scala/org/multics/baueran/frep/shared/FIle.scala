package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class FIle(header: String,
                member_id: Int,
                date: String,
                description: String,
                cazes: List[Caze])

object FIle {

  implicit val decoder: Decoder[FIle] = deriveDecoder[FIle]
  implicit val encoder: Encoder[FIle] = deriveEncoder[FIle]

}