package no.uio.ifi.dmms.cepoverlay.network.topology;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PlacementModule {
    private static final Logger log = Logger.getLogger(PlacementModule.class.getName());

    private List<Instance> sources;
    private List<Instance> nodes;
    private Instance destination;
    private NetworkGraph graph;

    public PlacementModule() {
        log.debug("> Enabling placement module");
        sources = new ArrayList();
        nodes = new ArrayList();
        int i;
        int j = 1;
        for (i = 1; i <= 8*3; i += 3) {
            //nodes.add(new Instance("T" + j, "10.0.0." + i, 1080, 1, "1Mbps", 100));
            //nodes.add(new Instance("A" + j, "10.0.0." + (i+1), 1080, 1, "1Mbps", 100));
            Instance source = new Instance("P" + j, "10.0.0." + (i+2), 1080, 1, "1Mbps", 100);
            sources.add(source);
            nodes.add(source);
            j++;
        }

        Instance j1 = new Instance("J1", "10.0.0."+i++, 1080, 1, "1Mbps", 100);
        Instance j2 = new Instance("J2", "10.0.0."+i++, 1080, 1, "1Mbps", 100);

        nodes.add(j1);
        nodes.add(j2);

        destination = new Instance("D1", "10.0.0."+i++, 1080, 1, "1Mbps", 100);
        nodes.add(destination);

        graph = new NetworkGraph(nodes);

        /* Create the links */
        for (i = 0; i < 4; i++) {
            graph.addEdge(sources.get(i), j1);
        }

        for (i = 4; i < 8; i++) {
            graph.addEdge(sources.get(i), j2);
        }
        graph.addEdge(j1,j2);
        graph.addEdge(j1, destination);
        graph.addEdge(j2, destination);
    }


    public List<String> findOptimalPlacements(ConcurrentHashMap<String, Boolean> rateMap) {
        int shortest = Integer.MAX_VALUE;
        List<Instance> candidates = new ArrayList<>();
        for (Instance i : nodes) {
            if (!i.getInstanceName().equals("J1") && !i.getInstanceName().equals("J2") && !i.getInstanceName().equals("C1"))
                continue;

            int rate = getRate(rateMap, graph, sources, i);
            if (rate < shortest) {
                candidates =  new ArrayList<>();
                candidates.add(i);
                shortest = rate;
            }
            else if (rate == shortest)
                candidates.add(i);
        }

        List<String> output = new ArrayList<>();

        for (Instance candidate : candidates)
            output.add(candidate.getAddress());

        return output;
    }

    private int getRate(ConcurrentHashMap<String, Boolean> rateChart, NetworkGraph graph, List<Instance> sources, Instance placement) {
        int linkCount = 0;
        /* For each with value = false, calculate hop count to placement node */
        /* Calculate number of hops to the placement node */
        boolean active = false;
        for (Instance from : sources) {
            if (rateChart.get(from.getAddress())) {
                active = true;
                int numLink = graph.getShortestRoute(from, placement).size();
                linkCount += numLink;
            }
        }

        /* Calculate remaining hops to destination */
        if (active) {
            linkCount += graph.getShortestRoute(placement, destination).size();

            return linkCount;
        }
        return Integer.MAX_VALUE;
    }
}
