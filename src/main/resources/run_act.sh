#!/bin/bash

nodes=A1:10.0.0.1:1080:1:10Mbps,B1:10.0.0.2:1080:1:10Mbps,C1:10.0.0.3:1080:1:10Mbps
sources=A1:8:10.0.0.1:2080:10.0.0.1:1081:2:0,B1:9:10.0.0.2:2080:10.0.0.2:1081:2:0
links=A1-C1,B1-C1,A1-B1
master_node=C1
duration=2000


conf_prop=config/20_15_0_plus.properties

exp_name=exp_adj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
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

exp_name=exp_aj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 15 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10

exp_name=exp_bj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 16 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=exp_cj
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 17 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10