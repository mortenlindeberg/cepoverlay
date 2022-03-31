#!/usr/bin/python

from os import system
import sys

def find_end(letter, count, exp):
    if letter == 'B':
        system('grep -F \"migrated snapshot (operator) started\" %s/E1/app.log > end.tmp' % exp)
    elif letter == 'A':
        system('grep -F \"migrated snapshot (operator) started\" %sE2/app.log > end.tmp' % exp)
    else:
        print 'Could not determine adaptation file'
    i = 0
    for line in open("end.tmp","r"):
        lineArr = line.split(' ')
        if i == count:
            return lineArr[6]
        else:
            i = 1 + i

def get_times(file):
    times = {}
    system('rm *.tmp')
    system('grep -F \"Query Adapt from node B to node A\" %s/A1/app.log > start.tmp' % file)
    count = 0
    for line in open("start.tmp","r"):
        lineArr = line.split(' ')
        startStr = lineArr[6]

        if not startStr.isdigit():
            return 0

        start = int(startStr)
        end = int(find_end('B', count, exp))
        #print 'B to A',(end-start),start,end
        times[start] = (end-start)
        count = count + 1

    system('rm *.tmp')
    system('grep -F \"Query Adapt from node A to node B\" %s/A1/app.log > start.tmp' % file)
    count = 0
    for line in open("start.tmp","r"):
        lineArr = line.split(' ')
        startStr = lineArr[6]

        if not startStr.isdigit():
            return 0
        start = int(startStr)
        end = int(find_end('A', count, exp))
        #print 'A to B',(end-start),start,end
        times[start] = (end-start)
        count = count + 1

    i = 0
    #print 'before del',times
    for k in sorted (times.keys()):
        if i < 2:
            del times[k]
        else:
            break
        i = i + 1

    return times


exp = sys.argv[1]
times = get_times(exp)
sum = 0
count = 0

try:
    for k in times:
        sum = sum + times[k]
        count = count + 1
except TypeError:
    count = 0

if (count == 0):
    print 0
else:
    print (sum / count)