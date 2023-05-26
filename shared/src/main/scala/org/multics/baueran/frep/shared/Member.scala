package org.multics.baueran.frep.shared

import java.util.Date
import java.text.SimpleDateFormat

import io.circe._
import io.circe.{ Decoder, Encoder }

case class Member(member_id: Int,
                  member_name: String,
                  hash: String,
                  realname: String,
                  email: String,
                  country: String,
                  numberoflogins: Int,
                  company: Option[String] = None,
                  title: Option[String] = None,
                  student_until: Option[Date] = None,
                  profession: Option[String] = None,
                  access: Option[String] = None,
                  lastseen: Option[String] = None,
                  isadmin: Option[Boolean] = None,
                  bannedsince: Option[String] = None
                 )

object Member {
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

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
      val access = c.downField("access").as[String].toOption
      val lastseen = c.downField("lastseen").as[String] match {
        case Right(date) => Some(date)
        case _ => None
      }
      val numberoflogins = c.downField("numberoflogins").as[Int] match {
        case Right(d) => d
        case _ => return Left(DecodingFailure("Member decoding failed: numberoflogins.", c.history))
      }
      val isadmin = c.downField("isadmin").as[Boolean].toOption
      val bannedsince = c.downField("bannedsince").as[String] match {
        case Right(date) => Some(date)
        case _ => None
      }

      Right(Member(member_id, member_name, hash, realname, email, country, numberoflogins, company, title, student_until, profession, access, lastseen, isadmin, bannedsince))
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
      ("access", m.access match {
        case Some(d) => Json.fromString(d)
        case None => Json.Null
      }),
      ("lastseen", m.lastseen match {
        case Some(d) => Json.fromString(dateFormat.format(d))
        case None => Json.Null
      }),
      ("numberoflogins", Json.fromInt(m.numberoflogins)),
      ("isadmin", m.isadmin match {
        case Some(d) => Json.fromBoolean(d)
        case None => Json.Null
      }),
      ("bannedsince", m.bannedsince match {
        case Some(d) => Json.fromString(dateFormat.format(d))
        case None => Json.Null
      })
    )
  }

}
