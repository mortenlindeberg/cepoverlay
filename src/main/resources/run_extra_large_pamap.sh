#!/bin/bash

P1SourceNodes=T1:10.0.0.1:1080:1:10Mbps,A1:10.0.0.2:1080:1:10Mbps,P1:10.0.0.3:1080:1:10Mbps
P2SourceNodes=T2:10.0.0.4:1080:1:10Mbps,A2:10.0.0.5:1080:1:10Mbps,P2:10.0.0.6:1080:1:10Mbps
P3SourceNodes=T3:10.0.0.7:1080:1:10Mbps,A3:10.0.0.8:1080:1:10Mbps,P3:10.0.0.9:1080:1:10Mbps
P4SourceNodes=T4:10.0.0.10:1080:1:10Mbps,A4:10.0.0.11:1080:1:10Mbps,P4:10.0.0.12:1080:1:10Mbps
P5SourceNodes=T5:10.0.0.13:1080:1:10Mbps,A5:10.0.0.14:1080:1:10Mbps,P5:10.0.0.15:1080:1:10Mbps
P6SourceNodes=T6:10.0.0.16:1080:1:10Mbps,A6:10.0.0.17:1080:1:10Mbps,P6:10.0.0.18:1080:1:10Mbps
P7SourceNodes=T7:10.0.0.19:1080:1:10Mbps,A7:10.0.0.20:1080:1:10Mbps,P7:10.0.0.21:1080:1:10Mbps
P8SourceNodes=T8:10.0.0.22:1080:1:10Mbps,A8:10.0.0.23:1080:1:10Mbps,P8:10.0.0.24:1080:1:10Mbps

nodes=$P1SourceNodes,$P2SourceNodes,$P3SourceNodes,$P4SourceNodes,$P5SourceNodes,$P6SourceNodes,$P7SourceNodes,$P8SourceNodes
nodes=$nodes,J1:10.0.0.25:1080:1:10Mbps,J2:10.0.0.26:1080:1:10Mbps,C1:10.0.0.27:1080:1:10Mbps
echo "Nodes: $nodes"

P1links=T1-P1,A1-P1
P2links=T2-P2,A2-P2
P3links=T3-P3,A3-P3
P4links=T4-P4,A4-P4
P5links=T5-P5,A5-P5
P6links=T6-P6,A6-P6
P7links=T7-P7,A7-P7
P8links=T8-P8,A8-P8

links=$P1links,$P2links,$P3links,$P4links,$P5links,$P6links,$P7links,$P8links
links=$links,P1-J1,P2-J1,P3-J1,P4-J1,J1-C1,J1-J2,P5-J2,P6-J2,P7-J2,P8-J2,J2-C1
echo "Links: $links"

P1Sources=T1:10:10.0.0.1:2080:10.0.0.3:1081:1:0,A1:11:10.0.0.2:2080:10.0.0.3:1081:1:0
P2Sources=T2:12:10.0.0.4:2080:10.0.0.6:1081:1:0,A2:13:10.0.0.5:2080:10.0.0.6:1081:1:0
P3Sources=T3:14:10.0.0.7:2080:10.0.0.9:1081:1:0,A3:15:10.0.0.8:2080:10.0.0.9:1081:1:0
P4Sources=T4:16:10.0.0.10:2080:10.0.0.12:1081:1:0,A4:17:10.0.0.11:2080:10.0.0.12:1081:1:0
P5Sources=T5:18:10.0.0.13:2080:10.0.0.15:1081:1:0,A5:19:10.0.0.14:2080:10.0.0.15:1081:1:0
P6Sources=T6:20:10.0.0.16:2080:10.0.0.18:1081:1:0,A6:21:10.0.0.17:2080:10.0.0.18:1081:1:0
P7Sources=T7:22:10.0.0.19:2080:10.0.0.21:1081:1:0,A7:23:10.0.0.20:2080:10.0.0.21:1081:1:0
P8Sources=T8:24:10.0.0.22:2080:10.0.0.24:1081:1:0,A8:25:10.0.0.23:2080:10.0.0.24:1081:1:0
sources=$P1Sources,$P2Sources,$P3Sources,$P4Sources,$P5Sources,$P6Sources,$P7Sources,$P8Sources
echo "Sources $sources"


master_node=C1
duration=4500

pp=false
jcmd="java -jar cepoverlay-1.0-SNAPSHOT-spring-boot.jar"

for queryId in 1 2
do
for strat in 25 26 27
do

  if [ $queryId = 1 ]
  then
    lw=40
    ep=3
    fin=50
  fi

  if [ $queryId = 2 ]
  then
    lw=40
    ep=6
    fin=200
  fi


  exp_name=extra_large_pamap\_$strat\_$queryId
  echo " -> Starting $exp_name with argument $nodes"
  $jcmd -configurator learningWindowSize=$lw,edgePointsSize=$ep,futureInterval=$fin,proactivePAMAPPlus=$pp,queryId=$queryId,slopeActive=true
  sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy $strat --duration $duration
  sleep $duration
  sleep 20
  echo " -> Stopping $exp_name"
  sudo killall cepoverlay
  sleep 1
  sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
  rm -rf delete
  sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
  sleep 10
done
done

