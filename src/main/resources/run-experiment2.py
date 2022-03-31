#!/usr/bin/python

import argparse, sys, os, subprocess, random, time
from collections import OrderedDict
from datetime import datetime


jcmd = "java -jar cepoverlay-1.0-SNAPSHOT-spring-boot.jar "
containerName = "docker-sim"

def execute(cmd):
    #print cmd
    os.system(cmd)

def start(arg, index, cpu_limit, strategy):
    # Start the containers
    execute("sudo docker run --privileged -dit --cpus=\"%s\" --net=none --name %s %s &> docker.log" % (cpu_limit, arg, containerName))
    time.sleep(2)

    # Set up the bridges and tap devices
    execute("sudo brctl addbr br-%s" % arg)
    execute("sudo tunctl -t tap-%s" % arg)
    execute("sudo ifconfig tap-%s 0.0.0.0 promisc up" % arg)
    execute("sudo brctl addif br-%s tap-%s" % (arg,arg))
    execute("sudo ifconfig br-%s up" % arg)
    execute("for f in /proc/sys/net/bridge/bridge-nf-*; do echo 0 > $f; done")

    # Attach the containers to the bridges / tap devices
    pid = get_pid(arg)
    mac = new_mac_addr()
    side_a = "side-int-%s" % arg
    side_b = "side-ext-%s" % arg

    # print "--> PID: %s" % pid
    # print "--> MAC: %s" % mac
    # print "--> Name / Index: %s/%s" % (arg, index)
    # print "--> IP addr 10.0.0.%d" % (int(index)+1)

    # Create namespace entry in /var/run/netns/ (https://github.com/chepeftw/NS3DockerEmulator/blob/master/net/container.sh)
    execute("sudo mkdir -p /var/run/netns")
    execute("sudo ln -s /proc/%s/ns/net /var/run/netns/%s" % (pid,pid))

    # Create pair of interfaces A and B (https://github.com/chepeftw/NS3DockerEmulator/blob/master/net/container.sh)
    execute("sudo ip link add %s type veth peer name %s" % (side_a,side_b))
    execute("sudo brctl addif br-%s %s" % (arg, side_a))
    execute("sudo ip link set %s up" % (side_a))

    # Place B inside containers network namespace (https://github.com/chepeftw/NS3DockerEmulator/blob/master/net/container.sh)
    execute("sudo ip link set %s netns %s" % (side_b, pid))
    execute("sudo ip netns exec %s ip link set dev %s name enp0s3" % (pid,side_b))
    execute("sudo ip netns exec %s ip link set enp0s3 address %s" % (pid, mac))
    execute("sudo ip netns exec %s ip link set enp0s3 up" % pid)
    execute("sudo ip netns exec %s ip addr add 10.0.0.%s/16 dev enp0s3" % (pid,(int(index)+1)))

    # Copy the latest jar of the application file into each of the containers
    execute("sudo docker cp cepoverlay-1.0-SNAPSHOT-spring-boot.jar %s:/" % arg)

    # Copy the config.properties file into each of the containers
    execute("sudo docker cp config.properties %s:/" % arg)

    if strategy > 19:
        # Copy the .dat files for the Activity experiment
        execute("sudo docker exec %s mkdir PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream1.dat %s:/PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream2.dat %s:/PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream3.dat %s:/PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream4.dat %s:/PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream5.dat %s:/PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream6.dat %s:/PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream7.dat %s:/PAMAPData" % arg)
        execute("sudo docker cp PAMAPData/stream8.dat %s:/PAMAPData" % arg)

    else:
        # Copy the .dat files for the LoadTemp experiment
        execute("sudo docker cp rte-temp-hourly-LFBD.dat %s:/" % arg)
        execute("sudo docker cp rte-temp-hourly-LFLL.dat %s:/" % arg)
        execute("sudo docker cp rte-temp-hourly-LFLY.dat %s:/" % arg)
        execute("sudo docker cp rte-temp-hourly-LFML.dat %s:/" % arg)
        execute("sudo docker cp rte-temp-hourly-LFPG.dat %s:/" % arg)
        execute("sudo docker cp rte-temp-hourly-LFPO.dat %s:/" % arg)
        execute("sudo docker cp rte-temp-hourly-LFQQ.dat %s:/" % arg)
        execute("sudo docker cp rte-temp-hourly-LFRS.dat %s:/" % arg)
        execute("sudo docker cp rte.dat %s:/" % arg)

    # Copy the shell script which sets up the routes
    execute("sudo docker exec %s mkdir routes" % arg)
    execute("sudo docker cp routes/%s-linux-routes.sh %s:/routes" % (arg, arg))
    execute("sudo docker exec %s chmod 700 ./routes/%s-linux-routes.sh" % (arg, arg))
    execute("sudo docker exec %s ./routes/%s-linux-routes.sh" % (arg, arg))
    #execute("sudo docker exec %s sar -o load.sar 1 9999 >/dev/null 2>&1 &" % arg)

