package org.multics.baueran.frep.backend.models

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

//CREATE TABLE MEMBER(
//MEMBER_ID    INT         PRIMARY KEY NOT NULL,
//MEMBER_NAME  VARCHAR(20) NOT NULL,
//REALNAME     VARCHAR(30) NOT NULL,
//EMAIL        VARCHAR(80) NOT NULL,
//COUNTRY      VARCHAR(80) NOT NULL,
//COMPANY      VARCHAR(80),
//TITLE        VARCHAR(8),
//STUDENT_UNTIL DATE,
//PROFESSION   VARCHAR(80)
//);
