package org.multics.baueran.frep.shared

import java.util.Date

import io.circe.{Decoder, HCursor}

case class Member(member_id: Long,
                  member_name: String,
                  hash: String,
                  realname: String,
                  email: String,
                  country: String,
                  company: Option[String] = None,
                  title: Option[String] = None,
                  student_until: Option[Date] = None,
                  profession: Option[String] = None)

//object Member {
//  implicit val memberDecoder: Decoder[Member] = new Decoder[Member] {
//    final def apply(c: HCursor): Decoder.Result[Member] = {
//      val member_id = c.downField("member_id").as[String]
//      val member_name = c.downField("member_name").as[String].getOrElse("ERROR")
//      val language = c.downField("language").as[String].getOrElse("ERROR")
//      val authorLastName = c.downField("authorLastName").as[String] match {
//        case Right(name) => Some(name)
//        case Left(_) => None
//      }
//
//    }
//  }
//}