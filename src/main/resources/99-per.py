#!/usr/bin/python

import sys
import numpy as np


LOW_LIMIT = 600
HIGH_LIMIT = 1600

def getLatencies(file):
    f = open(file, "r")
    o = []
    for line in f:
        lineArr = line.split()
        if (len(lineArr) < 3):
            continue

        timestamp = float(lineArr[0])

        if timestamp < LOW_LIMIT or timestamp > HIGH_LIMIT:
            continue

        timestamp = timestamp * 1000
        latency = float(lineArr[0]) * 1000 - int(lineArr[1])
        o.append(latency)
    return o

f = sys.argv[1]
p = int(sys.argv[2])


arr = getLatencies(f)
print f, np.percentile(arr, p)