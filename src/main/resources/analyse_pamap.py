#!/usr/bin/python
import sys
from os import listdir, system
from os.path import join
import time


def get_pcaps(dir):
    out = []
    for f in listdir(join('.',dir)):
        if f.endswith('-1.pcap') and f.startswith("trace-R"):
            out.append(join(dir, f))
    return out


def get_sum(file):
    starttime = time.strptime('01:00:00.0', '%H:%M:%S.%f')
    sum = 0
    for line in open(file):
        lineArr = line.split(' ')
        #print lineArr
        if len(lineArr[0]) > 8:
            timestamp = time.strptime(lineArr[0], '%H:%M:%S.%f')
            delta = (time.mktime(timestamp) - time.mktime(starttime))
            if delta > 100:
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
    if (start < 100):
        continue
    sum_delay = sum_delay + (end - start)

    count = count + 1

bw_sum = 0
for f in get_pcaps(pcap_dir):
    system('tcpdump -r %s -n ip 2> /dev/null | grep Flags > %s_bw.tmp ' % (f, f))
    bw_sum = bw_sum + get_sum('%s_bw.tmp' % f)
    system('rm %s_bw.tmp' % f)


if count == 0:
    latency = -1
else:
    latency = (sum_delay / count)

print pcap_dir, count, latency, bw_sum

