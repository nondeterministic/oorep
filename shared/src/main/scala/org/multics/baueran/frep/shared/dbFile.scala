package org.multics.baueran.frep.shared

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class dbFile(id: Int,
                  header: String,
                  member_id: Int,
                  date: String,
                  description: String,
                  case_ids: List[Int])

object dbFile {

  implicit val decoder: Decoder[dbFile] = deriveDecoder[dbFile]
  implicit val encoder: Encoder[dbFile] = deriveEncoder[dbFile]

  def decode(jsonDbFile: String) = {
    io.circe.parser.parse(jsonDbFile) match {
      case Right(json) => json.hcursor.as[dbFile] match {
        case Right(f) => Some(f)
        case Left(_) => None
      }
      case Left(_) => None
    }
  }

}
