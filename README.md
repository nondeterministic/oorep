# OOREP

OOREP is an acronym for open online repertory for homeopathy.  That is, it lets
users look up categories in homeopathic repertories.  This git repository
consists of its (mainly) ScalaJS source code.  A running version of it, however,
can also be found at https://www.oorep.com/.

## Current status / what's new

Besides the aforementioned homepage, check out the [NEWS](NEWS) file for the
latest updates.

## How to get the source code running

### Prerequisites

* Java SDK (for best results, use JDK 8 or 11.  (In particular, I've had build
  problems with JDK 12 - broken String class.))
* Scala Build Tool (SBT, tested with 1.3.x)
* A PostgreSQL database server >= 9.6 (tested with versions 10.x and 11.x)

Before executing SBT, the database (which is provided here as an SQL dump) needs
to be up and running and PostgreSQL's pgcrypto-extension created (e.g., as
super-user `postgres`, issue the command `CREATE EXTENSION pgcrypto;` on the
OOREP-database). The database needs to be named '`oorep`' and the PostgreSQL-user
owning it, '`oorep_user`'. Also, you will need to define the following environment
variables:

* `$OOREP_APPLICATION_HOST`: Usually something like `http://localhost:9000`
  for development environments, or `https://www.oorep.com` for a production
  environment (notice, no trailing slash)
* `$OOREP_APPLICATION_SECRET`: application-specific password you need to set
  if you run the server using `https` instead of `http` (which I don't, since
  I use a reverse proxy)
* `$OOREP_DB_HOST`: hostname of DB-server (e.g., localhost)
* `$OOREP_DB_PORT`: port of DB-server (usually 5432)
* `$OOREP_DB_PASS`: password used by the DB-user
* `$OOREP_URL_LOGOUT`: right now only mandatory if authentication is used:
  the URL which users must click on in order to logout of OOREP (usually your
  logout binding of an accompanying (SAML) Service Provider)
* `$OOREP_MAIL_SERVER`: right now only mandatory if authentication is used:
  hostname of your SMTP server to send password reset mails (e.g., mail.server.com)
* `$OOREP_MAIL_USER`: right now only mandatory if authentication is used:
  username on your SMTP server
* `$OOREP_MAIL_PASS`: right now only mandatory if authentication is used:
  password for that username on your SMTP server

Check `backend/conf/application.conf` for their respective use, and make other
adjustments as you see fit there.  Also be sure these variables are set before
attempting to execute the OOREP code using SBT.

### Recommended external libraries

It is strongly recommended to also download the following external JavaScript
libraries and fonts:

* [aos](https://github.com/michalsnik/aos) (tested with v2.3.4)
* [bootstrap](https://getbootstrap.com/) (tested with v4.2.1)
* [bootswatch](https://bootswatch.com/) (tested with v4.2.1)
* [jquery](https://jquery.com/) (tested with v3.3.1)
* [popper](https://popper.js.org/) (tested with v1.15.0)
* [Open Iconic font](https://useiconic.com/open)
* [Open Sans font](https://fonts.google.com/specimen/Open+Sans)
* [Linux Libertine Display font](https://en.wikipedia.org/wiki/Linux_Libertine)

For your convenience, an archived package with the respective versions 
is available [here](http://pspace.org/a/third-party-v2.tar.gz).  It should
be unpacked and then placed inside `backend/public/html` within the main
OOREP directory. (For OOREP versions < 0.11, you should use
[this file](http://pspace.org/a/third-party.tar.gz)
instead.)

### Compiling and running

When you're content with your setup, OOREP, a
[Play](https://www.playframework.com/) application written in Scala, can be run
like any other using SBT's targets `run` or `compile`.  Be sure, the database is
started and available before though. If all went well, the result should then be
available at http://localhost:9000/.

You can also build a distribution package of OOREP by first executing `compile`
and then `dist` in SBT, which will build an executable, 
`backend/target/universal/backend-x.y.z.zip`, and which can also be used to run
a stand-alone version of the application. This is also how the oorep.com-server
is run.

## Runtime optimisations

Some queries will be slow, if the database is not optimised. As this is
work in progress, the following is a somewhat crude approximation: It is
advisable to add the following (and possibly other) indexes to OOREP's
database:

```
create index on rubricremedy (abbrev, remedyid);
create index on rubricremedy (abbrev, rubricid);
create index on rubricremedy (abbrev, rubricid, remedyid);
create index on remedy (abbrev, id);
create index on rubric (abbrev, id);
```

## Making user login work

Since version v0.10.0, OOREP no longer provides its own code to handle login and logout
of users. Instead, the OOREP application expects the variable `X-Remote-User` to be set
in the HTTP request header then containing the OOREP user-ID.  There are various ways to
achieve  this.  In www.oorep.com SAML 2.0 is used as follows: all calls to OOREP are
routed through a SAML service provider (mod_auth_mellon) which protects calls to `/login`
and `/api/sec/...` by invoking  a SAML identity provider (SimpleSAMLphp). When the user
enters valid  credentials to the identity provider, this variable is set and passed back
to OOREP's service provider.

For this to work securely, you need to make sure that, indeed, all calls to `/login` and
`/api/sec/...` are safe-guarded by the service provider, e.g., by adding a
`<LocationMatch /(login|api/sec/.+)>` directive to an Apache2 reverse-proxy (or similar - depending
on your setup), which passes such calls on to the service provider before they reach the
main OOREP application server.

While to this end www.oorep.com uses a SAML 2.0 solution, other authentication protocol
implementations can also be used. Also, it is probably not easy to get all this to work without
using the aforementioned reverse-proxy, although it is not strictly an OOREP requirement.
