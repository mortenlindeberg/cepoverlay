#!/usr/bin/python
import sys
from os import listdir, system
from os.path import join
import time




def get_sum(file):
    starttime = time.strptime('01:00:00.0', '%H:%M:%S.%f')
    sum = 0
    for line in open(file):
        lineArr = line.split(' ')
        if len(lineArr[0]) > 8:
            timestamp = time.strptime(lineArr[0], '%H:%M:%S.%f')
            delta = (time.mktime(timestamp) - time.mktime(starttime))
            if delta > 300:
                pos = lineArr.index('length')
                length = int(lineArr[pos+1])
                sum = sum + length
        else:
            print '#skipping line ', line, file

    return sum/1000


output_file = '%s/C1/output.res' % sys.argv[1]
pcap_dir = sys.argv[1]

f = open(output_file, "r")
count = 0
sum_delay = 0

for x in f:
    lineArr = x.split()

    if (len(lineArr) < 2):
        continue

    start = float(lineArr[1]) / 1000
    end = float(lineArr[0])
    if (start < 300):
        continue
    sum_delay = sum_delay + (end - start)

    count = count + 1

bw_sum = 0


pcaps = ["trace-RJ1C1-1.pcap", "trace-RJ2C1-1.pcap","trace-RJ1J2-1.pcap",
         "trace-RP1J1-1.pcap", "trace-RP2J1-1.pcap", "trace-RP3J1-1.pcap", "trace-RP4J1-1.pcap",
         "trace-RP5J2-1.pcap", "trace-RP6J2-1.pcap", "trace-RP7J2-1.pcap", "trace-RP8J2-1.pcap"]

for f in pcaps:
    system('tcpdump -r %s/%s -n ip 2> /dev/null | grep Flags > %s_bw.tmp ' % (pcap_dir, f, f))
    bw_sum = bw_sum + get_sum('%s_bw.tmp' % f)
    system('rm %s_bw.tmp' % f)


if count == 0:
    latency = -1
else:
    latency = (sum_delay / count)

if "run" not in sys.argv[1]:
    run = "."
    strat = sys.argv[1].split('_')[3]
    query = sys.argv[1].split('_')[4]
else:
    run = sys.argv[1].split('_')[1].split('/')[0]
    strat = sys.argv[1].split('_')[4]
    query = sys.argv[1].split('_')[5]

print run,strat,query,count,str(latency).replace('.',','),bw_sum