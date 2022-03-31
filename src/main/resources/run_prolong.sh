#/bin/bash

nodes=A1:10.0.0.1:1076:1:1Mbps,B1:10.0.0.2:1078:1:1Mbps,E1:10.0.0.3:1080:1:1Mbps,E2:10.0.0.4:1082:1:1Mbps,I1:10.0.0.5:1084:1:1Mbps,D1:10.0.0.6:1086:1:1Mbps
links=A1-E1,E1-E2,B1-E2,E1-I1,E2-I1,I1-D1
master_node=I1
duration=300

l=20
f=15
ms=0
n=0

for p in 1000 5000 10000 20000 40000
do
  exp_name=exp_prolong_$p
  sources=A1:1:10.0.0.1:2079:10.0.0.1:1077:10:$n:$p,A1:1:10.0.0.1:2080:10.0.0.4:1083:10:$n:$p,B1:2:10.0.0.2:2081:10.0.0.4:1083:500:$n:$p
  cp config/$l\_$f\_$ms.properties config.properties
  echo " -> Starting $exp_name with argument $nodes"
  sudo ./run-experiment2.py --start --nodes $nodes --links $links --sources $sources --master_node $master_node --strategy 2 --duration $duration
  sleep $duration
  sleep 20
  echo " -> Stopping $exp_name"
  sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name
  rm -rf delete
  sudo ./run-experiment2.py --stop --nodes $nodes --exp_name delete
  sleep 10
  rm config.properties
done