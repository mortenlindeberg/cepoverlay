#!/usr/bin/python
import subprocess, sys, os

STEP_LENGTH = 1000
DURATION = 5000000

def read_file(filename):
    out = []
    cmd = "tshark -r %s \"tcp.dstport == 1081\"" % filename

    proc = subprocess.Popen(cmd,
                            shell=True,
                            stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)

    for line in proc.stdout.readlines():
        line_arr = line.split()
        if 'KNXnet' not in line:
            out.append((float(line_arr[1])*1000, int(line_arr[6])))

    return out


def print_array(array, name):
    f = open(name, 'w')

    index = 0
    tuple = array[index]
    timestamp = tuple[0]

    for step in range(0, (DURATION / STEP_LENGTH)):
        sum = 0
        step_limit = step * STEP_LENGTH

        # If array is empty, just print 0
        if index >= len(array):
            f.write("%d %d %d (LAST)\n" % (step_limit, sum, timestamp))

        # Get the new aggregate and print result
        else:
            while timestamp <= step_limit:
                sum = sum + tuple[1]
                index = index + 1
                if index < len(array): # never go beyond the array!
                    tuple = array[index]
                    timestamp = tuple[0]
                else:
                    break

            f.write("%d %d %d (ORD)\n" % (step_limit, sum, timestamp))



def create_bw_files(dir):
    a = []
    for num in (2, 5, 8, 11):
        a.extend(read_file('%s/trace-%d-0.pcap' % (dir, num)))
    a.sort(key=lambda tup: tup[0])

    print_array(a, 'j1.bw')

    b = []
    for num in (14, 17, 20, 23):
        b.extend(read_file('%s/trace-%d-0.pcap' % (dir, num)))
    b.sort(key=lambda tup: tup[0])

    print_array(b, 'j2.bw')


def create_opt_placements():
    f = open('optimal-placements.txt', 'w')
    j1_file = open('j1.bw')
    j2_file = open('j2.bw')

    while True:
        j1_line = j1_file.readline()
        j2_line = j2_file.readline()

        if (j1_line == '' and j2_line == ''):
            break

        if (j1_line == ''):
            j1 = (j2_line.split()[0], 0)
        else:
            j1 = j1_line.split()
        if (j2_line == ''):
            j2 = (j1[0], 0)
        else:
            j2 = j2_line.split()

        assert j1[0] == j2[0]

        if (int(j1[1]) > int(j2[1])):
            f.write('%s J1\n' % j1[0])
        elif (int(j1[1]) < int(j2[1])):
            f.write('%s J2\n' % j1[0])
        else:
            f.write('%s %s\n' % (j1[0], 'NA'))

    f.close()

def ip_to_name(ip):
    if ip == '10.0.0.27':
        return 'C1'
    elif ip == '10.0.0.26':
        return 'J2'
    elif ip == '10.0.0.25':
        return 'J1'
    else:
        return 'Not recognised: [%s]' % ip

def find_placements_file(file, offset):
    placement_list = []

    for line in open(file):
        if 'Send snapshot' not in line:
            continue

        line_arr = line.split(' ')
        timestamp = int(line_arr[0]) + offset
        if "DEBUG" in line:
            index = 12
        else:
            index = 13
        to_node = ip_to_name(line_arr[index].rstrip())
        placement_list.append((timestamp, to_node))

    return placement_list

def write_placement_list(p_list):
    f = open('placement-list.txt', 'w')
    for line in p_list:
        f.write('%d %s\n' % (line[0], line[1]))
    f.close()

