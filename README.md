![Logo](doc/logo.png "OOREP")

OOREP is an acronym for open online repertory for homeopathy.  That is, it lets
users look up categories in homeopathic repertories.  This git repository
consists of its (mainly) Scala source code.  A running version of it, however,
can also be found at https://www.oorep.com/.

## Current status / what's new

Besides the aforementioned homepage, check out the [NEWS](NEWS) file for the
latest development updates. (You may want to to focus on releases marked as 'stable'.)

## Running the program

You can choose between the following two different methods in order to get OOREP
up and running on your own machine.

### Method 1: By using ready-made Docker images (recommended method)

#### Prerequisite

* You have the commands `docker` and `docker compose` (formerly `docker-compose`) available on your machine.

#### Pull and start the Docker images from [Dockerhub](https://hub.docker.com/u/oorep)

But first, clone OOREP's source code repository from here. Afterwards change into
the `docker` subdirectory of your then local source code repository and execute the
following two commands one after another:
```
$ docker compose pull
$ docker compose up
```
Depending on your Internet connection, those commands may take a while to complete.
When all is done, you should be able to point your web browser to `http://localhost:9000`
and see OOREP's landing page. Needless to say, those images are test-only and are not
intended for any production environments or the like without further modifications.

Please also be aware that pulled docker images may not always be as up to date as the
rest of the OOREP source code.  Sometimes OOREP's code changes affect mostly the deployed
version of OOREP on oorep.com, but has little effect for a locally run application.  Then
I usually don't recreate all the docker images on dockerhub.

### Method 2: By building the project from scratch (for experts only)

#### Prerequisites

* Java SDK >= v17
* Scala Build Tool SBT >= v1.9.7
* A PostgreSQL database server >= v11

Before executing SBT, the database (which is provided here as an SQL dump) needs
to be up and running and PostgreSQL's pgcrypto-extension created (e.g., as
superuser `postgres`, issue the command `CREATE EXTENSION pgcrypto;` on the
OOREP-database; if you still encounter permission issues when restoring the database,
you may also want to try `ALTER ROLE "<your username>" superuser;`). Also, you will
need to define the following environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `OOREP_APP_PROTOCOL` | *(required)* | Usually either `http` or `https`. |
| `OOREP_APP_HOSTNAME` | *(required)* | The part that follows `OOREP_APP_PROTOCOL`, such as `www`, or any other hostname. |
| `OOREP_APP_DOMAIN` | *(optional)* | Your domain name, e.g. `oorep.com`; do not set when using `localhost` as hostname. |
| `OOREP_APP_PORT` | *(optional)* | Port of application server (normally `9000`); only set if the application server is used directly (do not set when using a reverse proxy). |
| `OOREP_APP_SECRET` | *(required for https)* | Application-specific secret; needed when running with `https` instead of `http`. |
| `OOREP_DB_NAME` | `oorep` | Name of your PostgreSQL database, unless you know better. |
| `OOREP_DB_USER` | `oorep_user` | Name of your PostgreSQL database user, unless you know better. |
| `OOREP_DB_PASS` | *(required)* | Password of your PostgreSQL database user; for test environments, use the one from the `docker` directory, unless you know better. |
| `OOREP_DB_HOST` | *(required)* | Full hostname of DB server (e.g. `localhost` or `db.oorep.com`). |
| `OOREP_DB_PORT` | `5432` | Port of DB server. |
| `OOREP_URL_LOGOUT` | *(required if auth used)* | URL users click to log out of OOREP (usually your logout binding of an accompanying (SAML) Service Provider). |
| `OOREP_MAIL_SERVER` | *(required if auth used)* | Hostname of your SMTP server for password reset mails (e.g. `mail.server.com`). |
| `OOREP_MAIL_USER` | *(required if auth used)* | Username on your SMTP server. |
| `OOREP_MAIL_PASS` | *(required if auth used)* | Password for that username on your SMTP server. |
  
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

## Session management for anonymous users

Since version 0.20.0, OOREP can now utilise a locally installed Redis memory database
for session management of anonymous users as well.  It will work without Redis present,
but it is designed to use it to detect request flooding and would be able to throttle
those inside the application without reliance on external tools like fail2ban or what
have you.

The following environment variables can be used to configure it:

| Variable | Default | Description |
| --- | --- | --- |
| `OOREP_SESSION_GUARD_ENABLED` | `true` | Enable/disable the anonymous session guard |
| `OOREP_SESSION_GUARD_FAIL_OPEN` | `true` | Allow requests if Redis is unavailable |
| `OOREP_SESSION_GUARD_LOG_ONLY` | `false` | Log guard decisions but do not block |
| `OOREP_REDIS_HOST` | `localhost` | Redis host |
| `OOREP_REDIS_PORT` | `6379` | Redis port |
| `OOREP_REDIS_DB` | `0` | Redis DB index |
| `OOREP_REDIS_PASSWORD` | *(unset)* | Redis password, if required |
| `OOREP_REDIS_TIMEOUT_MS` | `2000` | Redis connection/socket timeout |
| `OOREP_SESSION_TTL_SECONDS` | `86400` | Anonymous session TTL (sliding) |
| `OOREP_RATE_LIMIT_WINDOW_SECONDS` | `60` | Rate-limit window |
| `OOREP_RATE_LIMIT_GENERAL_PER_IP` | `120` | General API limit per IP/window |
| `OOREP_RATE_LIMIT_GENERAL_PER_SID` | `120` | General API limit per session/window |
| `OOREP_RATE_LIMIT_EXPENSIVE_PER_IP` | `60` | Expensive API limit per IP/window |
| `OOREP_RATE_LIMIT_EXPENSIVE_PER_SID` | `60` | Expensive API limit per session/window |
| `OOREP_SESSION_GUARD_BAN_ENABLED` | `true` | Enable incremental temporary bans |
| `OOREP_SESSION_GUARD_BAN_BASE_SECONDS` | `60` | First ban duration |
| `OOREP_SESSION_GUARD_BAN_MAX_SECONDS` | `7200` | Max ban duration (2h) |
| `OOREP_SESSION_GUARD_BAN_ABUSE_TTL_SECONDS` | `3600` | Abuse-counter decay TTL |
| `OOREP_TRUSTED_PROXIES` | `127.0.0.1,::1` | CSV of trusted proxy IPs/CIDRs for XFF |

