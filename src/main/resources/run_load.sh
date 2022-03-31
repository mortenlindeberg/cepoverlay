#!/bin/bash

nodes=A1:10.0.0.1:1080:1:1Mbps,B1:10.0.0.2:1080:1:1Mbps,C1:10.0.0.3:1080:1:1Mbps,D1:10.0.0.4:1080:1:1Mbps
sources=A1:6:10.0.0.1:2080:10.0.0.1:1081:50:0,C1:7:10.0.0.3:2080:10.0.0.3:1081:50:0
links=A1-B1,B1-D1,C1-D1
master_node=C1
duration=300


conf_prop=config/40_15_0.properties

exp_name=exp_adj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 14 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10




exp_name=exp_aj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 11 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10

exp_name=exp_bj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 12 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=exp_cj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 13 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10