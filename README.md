## Executing the OOREP software

For best results, use JDK 8.  (In particular, I've had build problems with JDK 12; broken String class.)

Set the following environment variables appropriately:

* `$OOREP_APPLICATION_SECRET`: application-specific password you need to see if you run the server using `https` instead of `http`
* `$OOREP_USER_PASS`: password used by the DB-user
* `$OOREP_REP_PATH`: directory in the local file system, where the repertory raw data is located
* `$OOREP_KEYSTORE_FILE`: full path of your `.keystore` file
* `$OOREP_KEYSTORE_PASS`: password of your keystore

Adjustments to this can also be made in `backend/conf/application.conf`, of course.

When satisfied with your configuration, you can start OOREP as follows:

    sbt
    run -Dhttps.port=9443 -Dhttp.port=9000
