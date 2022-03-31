#!/usr/bin/python
import sys

def window_avg(window):
    sum = 0
    for v in window:
        sum = sum + v

    return sum / len(window)
fasit = False

if (sys.argv[0] > 0 and sys.argv[1] == "f"):
    f = open(sys.argv[2], "r")
    fasit = True
else:
    f = open(sys.argv[2], "r")

memory = 0
n = 50
window = []

for x in f:
    lineArr = x.split()

    if (len(lineArr) < 2):
        continue

    if (fasit and float(lineArr[2]) < 20):
        continue

    value = float(lineArr[0])

    if memory == 0:
        memory = value
        continue

    window.append(value-memory)
    memory = float(lineArr[0])

    if len(window) == 6:
        window.pop(0)
        if (window_avg(window) == 0):
            print value, 0
        else:
            print value, 1/window_avg(window)