def get_pid(cName):
    cmd = ['docker', 'inspect', '--format', "'{{ .State.Pid }}'", cName]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (out, err) = proc.communicate()
    return out[1:-2].strip()

def new_mac_addr(): # https://stackoverflow.com/questions/8484877/mac-address-generator-in-python
    mac = [ 0x00, 0x24, 0x81,
            random.randint(0x00, 0x7f),
            random.randint(0x00, 0xff),
            random.randint(0x00, 0xff) ]

    return ':'.join(map(lambda x: "%02x" % x, mac))

def killAll():
    # Ensure that ns-3 is not running
    execute("for pid in `ps -ef | grep scratch | awk '{print $2}'` ; do sudo kill $pid ; done")

    # Kill all containers:
    execute("sudo docker stop $(sudo docker ps -aq) &> docker.log")
    execute("sudo docker rm $(sudo docker ps -aq) &> docker.log")

def stop(arg):
    # Tear down the bridges and tap devices
    execute("sudo ip link delete side-int-%s" % arg)
    execute("sudo ifconfig br-%s down" % arg)
    execute("sudo brctl delif br-%s tap-%s" % (arg,arg))
    execute("sudo brctl delbr br-%s" % arg)
    execute("sudo ip link set dev tap-%s down" % arg)
    execute("sudo tunctl -d tap-%s" % arg)

def parse_nodes(nodelist):
    nodemap = OrderedDict()
    for node in nodelist:
        nodeinfo = node.split(":")
        nodemap[nodeinfo[0]] = {"ip": nodeinfo[1], "port" : nodeinfo[2], "cpu-limit" : nodeinfo[3], "running" : False}

    return nodemap

