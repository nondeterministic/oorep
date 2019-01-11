package org.multics.baueran.frep.shared

import java.util.Date

case class Member(member_id: Long,
                  member_name: String,
                  md5: String,
                  realname: String,
                  email: String,
                  country: String,
                  company: Option[String] = None,
                  title: Option[String] = None,
                  student_until: Option[Date] = None,
                  profession: Option[String] = None)
