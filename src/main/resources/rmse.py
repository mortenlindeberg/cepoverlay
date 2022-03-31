#!/usr/bin/python
import sys, math
f = open(sys.argv[1])

for x in f:
    arr = x.split()
    val = float(arr[1])
    pred = float(arr[3])
    if (pred == -1):
        continue
    rmse = math.sqrt((val - pred)**2)

    print arr[1], rmse
