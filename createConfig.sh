#!/bin/bash
mvn clean; mvn install -DskipTests
jcmd="java -jar target/cepoverlay-1.0-SNAPSHOT-spring-boot.jar"
$jcmd -configurator learningWindowSize=80,edgePointsSize=3
