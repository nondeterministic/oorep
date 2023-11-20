FROM eclipse-temurin:17

ARG VERSION
ARG PLAYDIR=/usr/local

WORKDIR $PLAYDIR/
COPY backend-$VERSION backend-$VERSION
RUN echo 'pidfile.path = "/dev/null"' >> $PLAYDIR/backend-$VERSION/conf/application.conf

WORKDIR $PLAYDIR/backend-$VERSION/bin/
CMD rm -f $PLAYDIR/backend-$VERSION/RUNNING_PID; ./backend
