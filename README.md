![Logo](doc/logo.png "OOREP")

OOREP is an acronym for open online repertory for homeopathy.  That is, it lets
users look up categories in homeopathic repertories.  This git repository
consists of its (mainly) Scala source code.  A running version of it, however,
can also be found at https://www.oorep.com/.

## Current status / what's new

Besides the aforementioned homepage, check out the [NEWS](NEWS) file for the
latest development updates.

## Running the program

You can choose between the following two different methods in order to get OOREP
up and running on your own machine.

### Method 1: By using ready-made Docker images (recommended method)

#### Prerequisite

* You have the commands `docker` and `docker-compose` available on your machine.

#### Pull and start the Docker images from [Dockerhub](https://hub.docker.com/u/oorep)

But first, clone OOREP's source code repository from here. Afterwards change into
the `docker` subdirectory of your then local source code repository and execute the
following two commands one after another:
```
$ docker-compose pull
$ docker-compose up
```
Depending on your Internet connection, those commands may take a while to complete.
When all is done, you should be able to point your web browser to `http://localhost:9000`
and see OOREP's landing page. Needless to say, those images are test-only and are not
intended for any production environments or the like without further modifications.

### Method 2: By building the project from scratch (for experts only)

#### Prerequisites

* Java SDK versions 8 or 11
* Scala Build Tool SBT >= 1.3.0
* A PostgreSQL database server >= 9.6

Before executing SBT, the database (which is provided here as an SQL dump) needs
to be up and running and PostgreSQL's pgcrypto-extension created (e.g., as
superuser `postgres`, issue the command `CREATE EXTENSION pgcrypto;` on the
OOREP-database; if you still encounter permission issues when restoring the database,
you may also want to try `ALTER ROLE "<your username>" superuser;`). Also, you will
need to define the following environment variables:

* `$OOREP_APP_PROTOCOL`: usually either `http` or `https`
* `$OOREP_APP_HOSTNAME`: the part that follows `$OOREP_APP_PROTOCOL`,
  such as `www`, for example, or any other hostname
* `$OOREP_APP_DOMAIN`: your domain name, e.g., `oorep.com`, but do not set when, for
  example, you're merely using `localhost` as hostname
* `$OOREP_APP_PORT`: port of application server (normally `9000`), but should only be
  set if application server is directly used (i.e., do not set when using a reverse
  proxy!)
* `$OOREP_APP_SECRET`: application-specific password you need to set if you run the 
  application server using `https` instead of `http` (which I don't, since I use a
  reverse proxy that does end-to-end encryption)
* `$OOREP_DB_NAME`: name of your PostgreSQL database; set to `oorep`, unless 
  you know better!
* `$OOREP_DB_USER`: name of your PostgreSQL database user; set to `oorep_user`, unless
  you know better!
* `$OOREP_DB_PASS`: password of your PostgreSQL database; for test-environments, 
  use the one from within the `docker` directory of this repository, unless you know better!
* `$OOREP_DB_HOST`: full hostname of DB-server (e.g., `localhost` or `db.oorep.com`)
* `$OOREP_DB_PORT`: port of DB-server (usually `5432`)
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
adjustments as you see fit there (especially if you alter the domain variable).
Also be sure these variables are set before attempting to execute the OOREP code
using SBT.

#### Recommended external libraries

It is strongly recommended to also download the following external JavaScript
libraries and fonts:

* [aos](https://github.com/michalsnik/aos) (tested with v2.3.4)
* [bootstrap](https://getbootstrap.com/) (tested with v4.3.1)
* [bootswatch](https://bootswatch.com/) (tested with v4.3.1)
* [jquery](https://jquery.com/) (tested with v3.5.1)
* [Open Iconic font](https://useiconic.com/open)
* [Open Sans font](https://fonts.google.com/specimen/Open+Sans)
* [Linux Libertine Display font](https://en.wikipedia.org/wiki/Linux_Libertine)

For your convenience, an archived package with the respective versions 
is available [here](http://pspace.org/a/third-party-v3.tar.gz).  It should
be unpacked and then placed inside `backend/public/html` within the main
OOREP directory. (For OOREP versions < 0.11, you should use
[this file](http://pspace.org/a/third-party.tar.gz)
instead.)

#### Compiling and running

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

#### Runtime optimisations

Some queries will be slow, if the database is not optimised. As this is
work in progress, the following is a somewhat crude approximation: It is
advisable to add the following (and possibly other) indexes to OOREP's
database:

```
create index on rubricremedy (abbrev, remedyid);
create index on rubricremedy (abbrev, rubricid);
create index on rubricremedy (abbrev, rubricid, remedyid);
create index on remedy (nameabbrev, id);
create index on rubric (abbrev, id);
```

## Making user login work

Since version v0.10.0, OOREP no longer provides its own code to handle login and logout
of users. Instead, the OOREP application expects the variable `X-Remote-User` to be set
in the HTTP request header then containing the OOREP user-ID.  There are various ways to
achieve  this.  In www.oorep.com SAML is used as follows: all calls to OOREP are routed
through a SAML service provider which protects calls to `/login` and `/api/sec/...` by
invoking  a SAML identity provider. When the user enters valid  credentials to the
identity provider, this variable is set and passed back to OOREP's service provider.

For this to work securely, you need to make sure that, indeed, all calls to `/login` and
`/api/sec/...` are safe-guarded by the service provider, e.g., by adding a
`<LocationMatch /(login|api/sec/.+)>` directive to an Apache2 reverse-proxy (or similar - 
depending on your setup), which passes such calls on to the service provider before they
reach the main OOREP application server.

While to this end www.oorep.com uses a SAML solution, other authentication protocol
implementations can also be used. Also, it is probably not easy to get all this to work
without using the aforementioned reverse-proxy, although it is not strictly an OOREP
requirement.
