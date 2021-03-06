# Users schema

# --- !Ups

CREATE TABLE INFO(
  ABBREV       VARCHAR(256) PRIMARY KEY NOT NULL,
  TITLE        VARCHAR(257) NOT NULL,
  LANGUAG      VARCHAR(258) NOT NULL,
  AUTHORLASTNAME  VARCHAR(259),
  AUTHORFIRSTNAME VARCHAR(260),
  YEARR        INT,
  PUBLISHER    VARCHAR(261),
  LICENSE      VARCHAR(255),
  EDITION      VARCHAR(255),
  ACCESS       VARCHAR(262)
);

CREATE TABLE CHAPTER(
  ABBREV      VARCHAR(263) NOT NULL,
  ID          INT NOT NULL,
  TEXTT       VARCHAR(264)
);

CREATE TABLE RUBRICREMEDY(
  ABBREV      VARCHAR(265) NOT NULL,
  RUBRICID    INT NOT NULL,
  REMEDYID    INT NOT NULL,
  WEIGHT      INT NOT NULL,
  CHAPTERID   INT NOT NULL
);

CREATE TABLE REMEDY(
  ABBREV      VARCHAR(266) NOT NULL,
  ID          INT NOT NULL,
  NAMEABBREV  VARCHAR(267) NOT NULL,
  NAMELONG    VARCHAR(268) NOT NULL
);

CREATE TABLE CHAPTERREMEDY(
  ABBREV      VARCHAR(269) NOT NULL,
  REMEDYID    INT NOT NULL,
  CHAPTERID   INT NOT NULL
);

CREATE TABLE RUBRIC(
  ABBREV      VARCHAR(270) NOT NULL,
  ID          INT NOT NULL,
  MOTHER      INT,
  ISMOTHER    BOOLEAN,
  CHAPTERID   INT NOT NULL,
  FULLPATH    VARCHAR(2048) NOT NULL,
  PATH        VARCHAR(272),
  TEXTT       VARCHAR(273)
);

CREATE TABLE FILE(
  ID          SERIAL PRIMARY KEY,
  HEADER      VARCHAR(274) NOT NULL,
  MEMBER_ID   INT NOT NULL,
  DATE_       VARCHAR(275),
  DESCRIPTION VARCHAR(2048),
  CASE_IDS    INT[]
);

CREATE TABLE MEMBER(
  MEMBER_ID    INT         PRIMARY KEY NOT NULL,
  MEMBER_NAME  VARCHAR(276) NOT NULL,
  HASH         VARCHAR(277) NOT NULL,
  REALNAME     VARCHAR(278) NOT NULL,
  EMAIL        VARCHAR(279) NOT NULL,
  COUNTRY      VARCHAR(280) NOT NULL,
  COMPANY      VARCHAR(281),
  TITLE        VARCHAR(282),
  STUDENT_UNTIL DATE,
  PROFESSION   VARCHAR(283)
);

CREATE TABLE ACTIVITY(
  MEMER_ID     INT         REFERENCES MEMBER(MEMBER_ID) NOT NULL,
  TIME_FROM    DATE        NOT NULL,
  TIME_TO      DATE        NOT NULL,
  ACCOUNT_TYPE VARCHAR(284) NOT NULL,
  AMOUNT_PAID  VARCHAR(285)
);

-- https://stackoverflow.com/questions/14225397/postgresql-insert-an-array-of-composite-type-containing-arrays
-- CREATE TYPE WEIGHTEDREMEDY AS (REMEDY_ID INT, WEIGHT INT);

-- WORKS: insert into caserubric VALUES (  ROW(0, NULL, NULL, 0, 'FP', NULL, NULL), 'kent', 1, ARRAY[ROW(1,1)::WEIGHTEDREMEDY]  );
--CREATE TYPE CASERUBRIC AS (
--   RUBRIC_         RUBRIC,
--   REPERTORYABBREV VARCHAR(40),
--   RUBRICWEIGHT    INT,
--   WEIGHTEDREMEDIES WEIGHTEDREMEDY[]
--);

CREATE TABLE CAZE(
  ID           SERIAL PRIMARY KEY,
  HEADER       VARCHAR(286) NOT NULL,
  MEMBER_ID    INT NOT NULL,
  DATE_        VARCHAR(287),
  DESCRIPTION  VARCHAR(2048),
  RESULTS      INT[]
);

CREATE TABLE CAZERESULT(
  ID           SERIAL PRIMARY KEY,
  MEMBER_ID    INT NOT NULL,
  ABBREV       VARCHAR(265) NOT NULL,
  RUBRICID     INT NOT NULL,
  WEIGHT       INT NOT NULL
);

# --- !Downs

DROP TABLE ACTIVITY;
DROP TABLE MEMBER;
DROP TABLE INFO;
DROP TABLE RUBRIC;
DROP TABLE REMEDY;
-- DROP TYPE CASERUBRIC;
-- DROP TYPE WEIGHTEDREMEDY;
DROP TABLE CHAPTER;
DROP TABLE CHAPTERREMEDY;
DROP TABLE RUBRICREMEDY;
DROP TABLE FILE;
DROP TABLE CAZE;
