#!/bin/bash

nodes=SA1:10.0.0.1:1080:1:10Mbps,SB1:10.0.0.2:1080:1:10Mbps,A1:10.0.0.3:1080:1:10Mbps,B1:10.0.0.4:1080:1:10Mbps,C1:10.0.0.5:1080:1:10Mbps
sources=SA1:8:10.0.0.1:1080:10.0.0.1:1081:1:0,SB1:9:10.0.0.2:1080:10.0.0.2:1081:1:0
links=SA1-A1,SB1-B1,A1-B1,A1-C1,B1-C1

exp_name=single
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name

