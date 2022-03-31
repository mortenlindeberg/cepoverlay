#!/usr/bin/python
import sys
from os import listdir, system
from os.path import join
import time

WARMUP = 600


def get_pcaps(dir):
    out = []
    for f in listdir(join('.',dir)):
        if f.endswith('-0.pcap'):
            out.append(join(dir, f))
    return out


def get_sum(file):
    starttime = time.strptime('01:00:00.0', '%H:%M:%S.%f')
    sum = 0
    for line in open(file):
        lineArr = line.split(' ')
        if len(lineArr[0]) > 8:
            timestamp = time.strptime(lineArr[0], '%H:%M:%S.%f')
            delta = (time.mktime(timestamp) - time.mktime(starttime))
            if delta >= WARMUP:
                pos = lineArr.index('length')
                length = int(lineArr[pos+1])
                #		print length, delta
                sum = sum + length
        else:
            print '#skipping line ', line, file

    return sum


pcap_dir = sys.argv[1]

bw_sum = 0
for f in get_pcaps(pcap_dir):
    #    print f
    system('tcpdump -r %s -n port 1080 2> /dev/null | grep Flags > %s_bw.tmp ' % (f, f))
    bw_sum = bw_sum + get_sum('%s_bw.tmp' % f)
    system('rm %s_bw.tmp' % f)

print pcap_dir, bw_sum

