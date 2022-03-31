#!/bin/bash
rm cepoverlay-1.0-SNAPSHOT-spring-boot.jar
rm build.tar.gz
cd ../../../;mvn clean; mvn install -DskipTests
cp target/cepoverlay-1.0-SNAPSHOT-spring-boot.jar src/main/resources
cd src/main/resources/


tar zcvf build.tar.gz Dockerfile *.jar *.sh *.py *.dem *.cc
scp build.tar.gz mglindeb@login.ifi.uio.no:




