# --- !Ups

ALTER TABLE MEMBER ADD ACCESS VARCHAR(283);
ALTER TABLE MEMBER ADD ISADMIN BOOLEAN;
ALTER TABLE MEMBER ADD BANNEDSINCE VARCHAR(287);

CREATE TABLE REMEDYINFO(
  REMEDYID     INT PRIMARY KEY,
  ADDEFOR      TEXT
);

# --- !Downs

DROP TABLE MEMBER;
DROP TABLE REMEDYINFO;
