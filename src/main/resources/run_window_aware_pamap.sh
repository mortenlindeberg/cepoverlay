#!/bin/bash

nodes=SA1:10.0.0.1:1080:1:10Mbps,SB1:10.0.0.2:1080:1:10Mbps,A1:10.0.0.3:1080:1:10Mbps,B1:10.0.0.4:1080:1:10Mbps,C1:10.0.0.5:1080:1:10Mbps
sources=SA1:8:10.0.0.1:2080:10.0.0.3:1081:1:0,SB1:9:10.0.0.2:2080:10.0.0.4:1081:1:0
links=SA1-A1,SB1-B1,A1-B1,A1-C1,B1-C1

master_node=C1

duration=2000

pp=false
jcmd="java -jar cepoverlay-1.0-SNAPSHOT-spring-boot.jar"
lw=20
ep=6
fin=100
windowSize=1000
migrationTime=$fin
queryId=0

$jcmd -configurator learningWindowSize=$lw,edgePointsSize=$ep,futureInterval=$fin,proactivePAMAPPlus=$pp,queryId=$queryId,slopeActive=true,windowWait=false,windowAware=true

exp_name=window-aware-pamap
echo " -> Starting $exp_name with argument $nodes"
$jcmd -configurator learningWindowSize=$lw,edgePointsSize=$ep,futureInterval=$fin,proactivePAMAPPlus=$pp,queryId=$queryId,slopeActive=true,windowWait=false,windowAware=true
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 18 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10

exp_name=window-wait-pamap
echo " -> Starting $exp_name with argument $nodes"
$jcmd -configurator learningWindowSize=$lw,edgePointsSize=$ep,futureInterval=$fin,proactivePAMAPPlus=$pp,queryId=$queryId,slopeActive=true,windowWait=true,windowAware=true
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 18 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10

exp_name=window-nonaware-pamap
cp $conf_prop config.properties

echo " -> Starting $exp_name with argument $nodes"
$jcmd -configurator learningWindowSize=$lw,edgePointsSize=$ep,futureInterval=$fin,proactivePAMAPPlus=$pp,queryId=$queryId,slopeActive=true,windowWait=false,windowAware=false
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 19 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10
