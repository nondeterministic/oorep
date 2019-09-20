## Executing the program

For best results, use JDK 8.  (In particular, I've had build problems with JDK 12.)

Start OOREP as follows.  Set `$OOREP_USER_PASS` to the password used by the DB-user, then execute:

    sbt
    run -Dhttps.port=9443 -Dhttp.port=9000