def find_placements(dir):
    placement_list = []
    c1_offset = get_c_offset(dir)
    j1_offset = get_j_offset(dir, 'J1')
    j2_offset = get_j_offset(dir, 'J2')

    c1_list = find_placements_file('%s/C1/app.log' % dir, c1_offset)
    placement_list.extend(c1_list)

    j1_list = find_placements_file('%s/J1/app.log' % dir, j1_offset)
    placement_list.extend(j1_list)

    j2_list = find_placements_file('%s/J2/app.log' % dir, j2_offset)
    placement_list.extend(j2_list)

    placement_list.sort(key=lambda tup: tup[0])
    write_placement_list(placement_list)
    f = open('real-placements.txt', 'w')
    time_step = 0
    last_placement = 'C1'
    for placement in placement_list:
        while time_step <= placement[0]:
            f.write('%d %s %d %s %f %f\n' % (time_step, last_placement, placement[0], placement[1], j1_offset, j2_offset))
            time_step = time_step + STEP_LENGTH
        last_placement = placement[1]

    f.write('%d %s %d %s %f %f\n' % (time_step, last_placement, placement[0], placement[1], j1_offset, j2_offset))

    while time_step < DURATION:
        time_step = time_step + STEP_LENGTH
        f.write('%d %s %d %s %f %f\n' % (time_step, last_placement, placement[0], placement[1], j1_offset, j2_offset))

    f.close()


def compare_placements():
    real_file = open('real-placements.txt')
    optimal_file = open('optimal-placements.txt')
    sum = 0

    while True:
        r_line = real_file.readline()
        o_line = optimal_file.readline()

        if r_line == '' or o_line == '':
            break

        r = r_line.split()
        o = o_line.split()

        if r[1] == o[1]:
            sum = sum + 1

        print r[0], o[0], sum, r[1], o[1]

    real_file.close()
    optimal_file.close()


def get_j_offset(dir, node):
    if node == 'J1':
        pcap_num = 24
    else:
        pcap_num = 25

    app_cmd = "cat %s/%s/app.log |grep \"1080 Overlay shutting down\"|awk '{print $1}'" % (dir, node)
    shark_cmd = "tshark -r %s/trace-%d-0.pcap \"tcp.dstport == 1080 and (frame.time >= %s\"Jan 1, 1970 02:00:00%s\")\" 2> /dev/null |tail -n 1 |awk {'print $2'}" % (
    dir, pcap_num, chr(92), chr(92))

    proc = subprocess.Popen(app_cmd,
                            shell=True,
                            stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)

    for line in proc.stdout.readlines():
        app_time = int(line)

    proc = subprocess.Popen(shark_cmd,
                            shell=True,
                            stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)

    for line in proc.stdout.readlines():
        shark_time = float(line)*1000
    offset = (shark_time - app_time)
    return offset


def get_c_offset(dir):
    app_cmd = "cat %s/C1/app.log|grep \"Sending abort late\"|head -n 1|awk '{print $1}'" % dir
    shark_cmd = "tshark -r %s/trace-26-0.pcap \"frame contains AbortLateArrival\" 2> /dev/null|head -n 1|awk '{print $2}'" % dir

    proc = subprocess.Popen(app_cmd,
                            shell=True,
                            stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)

    for line in proc.stdout.readlines():
        app_time = int(line)

    proc = subprocess.Popen(shark_cmd,
                            shell=True,
                            stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)

    for line in proc.stdout.readlines():
        shark_time = float(line)*1000

    offset = (shark_time - app_time)
    return offset


level = int(sys.argv[1])
dir = sys.argv[2]

if level > 4:
    os.system('rm *.bw')
    create_bw_files(dir)

if level > 3:
    os.system('rm optimal-placements.txt')
    create_opt_placements()

if level > 2:
    os.system('rm real-placements.txt')
    find_placements(dir)

if level > 1:
    compare_placements()

'''
for r in 1 2 3 4 5; do for s in 26 27; do for w in true false false_true; do ./find-optimal-placement.py 5 run\_$r/extra_large_pamap\_$s\_4\_$w > $r-$s-$w.plc; done; done; done

for r in 1 2 3 4 5; do for s in 26 27; do for w in true false false_true; do printf "\n$r-$s-$w " cat run\_$r/extra_large_pamap\_$s\_4\_$w |grep "1080 Overlay shutting down" ; done; done; done
for r in 1 2 3 4 5; do for s in 26 27; do for w in true false false_true; do for node in C1 J1 J2; do printf "$r-$s-$w-$node " ; cat run\_$r/extra_large_pamap\_$s\_4\_$w/$node/app.log |grep "1080 Overlay shutting down" ; done; done; done; done
'''
