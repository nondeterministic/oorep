# OOREP

OOREP is an acronym for open online repertory for homeopathy.  That is, it lets
users look up categories in homeopathic repertories.  This GitLab repository
consists of its (mainly) ScalaJS source code.  A running version of it, however,
can also be found at https://www.oorep.com/.

## Current status / what's new

Besides the aforementioned homepage, check out the [NEWS](NEWS) file for the
latest updates.

## How to get the source code running

Prerequisites:

* Java SDK (For best results, use JDK 8.  (In particular, I've had build
  problems with JDK 12 - broken String class.))
* Scala Build Tool (SBT, tested with >= 1.3.0)
* An SQL database (tested with PostgreSQL >= 10.0)

Before executing SBT, the database needs to be up and running.  Also, you will
need to define the following environment variables:

* `$OOREP_APPLICATION_SECRET`: application-specific password you need to see
  if you run the server using `https` instead of `http`
* `$OOREP_USER_PASS`: password used by the DB-user
* `$OOREP_REP_PATH`: directory in the local file system, where the repertory
  raw data is located

Check `backend/conf/application.conf` for their respective use, and make other
adjustments as you see fit there.

When you're content with your setup, OOREP, a Play-application, can be run
like any other using `run` or `compile`.  If all went well, the result should
then be available at http://localhost:9000/.

You can also build a distribution package of OOREP by first executing `compile`
and then `dist`, which will build an executable, 
`backend/target/universal/backend-x.y.z.zip`, which can also be used to run
a stand-alone version of the application.