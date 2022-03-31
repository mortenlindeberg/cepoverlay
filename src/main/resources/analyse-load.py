#!/usr/bin/python3

import sys, datetime, time
from os import listdir, system
from os.path import join, isdir


start_time=0

def get_rel_time(timestamp):
    actual_time = datetime.datetime.combine(start_time.date(), datetime.datetime.strptime(timestamp, '%H:%M:%S').time())

    # If experiment ran overnight, we must add one day to make things right
    if actual_time < start_time:
        actual_time = actual_time + datetime.timedelta(days=1)
    relative_timestamp = actual_time - start_time

    print(start_time, timestamp,actual_time, relative_timestamp)

    return relative_timestamp

def analyse_cpu(sar_file):
    system("sar -f %s 2> /dev/null |grep all|awk '{print $1,$4}' > tmp.cpu" % sar_file)
    sum = 0.0
    count = 0
    for line in open("tmp.cpu"):
        count = count + 1
        lineArr = line.split(' ')
        sum = sum + float(lineArr[1].rstrip())


    system("rm tmp.cpu")
    if (count == 0):
        return -1
    return (sum/count)


def analyse_mem(sar_file):
    system("sar -r -f %s 2> /dev/null |egrep -v Linux| egrep -v  kbmemfree|awk '{print $1,$5}' > tmp.mem" % sar_file)
    sum = 0.0
    count = 0
    for line in open("tmp.mem"):
        lineArr = line.split(' ')
        if len(lineArr[0]) < 5:
            continue
        count = count + 1
        sum = sum + float(lineArr[1].rstrip())


    system("rm tmp.mem")
    if (count == 0):
        return -1
    return (sum/count)

def get_meta(sar_file):
    #['run', '1/extra', 'large', 'pamap', '25', '1/A7'] pamap 25
    arr =  sar_file.split('_')
    run = arr[1].split('/')[0]
    strat = arr[4]
    query = arr[5].split('/')[0]
    node = arr[5].split('/')[1]
    return "%s %s %s %s" % (run, strat, query, node)



dir=sys.argv[1]
posix_file = open("%s/A1/start.res" % dir)
posix_start = float(int(posix_file.read()) * 0.001)

start_time=datetime.datetime.fromtimestamp(posix_start)


for entry in listdir(join('.', dir)):
    sar_dir = join(dir, entry)
    if (entry.startswith("A") or entry.startswith("T") or entry.startswith("J") or entry.startswith("C")) and isdir(sar_dir):
        full_dir = join(sar_dir,"load.sar")
        print(get_meta(sar_dir), analyse_cpu(full_dir), analyse_mem(full_dir))

