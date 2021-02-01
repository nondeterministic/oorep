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
* Scala Build Tool (SBT, tested with >= 1.3.0)
* A PostgreSQL database server (tested with versions 10 and 11, but should work
  with about any version)

Before executing SBT, the database (which is provided here as an SQL dump) needs
to be up and running.  Also, you will need to define the following environment
variables:

* `$OOREP_APPLICATION_HOST`: Usually something like `http://localhost:9000`
  for development environments, or `https://www.oorep.com` for a production
  environment (notice, no trailing slash).
* `$OOREP_APPLICATION_SECRET`: application-specific password you need to see
  if you run the server using `https` instead of `http`
* `$OOREP_DBHOST`: hostname of DB-server (e.g., localhost)
* `$OOREP_DBPORT`: port of DB-server (usually 5432)
* `$OOREP_DBUSER_PASS`: password used by the DB-user

Check `backend/conf/application.conf` for their respective use, and make other
adjustments as you see fit there.  Also be sure these variables are set before
attempting to execute the OOREP code.

### Recommended external libraries

It is strongly recommended to also download the following external JavaScript
libraries and fonts:

* [bootstrap](https://getbootstrap.com/) (tested with v4.1.3)
* [jquery](https://jquery.com/) (tested with v3.3.1)
* [popper](https://popper.js.org/) (tested with v1.15.0)
* [notify.js](https://github.com/jpillora/notifyjs) (tested with v0.4.2)
* [Open Iconic font](https://useiconic.com/open)

For your convenience, an archived package with the respective versions 
is available [here](http://pspace.org/a/third-party.tar.gz).  It should
be unpacked and then placed inside `backend/public/html` within the main
OOREP directory.

### Compiling

When you're content with your setup, OOREP, a
[Play](https://www.playframework.com/)-application written in Scala, can be run
like any other using SBT's targets `run` or `compile`.  Be sure, the database is
started and available before though. If all went well, the result should then be
available at http://localhost:9000/.

You can also build a distribution package of OOREP by first executing `compile`
and then `dist` in SBT, which will build an executable, 
`backend/target/universal/backend-x.y.z.zip`, and which can also be used to run
a stand-alone version of the application.

### Runtime optimisations

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
