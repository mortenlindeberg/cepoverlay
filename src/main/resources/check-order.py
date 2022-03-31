#!/usr/bin/python
import os, sys

output_file = '%s/D1/output.res' % sys.argv[1]

prev_val = 0
errors = 0

f = open(output_file, "r")
for x in f:
    lineArr = x.split()

    if (len(lineArr) < 2):
        continue

    val = float(lineArr[1])
    
    if val < prev_val:
        errors = errors + 1
        #print val ,'< ', prev_val
    
    prev_val = val
    
print errors
    
