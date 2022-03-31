#!/bin/bash

P1SourceNodes=T1:10.0.0.1:1080:1:10Mbps,A1:10.0.0.2:1080:1:10Mbps,P1:10.0.0.3:1080:1:10Mbps
P2SourceNodes=T2:10.0.0.4:1080:1:10Mbps,A2:10.0.0.5:1080:1:10Mbps,P2:10.0.0.6:1080:1:10Mbps
P3SourceNodes=T3:10.0.0.7:1080:1:10Mbps,A3:10.0.0.8:1080:1:10Mbps,P3:10.0.0.9:1080:1:10Mbps
P4SourceNodes=T4:10.0.0.10:1080:1:10Mbps,A4:10.0.0.11:1080:1:10Mbps,P4:10.0.0.12:1080:1:10Mbps
nodes=$P1SourceNodes,$P2SourceNodes,$P3SourceNodes,$P4SourceNodes
nodes=$nodes,J1:10.0.0.13:1080:1:10Mbps,J2:10.0.0.14:1080:1:10Mbps,C1:10.0.0.15:1080:1:10Mbps
echo "Nodes: $nodes"

P1links=T1-P1,A1-P1
P2links=T2-P2,A2-P2
P3links=T3-P3,A3-P3
P4links=T4-P4,A4-P4
links=$P1links,$P2links,$P3links,$P4links
links=$links,P1-J1,P2-J1,P3-J2,P4-J2,J1-C1,J2-C1
echo "Links: $links"

P1Sources=T1:10:10.0.0.1:2080:10.0.0.3:1081:2:0,A1:11:10.0.0.2:2080:10.0.0.3:1081:2:0
P2Sources=T2:12:10.0.0.4:2080:10.0.0.6:1081:2:0,A2:13:10.0.0.5:2080:10.0.0.6:1081:2:0
P3Sources=T3:14:10.0.0.7:2080:10.0.0.9:1081:2:0,A3:15:10.0.0.8:2080:10.0.0.9:1081:2:0
P4Sources=T4:16:10.0.0.10:2080:10.0.0.12:1081:2:0,A4:17:10.0.0.11:2080:10.0.0.12:1081:2:0
sources=$P1Sources,$P2Sources,$P3Sources,$P4Sources
echo "Sources $sources"


master_node=C1
duration=2000
conf_prop=config/20_15_0_plus_5000.properties

exp_name=large_pamap_ap
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 24 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=large_pamap_p1
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 20 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=large_pamap_p2
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 21 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=large_pamap_p3
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 22 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=large_pamap_p4
echo " -> Starting $exp_name with argument $nodes"
cp $conf_prop config.properties
sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 23 --duration $duration
sleep $duration
sleep 20
echo " -> Stopping $exp_name"
sudo killall cepoverlay
sleep 1
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
rm -rf delete
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
sleep 10


exp_name=large_pamap
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


P1Sources=T1:10:10.0.0.1:2080:10.0.0.15:1081:2:0,A1:11:10.0.0.2:2080:10.0.0.15:1081:2:0
P2Sources=T2:12:10.0.0.4:2080:10.0.0.15:1081:2:0,A2:13:10.0.0.5:2080:10.0.0.15:1081:2:0
P3Sources=T3:14:10.0.0.7:2080:10.0.0.15:1081:2:0,A3:15:10.0.0.8:2080:10.0.0.15:1081:2:0
P4Sources=T4:16:10.0.0.10:2080:10.0.0.15:1081:2:0,A4:17:10.0.0.11:2080:10.0.0.15:1081:2:0
sources=$P1Sources,$P2Sources,$P3Sources,$P4Sources

exp_name=large_pamap_split
echo " -> Starting $exp_name with argument $nodes"

cp $conf_prop config.properties
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