CREATE USER oorep_user;
ALTER USER oorep_user PASSWORD 'poofoo';
CREATE USER oorep_user_idp;
\c oorep;
CREATE extension pgcrypto;
ALTER DATABASE oorep OWNER TO oorep_user;
