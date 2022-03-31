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

exp = sys.argv[1]

system('rm *.tmp')
system('grep -F \"Query Adapt from node B to node A\" %s/A1/app.log > start.tmp' % exp)
count = 0
for line in open("start.tmp","r"):
	lineArr = line.split(' ')
	start = int(lineArr[6])
	end = int(find_end('B', count, exp))
	print 'B to A',(end-start),start,end
	count = count + 1

system('rm *.tmp')
system('grep -F \"Query Adapt from node A to node B\" %s/A1/app.log > start.tmp' % exp)
count = 0
for line in open("start.tmp","r"):
	lineArr = line.split(' ')
	start = int(lineArr[6])
	end = int(find_end('A', count, exp))
	print 'A to B',(end-start),start,end
	count = count + 1
