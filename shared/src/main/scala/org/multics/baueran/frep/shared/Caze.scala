package org.multics.baueran.frep.shared

//import java.text.SimpleDateFormat
//import java.util.Date

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Caze(header: String,
                member_id: Int,
                date: String,
                description: String,
                results: List[CaseRubric])

object Caze {
//  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//
//  implicit val decodeDate: Decoder[Date] = new Decoder[Date] {
//    final def apply(c: HCursor): Decoder.Result[Date] =
//      for {
//        date <- c.downField("Date").as[String]
//      } yield {
//        dateFormat.parse(date)
//      }
//  }
//
//  implicit val encodeDate: Encoder[Date] = new Encoder[Date] {
//    final def apply(date: Date): Json = Json.obj(
//      ("Date", Json.fromString(dateFormat.format(date)))
//    )
//  }

  implicit val caseRubricEncoder: Encoder[CaseRubric] = deriveEncoder[CaseRubric]
  implicit val caseRubricDecoder: Decoder[CaseRubric] = deriveDecoder[CaseRubric]

//  implicit val caseRubricListEncoder: Encoder[List[CaseRubric]] = deriveEncoder[List[CaseRubric]]
//  implicit val caseRubricListDecoder: Decoder[List[CaseRubric]] = deriveDecoder[List[CaseRubric]]

  implicit val decoder: Decoder[Caze] = deriveDecoder[Caze]
  implicit val encoder: Encoder[Caze] = deriveEncoder[Caze]
}
