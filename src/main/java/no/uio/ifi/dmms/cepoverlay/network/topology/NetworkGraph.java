package no.uio.ifi.dmms.cepoverlay.network.topology;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkGraph {
    private static final Logger log = Logger.getLogger(NetworkGraph.class.getName());
    private Graph<Instance, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);


    public NetworkGraph(List<Instance> instances) {
        for (Instance instance : instances) graph.addVertex(instance);
    }

    public void addEdge(Instance i, Instance j) {
        graph.addEdge(i, j);
        graph.setEdgeWeight(i, j, 1);

        graph.addEdge(i, j);
    }

    public List<Instance> getShortestRoute(Instance source, Instance destination) {
        List<Instance> route = DijkstraShortestPath.findPathBetween(graph, source, destination).getVertexList();

        return route;
    }

    public Graph<Instance, DefaultWeightedEdge> getPlacementTree(List<Instance> sources, Instance destination) {
        List<Instance> steinerNodes = new ArrayList<>();
        steinerNodes.add(destination);
        steinerNodes.addAll(sources);

        return SteinerTree.getSteinerTree(graph, steinerNodes);
    }

    public Instance getBranchingPoint(Graph<Instance, DefaultWeightedEdge> g, Instance source, Instance destination) {
        Graph<Instance, DefaultWeightedEdge> gr = g;
        List<Instance> route = DijkstraShortestPath.findPathBetween(graph, source, destination).getVertexList();

        for (Instance instance : route) {
            if (gr.degreeOf(instance) > 1)
                return instance;
        }

        return null;
    }

    public Instance findJoinPlacement(Instance source1, Instance source2, Instance destination) {
        /* Find the Steiner Tree */
        Graph<Instance, DefaultWeightedEdge> steinerTree = getPlacementTree(Arrays.asList(source1, source2), destination);
        System.out.println(steinerTree);
        /* Find the dominant input stream */
        Instance dominantNode;
        if (source1.getPredictedInputRate() > source2.getPredictedInputRate())
            dominantNode = source1;
        else if (source1.getPredictedInputRate() < source2.getPredictedInputRate())
            dominantNode = source2;
        else { /* if predicted output is equal, use steiner tree and branching pont as the join location */
            return getBranchingPoint(steinerTree, source1, destination);
        }

        /* If dominant node is in the Steiner Tree, return the dominant node */
        if (steinerTree.vertexSet().contains(dominantNode)) return dominantNode;

        /* Otherwise find the node in the Steiner Tree that is closest to the dominant source */
        Instance candidate = null;
        int candidateDistance = Integer.MAX_VALUE;
        for (Instance i : steinerTree.vertexSet()) {
            int distance = getShortestRoute(i, dominantNode).size();
            if (distance < candidateDistance) {
                candidateDistance = distance;
                candidate = i;
            }
        }
        return candidate;
    }

    public void addVertex(Instance vertex) {
        graph.addVertex(vertex);
    }
}
