package org.multics.baueran.frep.backend.models

import java.util.Date

case class Users(user_id: Long,
                 user_name: String,
                 md5: String,
                 realname: String,
                 email: String,
                 country: String,
                 company: Option[String] = None,
                 title: Option[String] = None,
                 student_until: Option[Date] = None,
                 profession: Option[String] = None)
