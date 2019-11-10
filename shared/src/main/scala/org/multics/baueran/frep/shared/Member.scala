package org.multics.baueran.frep.shared

import java.util.Date
import java.text.SimpleDateFormat

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{ Decoder, Encoder }

case class Member(member_id: Int,
                  member_name: String,
                  hash: String,
                  realname: String,
                  email: String,
                  country: String,
                  company: Option[String] = None,
                  title: Option[String] = None,
                  student_until: Option[Date] = None,
                  profession: Option[String] = None,
                  lastseen: Option[Date] = None,
                  numberoflogins: Int)

//object Member {
//  implicit val memberDecoder: Decoder[Member] = deriveDecoder[Member]
//  implicit val memberEncoder: Encoder[Member] = deriveEncoder[Member]
//}

object Member {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  implicit val memberDecoder: Decoder[Member] = new Decoder[Member] {
    final def apply(c: HCursor): Decoder.Result[Member] = {
      val member_id = c.downField("member_id").as[Int] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: member_id.", c.history))
      }
      val member_name = c.downField("member_name").as[String] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: member_name.", c.history))
      }
      val hash = c.downField("hash").as[String] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: hash.", c.history))
      }
      val realname = c.downField("realname").as[String] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: realname.", c.history))
      }
      val email = c.downField("email").as[String] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: email.", c.history))
      }
      val country = c.downField("country").as[String] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: country.", c.history))
      }
      val company = c.downField("company").as[String].toOption
      val title = c.downField("title").as[String].toOption
      val student_until = c.downField("student_until").as[String] match {
        case Right(date) => Some(dateFormat.parse(date))
        case _ => None
      }
      val profession = c.downField("profession").as[String].toOption
      val lastseen = c.downField("lastseen").as[String] match {
        case Right(date) => Some(dateFormat.parse(date))
        case _ => None
      }
      val numberoflogins = c.downField("numberoflogins").as[Int] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: numberoflogins.", c.history))
      }

      Right(Member(member_id, member_name, hash, realname, email, country, company, title, student_until, profession, lastseen, numberoflogins))
    }
  }

  implicit val memberEncoder: Encoder[Member] = new Encoder[Member] {
    def apply(m: Member): Json = Json.obj(
      ("member_id", Json.fromInt(m.member_id)),
      ("member_name", Json.fromString(m.member_name)),
      ("hash", Json.fromString(m.hash)),
      ("realname", Json.fromString(m.realname)),
      ("email", Json.fromString(m.email)),
      ("country", Json.fromString(m.country)),
      ("company", m.company match {
        case Some(d) => Json.fromString(d)
        case None => Json.Null
      }),
      ("title", m.title match {
        case Some(d) => Json.fromString(d)
        case None => Json.Null
      }),
      ("student_until", m.student_until match {
        case Some(d) => Json.fromString(dateFormat.format(d))
        case None => Json.Null
      }),
      ("profession", m.profession match {
        case Some(d) => Json.fromString(d)
        case None => Json.Null
      }),
      ("lastseen", m.lastseen match {
        case Some(d) => Json.fromString(dateFormat.format(d))
        case None => Json.Null
      }),
      ("numberoflogins", Json.fromInt(m.numberoflogins))
    )
  }

}
