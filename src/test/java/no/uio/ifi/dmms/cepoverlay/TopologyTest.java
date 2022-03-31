package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.network.topology.NetworkGraph;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TopologyTest {

    //Instance a1 = new Instance("A1","10.0.0.1",1080, 1, "1Mbps", 100);
    //Instance a2 = new Instance("A2","10.0.0.2",1081, 1, "1Mbps", 1);
    Instance e1 = new Instance("E1","10.0.0.3",1082, 1, "1Mbps", 100);
    Instance e2 = new Instance("E2","10.0.0.4",1083, 1, "1Mbps", 1);
    Instance i1 = new Instance("I1","10.0.0.5",1084);
    Instance d1 = new Instance("D1","10.0.0.6",1085);


    //private List<Instance> instances = Arrays.asList(a1,a2, e1,e2,i1,d1);
    private List<Instance> instances = Arrays.asList(e1,e2,i1,d1);

    @Test
    public void testHeuristicPlacement() {
        NetworkGraph graph = new NetworkGraph(instances);
        //graph.addEdge(a1,e1);
        //graph.addEdge(a2,e2);
        graph.addEdge(e1,e2);;
        graph.addEdge(e1,i1);
        graph.addEdge(e2,i1);
        graph.addEdge(i1,d1);
        System.out.println(graph);
        Assert.assertEquals(e1,graph.findJoinPlacement(e1,e2,d1));

        e1.setPredictedInputRate(0);
        e2.setPredictedInputRate(1);

        Assert.assertEquals(e2,graph.findJoinPlacement(e1,e2,d1));
    }


}

