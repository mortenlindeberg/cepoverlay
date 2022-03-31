#!/usr/bin/python

import sys
from print_dir import print_dir

def get_start_time(start_filename):
    start_file = open(start_filename, 'r')
    line = start_file.readline()
    return long(line)


# Start ----RelativeStart-------- X RealTime/RelativeTime
# RelativeStart = Real-time - Relative-time
# Offset = RelativeStart - Start
def get_offset(log_file_name, start_time):
    log_file = open(log_file_name, 'r')
    for line in log_file.readlines():
        if 'OverlayInstance initiating' in line:
            line_arr = line.split( )
            relative_time = long(line_arr[0])
            real_time = long(line_arr[10])
            relative_start = (real_time - relative_time)

            return relative_start - start_time


def get_events(log_file_name, offset, event_string):
    log_file = open(log_file_name, 'r')
    event_list = []
    for line in log_file.readlines():
        timestamp_str = u'%s' % line.split(' ')[0]
        if not timestamp_str.isnumeric():
            continue

        timestamp = int(timestamp_str)

        if timestamp < 600000:
            continue

        if event_string in line:
            mig_time = int(line.split()[0])+int(offset)
            event_list.append(mig_time)

    return event_list

def get_duration(log_file_name):
    log_file = open(log_file_name, 'r')
    sum = 0
    count = 0
    for line in log_file.readlines():
        timestamp_str = u'%s' % line.split(' ')[0]
        if not timestamp_str.isnumeric():
            continue

        timestamp = int(timestamp_str)

        if timestamp < 600000:
            continue

        if 'Duration' in line:
            sum = sum + int(line.split( )[12])
            count = count + 1

    return (sum / count)

def get_migration_count(log_file_name):
    log_file = open(log_file_name, "r")
    log_data = log_file.read()
    return log_data.count('Duration:')



experiment_dir = sys.argv[1]

start_time = get_start_time('%s/A1/start.res' % experiment_dir)

c1_offset = get_offset('%s/C1/app.log' % experiment_dir, start_time)
j1_offset = get_offset('%s/J1/app.log' % experiment_dir, start_time)
j2_offset = get_offset('%s/J2/app.log' % experiment_dir, start_time)

c1_sends = get_events('%s/C1/app.log' % experiment_dir,  c1_offset, 'Send snapshot from')
j1_sends = get_events('%s/J1/app.log' % experiment_dir,  j1_offset, 'Send snapshot from')
j2_sends = get_events('%s/J2/app.log' % experiment_dir,  j2_offset, 'Send snapshot from')
sends = c1_sends + j1_sends + j2_sends

c1_starts = get_events('%s/C1/app.log' % experiment_dir,  c1_offset, 'migrated snapshot (operator) started')
j1_starts = get_events('%s/J1/app.log' % experiment_dir,  j1_offset, 'migrated snapshot (operator) started')
j2_starts = get_events('%s/J2/app.log' % experiment_dir,  j2_offset, 'migrated snapshot (operator) started')
starts = c1_starts + j1_starts + j2_starts

starts.sort()
sends.sort()

freezes = []

for i in range(0, len(starts)):
    freeze = starts[i] - sends[i]
    assert(freeze > 0)
    freezes.append(freeze)

print sum(freezes) / len(freezes), get_duration('%s/C1/app.log' % experiment_dir), get_migration_count('%s/C1/app.log' % experiment_dir)
