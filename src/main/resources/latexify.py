#!/usr/bin/python
import sys

input_file_name = sys.argv[1]

for line in open(input_file_name, 'r'):
    line_arr = line.split()
    i = 0
    for arr in line_arr:
        if arr != '\t' and arr != ' ':
            if i < len(line_arr) - 1:
                if i == 0:
                    print arr, ' & ',
                elif (i % 2) == 0:
                    print '(sd: %s) &' % arr,
                else:
                    print arr,
            else:
                if (i % 2) == 0:
                    print '(sd: %s)' % arr,
                else:
                    print arr,
        i = i + 1

    print '\\\\'