def get_kill_list(source, nodemap):
    out = ""

    for node in nodemap:
        if node != source:
            if len(out) > 0:
                out = "%s," % out
            out = "%s%s:%s:%s:%s" % (out,node,nodemap[node]["type"],nodemap[node]["ip"],nodemap[node]["port"])

    return out

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--start", help="The start command which will end up starting everything", action="store_true")
    parser.add_argument("--stop", help="The stop command which will end up stopping everything", action="store_true")
    parser.add_argument("--rebuild", help="This command will force docker images to be rebuilt", action="store_true")
    parser.add_argument("--nodes", help="Comma separated list of nodes in format [Node name]:[Type]:[IP address]:[Control port]:[CPU limit]")
    parser.add_argument("--links", help="Comma separated list of links in format [Source]-[Destination]")
    parser.add_argument("--exp_name", help="Please name the experiment")
    parser.add_argument("--sources", help="Specify  the source of a data stream (source type) - comma separated ([name]:[stream ID]:[local address]:[local port]:[remote address]:[remote port]:[sleep]:[noise] ).")
    parser.add_argument("--master_node", help="Name of the master node")
    parser.add_argument("--strategy", help="Integer specifying strategy")
    parser.add_argument("--duration", help="Duration of experiment in seconds")

    args = parser.parse_args()

    if args.start:
        file = open("current.txt", "w")
        file.write("%s" % vars(args))
        file.close()

        nodemap=parse_nodes(args.nodes.split(","))
        master_node=args.master_node

        # Determine the routes using the java helper function
        execute_str = "%s -router 10.0.0.x -nodes %s -links %s" % (jcmd, args.nodes, args.links)
        execute(execute_str)

        count = 0
        # Create the virtual tap interfaces:
        for node in nodemap:
            start(node, count, nodemap[node]["cpu-limit"], args.strategy)
            count = count + 1

        # Start ns-3
        execute("cd ns-3.29; rm *.pcap 2> /dev/null; ./waf --run \"scratch/cepoverlay --nodes=%s --links=%s\"& cd .." % (args.nodes, args.links))
        time.sleep(12)
        #execute("sar -o load.sar 1 9999 >/dev/null 2>&1 &")
        execute("docker stats > dstats.txt&")

        # Write starttime
        execute("%s -timestamp" % jcmd)
        for node in nodemap:
            execute("sudo docker cp start.res %s:/" % node)
            execute("docker exec -d %s bash -c 'hwclock -s &> clock.log'" % node)
            time.sleep(1)

        time.sleep(4)

        print("\n -- Start by running the overlay on involved nodes --")
        for node in nodemap:
            execute_str = "docker exec -d %s bash -c '%s -normal -name %s -la %s -lp %s  &> app.log'" % (node, jcmd, node, nodemap[node]["ip"], nodemap[node]["port"])
            execute(execute_str)

        time.sleep(15)

        print("\n -- Running the sources --")
        for source in args.sources.split(','):
            source_node = source.split(":")[0]
            stream_id = source.split(":")[1]
            local_address = source.split(":")[2]
            local_port = source.split(":")[3]
            remote_address = source.split(":")[4]
            remote_port = source.split(":")[5]
            sleep = source.split(":")[6]
            noise = source.split(":")[7]

            if (len(source.split(":")) > 8):
                prolong = source.split(":")[8]
            else:
                prolong = 0
            if (len(source.split(":")) > 9):
                frequency = source.split(":")[9]
            else:
                frequency = 0

            strategy = args.strategy

            execute_str = "docker exec -d %s bash -c '%s -source -sid %s -la %s -lp %s -ra %s -rp %s -sleep %s -noise %s -prolong %s -frequency %s -strategy %s > source_%s_%s.log" % (source_node, jcmd, stream_id, local_address, local_port, remote_address, remote_port, sleep, noise, prolong, frequency, strategy, stream_id, remote_port)
            execute("%s'" % execute_str )

        time.sleep(2)

        print("\n -- Executing the master --")
        strategy = args.strategy
        execute_str = "docker exec -d %s bash -c '%s -master -strategy %s -duration %s > master.log'" % (master_node, jcmd, strategy, args.duration)
        execute(execute_str)


    elif args.stop:
        # Store the experiment data
        nodemap=parse_nodes(args.nodes.split(","))
        folder_name = args.exp_name

        # Get the SAR results and place them in experiment directory
        #execute("killall sar")
        execute("k=`ps ax |grep \"docker stats\"|egrep -v grep|awk '{print $1}'`; kill -9 $k") # Kill the process that runs the docker stats cmd

        # Metadata about the execution (arguments)
        execute("mkdir %s; mv current.txt %s; mv finished.run %s 2> /dev/null" % (folder_name, folder_name, folder_name))
        #execute("mv load.sar %s" % folder_name)
        execute("mv dstats.txt %s" % folder_name)
        execute("mv config.properties %s" % folder_name)

        # Tracefiles from ns-3
        execute("mv ns-3.29/*.pcap %s" % folder_name)

        for node in nodemap:
            folder_name_node = "%s/%s" % (folder_name, node)
            execute("docker exec -d %s bash -c 'mkdir experiment_run;cp *.log experiment_run; cp *.res experiment_run; cp *.sar experiment_run'" % (node))
            execute("mkdir %s" % folder_name_node)
            execute("docker cp %s:/experiment_run %s" % (node, folder_name_node))
            time.sleep(1)
            execute("mv %s/experiment_run/* %s; rm -rf %s/experiment_run" % (folder_name_node,folder_name_node, folder_name_node))

        execute("sudo rm -rf /var/run/netns") # Kill all the links we created
        killAll()
        nodemap=parse_nodes(args.nodes.split(","))

        for node in nodemap:
            stop(node)

    elif args.rebuild:
        execute("docker system prune -a")
        execute("docker build . -t %s" % containerName)

    else:
        print("I quit! I do not know what the h*** I am supposed to do..")

if __name__ == '__main__':
    main()