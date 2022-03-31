#!/usr/bin/python
import os, sys
from os import listdir, system
from os.path import isfile, join, isdir
import time
import datetime


def get_pcaps(dir):
	out = []
	out.append("%s/trace-RE1E2-1.pcap" % dir)
	out.append("%s/trace-RE1I1-1.pcap" % dir)
	out.append("%s/trace-RE2I1-1.pcap" % dir)
	out.append("%s/trace-RI1D1-1.pcap" % dir)
	return out

def get_sum(file):
	starttime = time.strptime('02:00:00.0', '%H:%M:%S.%f')
	sum = 0
	for line in open(file):
		lineArr = line.split(' ')
		#print lineArr
		if len(lineArr[0]) > 8:
			timestamp = time.strptime(lineArr[0], '%H:%M:%S.%f')
			delta = (time.mktime(timestamp) - time.mktime(starttime))
			if delta > 100:
				pos = lineArr.index('length')
				length = int(lineArr[pos+1])
				sum = sum + length
		else:
			print '#skipping line ',line,file

	return sum/1000

output_file = '%s/D1/output.res' % sys.argv[1]
adapt_file = '%s/A1/adapt.res' % sys.argv[1]
start_file = '%s/I1/start.res' % sys.argv[1]
original_file = '%s/A1/original.res' % sys.argv[1]
pcap_dir = sys.argv[1]

f = open(original_file, "r")
fcount = 0
for x in f:
	lineArr = x.split()
	if (len(lineArr) < 1):
		continue
	if (float(lineArr[0]) < 100):
		continue
	val = float(lineArr[1])
	if val >= 20:
		fcount = fcount + 1


f = open(output_file, "r")

count = 0
sum_delay = 0

for x in f:
	lineArr = x.split()

	if (len(lineArr) < 2):
		#		print 'error on line', lineArr
		continue

	start = float(lineArr[1])
	end = float(lineArr[0])
	if (start < 100):
		continue
	sum_delay = sum_delay + (end - start)

	count = count + 1

# Find error for each predicted value
if isfile(adapt_file):
	sum_error = 0
	val_count = 0
	f = open(adapt_file, "r")
	for l in f:
		lineArr = l.split()

		if (len(lineArr) < 10):
			continue
		start = float(lineArr[0])

		if (start < 100):
			continue
		val = float(lineArr[1])
		if (val == -1):
			continue
		try:
			pred = float(lineArr[3])
		except:
			pred = val
		error = abs(pred-val) / val
		sum_error = error + sum_error
		val_count = val_count + 1

	if val_count == 0:
		error = 0
	else:
		error = float(sum_error) / float(val_count)
else:
	error = 0


# Runs through the pcap files and summarized the bandwidth consumed on all cap devices
bw_sum = 0

for f in get_pcaps(pcap_dir):
	#print '> Analysing file: ',f
	system('tcpdump -r %s -n ip 2> /dev/null | grep Flags > %s_bw.tmp ' % (f, f))

	bw_sum = bw_sum + get_sum('%s_bw.tmp' % f)
	system('rm %s_bw.tmp' % f)


#print '# Lines, loss, average delay, bw_sum'

if count == 0:
	latency = -1
else:
	latency = (sum_delay / count)

print pcap_dir, count, (fcount-count), latency, bw_sum, error



'''
os.system('cat %s|grep arrow > arrows.gp' % adapt_file)
os.system('./rate.py r %s  > rate.res' % output_file)
os.system('./rate.py f %s > frate.res' % original_file)
os.system('cp %s .' % output_file)
os.system('cp %s .' % adapt_file)
os.system('cp %s .' % start_file)
os.system('cp %s .' % original_file)
os.system('cp arrows.gp %s' % pcap_dir)
os.system('gnuplot plot-experiment.dem')
'''
