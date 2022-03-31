package no.uio.ifi.dmms.cepoverlay.network.topology;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RouteHelper {
    private static Logger log = Logger.getLogger(RouteHelper.class.getName());

    private Map<String, Map<String, Integer>> routerInterfaceMap = new HashMap();
    private Map<String, FileWriter> linuxFiles = new HashMap();
    private Map<String, FileWriter> ns3Files = new HashMap();
    private final NetworkGraph networkGraph;
    private List<Instance> routers = new ArrayList();

    public RouteHelper(List<Instance> instances, String linkString, String routerAddress) {
        networkGraph = new NetworkGraph(instances);
        String[] links = linkString.split(",");
        int num = instances.size() + 1; // We start the IP address with n+1 to allow networks without subnets.

        // Create file writers for the routes for the nodes
        try {
            for (Instance i : instances) {
                FileWriter fwLinux = new FileWriter("routes" + File.separator + i.getInstanceName() + "-linux-routes.sh");
                fwLinux.write("#!/bin/bash\n");
                linuxFiles.put(i.getInstanceName(), fwLinux);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add the routers, using same strategy as in ns-3 script (HIGHLY IMPORTANT!)
        for (String link : links) {
            String[] linkDetails = link.split("-");
            Instance from = getInstanceByName(instances, linkDetails[0]);
            Instance to = getInstanceByName(instances, linkDetails[1]);
            String[] addresses = {getRouterIp(routerAddress, num++), getRouterIp(routerAddress, num++)}; // Routers have two IP addresses

            Instance router = new Instance("R" + from.getInstanceName() + to.getInstanceName(), Arrays.asList(addresses), -1,1, from.getLinkRate(),1);
            Map<String, Integer> ifMap = new HashMap();

            ifMap.put(from.getInstanceName(), 1);
            ifMap.put(to.getInstanceName(), 2);
            routerInterfaceMap.put(router.getInstanceName(), ifMap); // This is to keep track of which interface we can use later to route towards each of the linked nodes

            routers.add(router);

            networkGraph.addVertex(router);
            if (from != null && to != null) {
                networkGraph.addEdge(from, router);
                networkGraph.addEdge(router, to);
            } else
                log.error("Unrecognized node link: " + link);
        }


        // Create file writers for the routes of the routers
        try {
            for (Instance i : routers) {
                ns3Files.put(i.getInstanceName(), new FileWriter("routes" + File.separator + i.getInstanceName() + "-ns3-routes.txt"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Instance> getRouters() {
        return routers;
    }

    private String getRouterIp(String routerAddress, int i) {
        String[] s = routerAddress.split("x");
        if (s.length > 1)
            return s[0] + i + s[1];
        else return s[0] + i;
    }

    private int getIPRank(String address) {
        String[] addressArray = address.split("\\.");
        int rank = 0;
        for (int i = 0; i < addressArray.length; i++) {
            rank += Integer.parseInt(addressArray[i]) * Math.pow(10, addressArray.length - i - 1);
        }
        return rank;
    }
    private boolean isAbove(Instance source, Instance destination) {
        return (getIPRank(source.getAddress()) > getIPRank(destination.getAddress()));
    }

    public void createRouteForLinux(Instance source, Instance destination) {
        List<Instance> route = networkGraph.getShortestRoute(source, destination);
        int iface;
        if (isAbove(source, route.get(2))) // If IP address of source is higher than next hop (following the router), we should use the upper interface of the router noded.
            iface = 1;

        else
            iface = 0;
        try {
            linuxFiles.get(source.getInstanceName()).append("route add " + destination.getAddress() + " gw " + route.get(1).getAddress(iface) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createRouteForNs3(Instance source, Instance destination) {
        List<Instance> route = networkGraph.getShortestRoute(source, destination);
        int rIf = routerInterfaceMap.get(source.getInstanceName()).get(route.get(1).getInstanceName());
        try {
            ns3Files.get(source.getInstanceName()).append(destination.getAddress() + " " + route.get(1).getAddress() + " " + rIf + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void closeFiles() {
        try {
            for (String key : linuxFiles.keySet())
                linuxFiles.get(key).close();

            for (String key : ns3Files.keySet())
                ns3Files.get(key).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static NetworkGraph readNetworkGraph(String linkString, List<Instance> instances) {
        NetworkGraph networkGraph = new NetworkGraph(instances);
        String[] links = linkString.split(",");
        for (String link : links) {
            String[] linkDetails = link.split("-");
            Instance from = getInstanceByName(instances, linkDetails[0]);
            Instance to = getInstanceByName(instances, linkDetails[1]);
            if (from != null && to != null)
                networkGraph.addEdge(from, to);
            else
                log.error("Unrecognized node link: " + link);
        }

        return networkGraph;
    }

    public static Instance getInstanceByName(List<Instance> instances, String name) {
        for (Instance instance : instances) {
            if (instance.getInstanceName().equals(name))
                return instance;
        }
        log.debug("Instance " + name + " not in " + instances);
        return null;
    }
}
