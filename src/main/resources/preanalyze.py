#!/usr/bin/python
from os.path import exists


def get_delay(filename):
    if not exists(filename):
        return -1

    f = open(filename, "r")
    count = 0
    sum_delay = 0

    for x in f:
        lineArr = x.split()

        if (len(lineArr) < 2):
            continue

        start = float(lineArr[1])
        end = float(lineArr[0])
        sum_delay = sum_delay + (end - start)

        count = count + 1

    return (sum_delay / count)
l=20
for f in (10, 40, 80, 120, 160, 200, 240, 280, 320, 360, 400):
    filename = "exp_%s_%s/D1/output.res" % (l,f)
    print l, f, get_delay(filename)