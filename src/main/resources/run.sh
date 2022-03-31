#!/bin/bash

sources=A1:1:10.0.0.1:2080:10.0.0.4:1083:10:0,B1:2:10.0.0.2:2081:10.0.0.4:1083:500:0
nodes=A1:10.0.0.1:1076:1:1Mbps,B1:10.0.0.2:1078:1:1Mbps,E1:10.0.0.3:1080:1:1Mbps,E2:10.0.0.4:1082:1:1Mbps,I1:10.0.0.5:1084:1:1Mbps,D1:10.0.0.6:1086:1:1Mbps
links=A1-E1,E1-E2,B1-E2,E1-I1,E2-I1,I1-D1
master_node=I1
duration=300

exp_name=exp_zero
conf_prop=config/20_15_0.properties

echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 0 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=exp_one
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sources=A1:1:10.0.0.1:2080:10.0.0.4:1083:10:0,B1:2:10.0.0.2:2081:10.0.0.4:1083:500:0
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 1 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=exp_two
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 2 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10

exp_name=exp_pluss
cp config/20_20_0_plus.properties config.properties
echo " -> Starting $exp_name with argument $nodes"
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 2 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10
rm config.properties


exp_name=exp_three
sources=A1:1:10.0.0.1:2080:10.0.0.3:1081:10:0,B1:2:10.0.0.2:2081:10.0.0.3:1081:500:0
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 3 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=exp_four
sources=A1:1:10.0.0.1:2080:10.0.0.5:1085:10:0,B1:2:10.0.0.2:2081:10.0.0.5:1085:500:0
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 4 --duration $duration
sleep $duration
sleep 20
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10