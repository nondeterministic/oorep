package org.multics.baueran.frep.shared

import java.text.SimpleDateFormat
import java.util.Date

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
// import io.circe.cursor._

case class Caze(id: String,
                member_id: Int,
                date: Date,
                description: String,
                results: List[CaseRubric])

object Caze {
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  implicit val jsonDecodeDate: Decoder[Date] = new Decoder[Date] {
    final def apply(c: HCursor): Decoder.Result[Date] =
      for {
        date <- c.downField("Date").as[String]
      } yield {
        dateFormat.parse(date)
      }
  }

  implicit val jsonEncodeDate: Encoder[Date] = new Encoder[Date] {
    final def apply(date: Date): Json = Json.obj(
      ("Date", Json.fromString(dateFormat.format(date)))
    )
  }

//  case class CaseRubric(rubric: Rubric,
//                        repertoryAbbrev: String,
//                        var rubricWeight: Int,
//                        weightedRemedies: List[WeightedRemedy])

//  implicit val jsonDecodeListCaseRubric: Decoder[List[CaseRubric]] = new Decoder[List[CaseRubric]] {
//    final def apply(c: HCursor): Decoder.Result[List[CaseRubric]] =
//      Right(List.empty)
//  }

//  implicit val jsonEncodeListCaseRubric: Encoder[List[CaseRubric]] = new Encoder[List[CaseRubric]] {
//    final def apply(caseRubricList: List[CaseRubric]): Json = Json.fromValues(
//      caseRubricList.map(CaseRubric.caseRubricEncoder.apply(_))
//    )
//  }


  implicit val jsonCazeDecoder: Decoder[Caze] = deriveDecoder[Caze]
  implicit val jsonCazeEncoder: Encoder[Caze] = deriveEncoder[Caze]
}
