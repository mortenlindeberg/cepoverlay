#!/usr/bin/python3

import argparse, sys, os, subprocess, random, time
from collections import OrderedDict
from datetime import datetime

jcmd = "java -jar cepoverlay-1.0-SNAPSHOT-spring-boot.jar " # Create a smaller special purpose JAR that does not include the overlay
containerName = "docker-sim"

def execute(cmd):
    print(cmd)
    os.system(cmd)

def start(arg, index, cpu_limit, strategy):
    # Start the containers
    if arg == "A1":
        execute("sudo docker run --privileged -dit -p 8081:8081 --cpus=\"%s\" --net=none --name %s %s &> docker.log" % (cpu_limit, arg, containerName))
    else:
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

    print("--> PID: %s" % pid)
    print("--> MAC: %s" % mac)
    print("--> Name / Index: %s/%s" % (arg, index))
    print("--> IP addr 10.0.0.%d" % (int(index)+1))

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
    execute("sudo docker cp flink.tar.gz %s:/" % arg)
    execute("sudo docker cp cepoverlay-1.0-SNAPSHOT-spring-boot.jar %s:/" % arg) # TODO: Should make a smaller router function that does this, not the jar with the overlay

    # Copy the config.properties file into each of the containers
    execute("sudo docker cp config.properties %s:/" % arg)

    # Copy the shell script which sets up the routes
    execute("sudo docker exec %s mkdir routes" % arg)
    execute("sudo docker cp routes/%s-linux-routes.sh %s:/routes" % (arg, arg))
    execute("sudo docker exec %s chmod 700 ./routes/%s-linux-routes.sh" % (arg, arg))
    execute("sudo docker exec %s ./routes/%s-linux-routes.sh" % (arg, arg))

def get_pid(cName):
    cmd = ['docker', 'inspect', '--format', "'{{ .State.Pid }}'", cName]
    cp = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out = cp.stdout.decode()
    pid = out.split('\'')[1].split('\'')[0]
    return pid

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
        execute("cd ns-3.29; rm *.pcap 2> /dev/null; ./waf --run \"scratch/flink-d --nodes=%s --links=%s\"& cd .." % (args.nodes, args.links))
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

        print("\n -- Start by running flink on involved nodes --")
        for node in nodemap:
            execute_str = "docker exec -d %s bash -c 'tar xzf flink.tar.gz ; rm flink.tar.gz; rm *.jar &> app.log'" % (node)
            execute(execute_str)

        time.sleep(15)


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
            execute("docker exec -d %s bash -c 'mkdir experiment_run;cp flink-1.13.2/log/* experiment_run; cp *.res experiment_run; cp *.sar experiment_run'" % (node))
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