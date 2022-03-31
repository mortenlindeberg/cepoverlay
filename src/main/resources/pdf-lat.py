#!/usr/bin/python3

import sys
import numpy as np
import matplotlib.pyplot as plt
import statistics
import seaborn as sns

LOW_LIMIT = 600
HIGH_LIMIT = 4500
UPPER_TRESHOLD = 500

def getLatencies(o, above, file):
    f = open(file, "r")
    for line in f:
        lineArr = line.split()
        if (len(lineArr) < 3):
            continue

        timestamp = float(lineArr[0])

        if timestamp < LOW_LIMIT or timestamp > HIGH_LIMIT:
            continue

        latency = float(lineArr[0]) * 1000 - int(lineArr[1])

        if latency > UPPER_TRESHOLD:
            above = above + 1

        o.append(latency)


    return o, above


def average(lst):
    return sum(lst) / len(lst)


f26 = []
t26 = []
tf26 = []
f27 = []
t27 = []
tf27 = []

a_f26 = 0
a_t26 = 0
a_tf26 = 0
a_f27 = 0
a_t27 = 0
a_tf27 = 0

for r in range(1, 6):
    (f26, a_f26) = getLatencies(f26, a_f26, 'res/%s-26-10-false.res' % r)
    (t26, a_t26) = getLatencies(t26, a_t26, 'res/%s-26-10-true.res' % r)
    (tf26, a_tf26) = getLatencies(tf26, a_tf26, 'res/%s-26-10-false_true.res' % r)
    (f27, a_f27) = getLatencies(f27, a_f27, 'res/%s-27-0-false.res' % r)
    (t27, a_t27) = getLatencies(t27, a_t27, 'res/%s-27-0-true.res' % r)
    (tf27, a_tf27) = getLatencies(tf27, a_tf27, 'res/%s-27-0-false_true.res' % r)

print('None 27', average(f27), a_f27, max(f27), min(f27), len(f27), np.percentile(f27, 95), np.percentile(f27, 99), statistics.median(f27))
print('Aware 27', average(t27), a_t27, max(t27), min(t27), len(t27), np.percentile(t27, 95), np.percentile(t27, 99), statistics.median(t27))
print('Wait 27', average(tf27), a_tf27, max(tf27), min(tf27), len(tf27), np.percentile(tf27, 95), np.percentile(tf27, 99), statistics.median(tf27))


bins = 50
fig, ax = plt.subplots(ncols=6)
ax = sns.distplot(f26, kde=True, hist=True, hist_kws={"range": [0,500]})
ax = sns.distplot(t26, kde=True, hist=True, hist_kws={"range": [0,500]})
ax = sns.distplot(f27, kde=True, hist=True, hist_kws={"range": [0,500]})
ax = sns.distplot(t27, kde=True, hist=True, hist_kws={"range": [0,500]})
ax = sns.distplot(tf27, kde=True, hist=True, hist_kws={"range": [0,500]})
ax.set_xlim(0, 500)
plt.show()
plt.savefig('pdf-lat.pdf')
