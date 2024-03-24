package org.multics.baueran.frep.shared

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class FileOverviewRow(file_description: String, file_id: Option[Int], file_header: Option[String])

object FileOverviewRow {
  implicit val decoder: Decoder[FileOverviewRow] = deriveDecoder[FileOverviewRow]
  implicit val encoder: Encoder[FileOverviewRow] = deriveEncoder[FileOverviewRow]
}
