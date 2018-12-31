CREATE TABLE MEMBER(
  MEMBER_ID    INT         PRIMARY KEY NOT NULL,
  MEMBER_NAME  VARCHAR(20) NOT NULL,
  REALNAME     VARCHAR(30) NOT NULL,
  EMAIL        VARCHAR(80) NOT NULL,
  COUNTRY      VARCHAR(80) NOT NULL,
  COMPANY      VARCHAR(80),
  TITLE        VARCHAR(8),
  STUDENT_UNTIL DATE,
  PROFESSION   VARCHAR(80)
);

CREATE TABLE ACTIVITY(
  MEMER_ID     INT         REFERENCES USER(MEMBER_ID) NOT NULL,
  TIME_FROM    DATE        NOT NULL,
  TIME_TO      DATE        NOT NULL,
  ACCOUNT_TYPE VARCHAR(80) NOT NULL,
  AMOUNT_PAID  VARCHAR(80)
);

CREATE TABLE INFO(
  ABBREV       VARCHAR(18) PRIMARY KEY NOT NULL,
  TITLE        VARCHAR(40) NOT NULL,
  LANGUAG      VARCHAR(20) NOT NULL,
  AUTHORLASTNAME  VARCHAR(20),
  AUTHORFIRSTNAME VARCHAR(20),
  YEARR        INT,
  PUBLISHER    VARCHAR(40),
  EDITION      INT,
  ACCESS       VARCHAR(20)
);

--case class Info(abbrev: String, title: String, language: String,
--                authorLastName: Option[String], authorFirstName: Option[String],
--                year: Option[Integer], publisher: Option[String], edition: Option[Integer],
--                access: RepAccess)
