package org.multics.baueran.frep.shared

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Caze(id: Int,
                header: String,
                member_id: Int,
                date: String,
                description: String,
                results: List[CaseRubric])
{
  def canEqual(a: Any) = a.isInstanceOf[Caze]

  // Ignore id and date on purpose. Id is DB-generated and two same Cazes with different id should be treated as equal!
  override def equals(that: Any): Boolean = {
    that match {
      case that: Caze => this.canEqual(that) &&
        that.header == this.header &&
        that.member_id == this.member_id &&
        that.description == this.description &&
        that.results.length == this.results.length &&
        that.results.sortBy(r => r.repertoryAbbrev + r.rubric.fullPath)
          .zip(this.results.sortBy(r => r.repertoryAbbrev + r.rubric.fullPath))
          .filter { case (a,b) => a == b }.length == this.results.length
      case _ => false
    }
  }

  // Ignore id and date on purpose. Id is DB-generated and two same Cazes with different id should be treated as equal!
  override def hashCode: Int = {
    val prime = 31
    var result = results.toString().hashCode
    result = prime * result +
      (if (header == null) 0 else header.hashCode()) +
      member_id +
      (if (description == null) 0 else description.hashCode()) +
      results.map(_.hashCode()).fold(0)(_ + _)
    result * 23
  }

  /**
    * Returns a non-empty list of additional case rubrics not contained in *that*,
    *   if *this* is a strict (i.e., equality is not enough) superset of *that*.
    *
    * Returns an empty list, if that's not the case.
    *
    */

  def isSupersetOf(that: Caze): List[CaseRubric] = {
    if (that.member_id == member_id && that.results.length < results.length && that.header == header && that.description == description) {
      if (that.results.filter(!results.contains(_)).length == 0) { // If there are no results in *that* that are not contained in *this*...
        return results.filter(!that.results.contains(_))
      }
    }
    List()
  }

  /**
    * Returns a non-empty list of case rubrics that have different weights in *that* when compared to *this*' results.
    *
    * Returns an empty list otherwise (i.e., they could be equal or completely different in more ways than just weight).
    *
    */

  def isEqualExceptWeights(that: Caze): List[CaseRubric] = {
    if (that.member_id == member_id && that.results.length == results.length && that.header == header && that.description == description) {
      val unequalCRubricPairs =
        results
          .sortBy(r => r.repertoryAbbrev + r.rubric.fullPath)
          .zip(that.results.sortBy(r => r.repertoryAbbrev + r.rubric.fullPath))
          .filter { case (a, b) => a.equalsExceptWeight(b) }

      if (unequalCRubricPairs.length > 0)
        return unequalCRubricPairs.map(_._2)
    }

    List()
  }

  override def toString() = s"Caze($id, $header, $member_id, $date, $description, results: #${results.size})"
}

object Caze {

  implicit val caseRubricEncoder: Encoder[CaseRubric] = deriveEncoder[CaseRubric]
  implicit val caseRubricDecoder: Decoder[CaseRubric] = deriveDecoder[CaseRubric]

  implicit val decoder: Decoder[Caze] = deriveDecoder[Caze]
  implicit val encoder: Encoder[Caze] = deriveEncoder[Caze]

  def decode(jsonCaze: String) = {
    io.circe.parser.parse(jsonCaze) match {
      case Right(json) => json.hcursor.as[Caze] match {
        case Right(c) => Some(c)
        case Left(err) => None
      }
      case Left(err) => None
    }
  }

}
