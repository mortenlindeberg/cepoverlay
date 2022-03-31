#!/usr/bin/python3

import os, sys
from os import listdir, system
from os.path import isfile, join, isdir
from datetime import datetime, timedelta

def get_pcaps(dir):
    out = []
    for f in listdir(join('.',dir)):
        if f.endswith('.pcap') and f.startswith("trace-R"):
            out.append(join(dir,f))
    return out

def main():
    pcap_dir = sys.argv[1]
    data = {}

    for fn in get_pcaps(pcap_dir):

        system('tcpdump -r %s -n ip 2>> /dev/null|grep length >> %s_bw.tmp' % (fn, fn))
        cleanpath = os.path.abspath('%s_bw.tmp' % fn)
        f = open(cleanpath)
        for l in f:
            arr = l.split(' ')

            if not 'length' in arr:
                continue

            index = arr.index('length')

            # 01:01:01.987110
            time = datetime.strptime(arr[0], "%H:%M:%S.%f")
            length = int(arr[index+1])

            if (length > 0):
                if time in data:
                    prev = data[time]
                else:
                    prev = 0
                data[time] = length + prev

        system('rm %s' % cleanpath)

    first = -1
    sum = 0
    count = 0
    for key in sorted(data.keys()):
        if first == -1:
            first = key
            count = count + 1

        timestamp = first + timedelta(seconds=count)
        if key >= timestamp:
            print(count, sum)
            count = count + 1
            sum = data[key]
        else:
            sum = sum + data[key]



if __name__ == "__main__":
    main()