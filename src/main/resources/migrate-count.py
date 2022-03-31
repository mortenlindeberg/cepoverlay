#!/usr/bin/python

from os import system
from os import path
import sys

count = 0

if (not path.exists("%s/A1/adapt.res" % sys.argv[1])):
	print count
	exit()

for line in open("%s/A1/adapt.res" % sys.argv[1]):
	if line.startswith("set arrow"):
		count = count +1
print count