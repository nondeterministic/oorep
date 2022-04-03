FROM postgres

ENV POSTGRES_DB=oorep
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=poofoo

COPY init.sql /docker-entrypoint-initdb.d/
COPY oorep.sql /docker-entrypoint-initdb.d/

