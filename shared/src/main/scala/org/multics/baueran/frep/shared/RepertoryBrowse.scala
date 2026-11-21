package org.multics.baueran.frep.shared

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class RepertoryBrowseRubric(
  id: Int,
  mother: Option[Int],
  isMother: Option[Boolean],
  chapterId: Int,
  fullPath: String,
  path: Option[String],
  hasRemedies: Boolean
)

object RepertoryBrowseRubric {
  implicit val encoder: Encoder[RepertoryBrowseRubric] = deriveEncoder[RepertoryBrowseRubric]
  implicit val decoder: Decoder[RepertoryBrowseRubric] = deriveDecoder[RepertoryBrowseRubric]
}

case class RepertoryBrowseChapterInfo(
  chapter: Chapter,
  remedyRubricCount: Int
)

object RepertoryBrowseChapterInfo {
  implicit val encoder: Encoder[RepertoryBrowseChapterInfo] = deriveEncoder[RepertoryBrowseChapterInfo]
  implicit val decoder: Decoder[RepertoryBrowseChapterInfo] = deriveDecoder[RepertoryBrowseChapterInfo]
}

case class RepertoryBrowseChaptersResponse(
  chapters: List[RepertoryBrowseChapterInfo]
)

object RepertoryBrowseChaptersResponse {
  implicit val encoder: Encoder[RepertoryBrowseChaptersResponse] = deriveEncoder[RepertoryBrowseChaptersResponse]
  implicit val decoder: Decoder[RepertoryBrowseChaptersResponse] = deriveDecoder[RepertoryBrowseChaptersResponse]
}

case class RepertoryBrowseChapterResponse(
  chapterId: Int,
  rubrics: List[RepertoryBrowseRubric]
)

object RepertoryBrowseChapterResponse {
  implicit val encoder: Encoder[RepertoryBrowseChapterResponse] = deriveEncoder[RepertoryBrowseChapterResponse]
  implicit val decoder: Decoder[RepertoryBrowseChapterResponse] = deriveDecoder[RepertoryBrowseChapterResponse]
}

case class RepertoryRubricRemedies(
  rubric: Rubric,
  weightedRemedies: List[WeightedRemedy]
)

object RepertoryRubricRemedies {
  implicit val encoder: Encoder[RepertoryRubricRemedies] = deriveEncoder[RepertoryRubricRemedies]
  implicit val decoder: Decoder[RepertoryRubricRemedies] = deriveDecoder[RepertoryRubricRemedies]
}
