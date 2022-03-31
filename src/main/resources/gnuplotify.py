#!/usr/bin/python
import sys

input_file_name = sys.argv[1]

for line in open(input_file_name, 'r'):
    line_arr = line.split()
    i = 0
    for arr in line_arr:
        if arr != '\t' and arr != ' ':
            num=arr.replace(',','.')
            if i < len(line_arr) - 1:
                print num,'',
            else:
                print num,
        i = i + 1

    print ''
