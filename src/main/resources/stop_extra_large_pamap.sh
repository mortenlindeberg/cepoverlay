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
exp_name=single
sudo ./run-experiment2.py --stop --nodes $nodes --exp_name $exp_name