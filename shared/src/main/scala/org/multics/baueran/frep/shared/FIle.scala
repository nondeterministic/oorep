package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class FIle(dbId: Option[Int],
                header: String,
                member_id: Int,
                date: String,
                description: String,
                cazes: List[Caze])

object FIle {

  implicit val decoder: Decoder[FIle] = deriveDecoder[FIle]
  implicit val encoder: Encoder[FIle] = deriveEncoder[FIle]

  def decode(jsonFile: String) = {
    io.circe.parser.parse(jsonFile) match {
      case Right(json) => json.hcursor.as[FIle] match {
        case Right(f) => Some(f)
        case Left(err) => None
      }
      case Left(err) => None
    }
  }

}