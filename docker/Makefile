# ################################################################################################
#
# A helper Makefile in order to build the docker images, we then store on Dockerhub.
# (This file should have to be used only by the www.oorep.com administrators or OOREP developers.)
#
# ################################################################################################

SHELL:=/bin/bash

branch:="master"
version:="$(shell cat ../build.sbt | grep 'version :=' | awk '{print substr($$3, 2, length($$3)-2)}')"
playapp:=backend-$(version)
sbt:=sbt-1.3.4

all: oorep_app/$(playapp).zip oorep_db/oorep.sql .env
	@echo "**************************************"
	@echo "All done. Now, do docker-compose build"
	@echo "**************************************"

oorep_app/$(playapp).zip: tmp/$(sbt).zip tmp/oorep/LICENSE .env
	cd tmp/oorep && git pull
	cd tmp/oorep/backend/public/html && wget -nc http://pspace.org/a/third-party-v2.tar.gz && tar xzf third-party-v2.tar.gz && rm third-party-v2.tar.gz
	cd tmp/oorep && ../sbt/bin/sbt compile && ../sbt/bin/sbt dist
	cd tmp/oorep/backend/target/universal && unzip $(playapp).zip && rm $(playapp).zip && touch $(playapp)/RUNNING_info.json 
	cd tmp/oorep/backend/target/universal && zip -r $(playapp).zip $(playapp)
	cp tmp/oorep/backend/target/universal/$(playapp).zip ./oorep_app/
	cd ./oorep_app && unzip $(playapp).zip

.env:
	echo "VERSION=$(version)" > ./.env

tmp/$(sbt).zip:
	mkdir -p tmp
	cd tmp && wget -nc https://github.com/sbt/sbt/releases/download/v1.3.4/$(sbt).zip
	cd tmp && unzip $(sbt).zip

tmp/oorep/LICENSE: tmp/$(sbt).zip
	cd tmp && git clone git@github.com:nondeterministic/oorep.git

oorep_db/oorep.sql:
	cp ../oorep.sql.gz ./oorep_db/.
	cd oorep_db && gzip -d ./oorep.sql.gz

clean:
	rm -rf tmp/oorep*
	rm -rf oorep_app/backend-*
