package org.multics.baueran.frep.shared

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class CazeResult(id: Int,
                      member_id: Int,
                      abbrev: String,
                      rubricId: Int,
                      weight: Int,
                      label: Option[String])
{
  def canEqual(a: Any) = a.isInstanceOf[CazeResult]

  override def equals(that: Any): Boolean = {
    that match {
      case that: CazeResult => that.canEqual(this) &&
        that.id == id &&
        that.member_id == member_id &&
        that.abbrev == abbrev &&
        that.rubricId == rubricId &&
        that.weight == weight &&
        that.label.getOrElse("") == label.getOrElse("")
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 31
    (prime * abbrev.hashCode + member_id + rubricId + weight + label.getOrElse("23231").hashCode) * prime
  }

}

object CazeResult {
  implicit val caseResultEncoder: Encoder[CazeResult] = deriveEncoder[CazeResult]
  implicit val caseResultDecoder: Decoder[CazeResult] = deriveDecoder[CazeResult]
}
