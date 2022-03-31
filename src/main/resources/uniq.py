#!/usr/bin/python
import os, sys

output_file = '%s/D1/output.res' % sys.argv[1]
original_file = '%s/A1/original.res' % sys.argv[1]


os1 = []

rs1 = []
rs2 = []

f = open(original_file, "r")
for x in f:
    lineArr = x.split()

    if (len(lineArr) < 2):
        continue
    if float(lineArr[1]) < 20:
       continue

    val = float(lineArr[0])
    if not val in os1:
	os1.append(val)

#print '# Seeing if values are above max:',max

f = open(output_file, "r")
for x in f:
    lineArr = x.split()

    if (len(lineArr) < 2):
	continue

    val = float(lineArr[1])
    val2 = float(lineArr[2])

    if not val in rs1:
       rs1.append(val)

    if not val2 in rs2:
       rs2.append(val2)

#print len(os1), len(rs1), len(rs2)

for s1 in os1:
	if not s1 in rs1:
		print s1    
    
