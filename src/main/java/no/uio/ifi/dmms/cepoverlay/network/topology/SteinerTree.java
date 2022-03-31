package no.uio.ifi.dmms.cepoverlay.network.topology;


import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SteinerTree {
    // Based on code found on GitHub: https://github.com/usc-isi-i2/szeke/blob/master/src/main/java/edu/isi/karma/modeling/alignment/SteinerTree.java
    // The approach is taken from the paper "A fast algorithm for steiner trees" by L. Kou et. al.

    public static Graph<Instance, DefaultWeightedEdge> getSteinerTree(Graph<Instance, DefaultWeightedEdge> g, List<Instance> steinerNodes) {

        // Step 1: Create undirected distance graph G_1
        Graph<Instance, DefaultWeightedEdge> g1 = step1(g, steinerNodes);

        // Step 2: Find the minimal spanning tree, T_1, of G_1. (If there are several minimal spanning trees, pick an arbitrary one.)
        //startTime = System.currentTimeMillis();
        Graph<Instance, DefaultWeightedEdge> g2 = step2(g1);

        // Step 3: Construct the subgraph, Gs, of G by replacing each edge in T1 by its
        //         corresponding shortest path in G. (If there are several shortest paths,
        //         pick an arbitrary one.)
        Graph<Instance, DefaultWeightedEdge> g3 = step3(g2, g);

        // Step 4: Find the minimal spanning tree, Ts, of Gs. (If there are several minimal
        //         spanning trees, pick an arbitrary one.)
        Graph<Instance, DefaultWeightedEdge> g4 = step4(g3);

        // Step 5: Construct a Steiner tree, Tn, from Ts by deleting edges in Ts,if necessary,
        //         so that all the leaves in Tn are Steiner points.
        Graph<Instance, DefaultWeightedEdge> g5 = step5(g4, g, steinerNodes);

        return g5;
    }

    // Step 1
    public static Graph<Instance, DefaultWeightedEdge> step1(Graph<Instance, DefaultWeightedEdge> g, List<Instance> steinerNodes) {

        Graph<Instance, DefaultWeightedEdge> gOut = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (Instance n : steinerNodes)
            gOut.addVertex(n);

        BellmanFordShortestPath<Instance, DefaultWeightedEdge> path = new BellmanFordShortestPath<Instance, DefaultWeightedEdge>(g);

        for (Instance n1 : steinerNodes) {
            ShortestPathAlgorithm.SingleSourcePaths<Instance, DefaultWeightedEdge> paths = path.getPaths(n1);

            for (Instance n2 : steinerNodes) {
                if (n1.equals(n2))
                    continue;

                if (gOut.containsEdge(n1, n2))
                    continue;

                DefaultWeightedEdge e = gOut.addEdge(n1, n2);
                gOut.setEdgeWeight(e, paths.getWeight(n2));
            }
        }

        return gOut;
    }

    // Step 2
    private static Graph<Instance, DefaultWeightedEdge> step2(Graph<Instance, DefaultWeightedEdge> g1) {
        SpanningTreeAlgorithm.SpanningTree<DefaultWeightedEdge> minST = new KruskalMinimumSpanningTree<>(g1).getSpanningTree();

        Set<DefaultWeightedEdge> edges = minST.getEdges();
        Set<Instance> nodes = g1.vertexSet();

        Graph<Instance, DefaultWeightedEdge> gOut = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (Instance node : nodes)
            gOut.addVertex(node);

        for (DefaultWeightedEdge e : edges) {
            DefaultWeightedEdge newEdge = gOut.addEdge(g1.getEdgeSource(e), g1.getEdgeTarget(e));
            gOut.setEdgeWeight(newEdge, g1.getEdgeWeight(e));
        }
        return gOut;
    }

    // Step 3
    private static Graph<Instance, DefaultWeightedEdge> step3(Graph<Instance, DefaultWeightedEdge> g2, Graph<Instance, DefaultWeightedEdge> g) {

        Set<DefaultWeightedEdge> edges = g2.edgeSet();
        DijkstraShortestPath<Instance, DefaultWeightedEdge> path = new DijkstraShortestPath<>(g);

        Instance source, target;

        Graph<Instance, DefaultWeightedEdge> gOut = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (DefaultWeightedEdge edge : edges) {
            source = g.getEdgeSource(edge);
            target = g.getEdgeTarget(edge);

            List<DefaultWeightedEdge> pathEdges = path.getPath(source, target).getEdgeList();

            if (pathEdges == null)
                continue;

            for (int i = 0; i < pathEdges.size(); i++) {

                if (gOut.edgeSet().contains(pathEdges.get(i)))
                    continue;

                source = g.getEdgeSource(pathEdges.get(i));
                target = g.getEdgeTarget(pathEdges.get(i));

                if (!gOut.vertexSet().contains(source))
                    gOut.addVertex(source);

                if (!gOut.vertexSet().contains(target))
                    gOut.addVertex(target);

                gOut.addEdge(source, target, pathEdges.get(i));
            }
        }

        return gOut;
    }

    // Step 4
    private static Graph<Instance, DefaultWeightedEdge> step4(Graph<Instance, DefaultWeightedEdge> g3) {
        SpanningTreeAlgorithm.SpanningTree<DefaultWeightedEdge> minST = new KruskalMinimumSpanningTree<>(g3).getSpanningTree();

        Set<DefaultWeightedEdge> edges = minST.getEdges();
        Set<Instance> nodes = g3.vertexSet();

        Graph<Instance, DefaultWeightedEdge> gOut = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (Instance node : nodes)
            gOut.addVertex(node);

        for (DefaultWeightedEdge e : edges) {
            DefaultWeightedEdge newEdge = gOut.addEdge(g3.getEdgeSource(e), g3.getEdgeTarget(e));
            gOut.setEdgeWeight(newEdge, gOut.getEdgeWeight(e));
        }
        return gOut;
    }

    // Step 5
    private static Graph<Instance, DefaultWeightedEdge> step5(Graph<Instance, DefaultWeightedEdge> g4, Graph<Instance, DefaultWeightedEdge> g, List<Instance> steinerNodes) {
        Graph<Instance, DefaultWeightedEdge> gOut = g4;

        List<Instance> nonSteinerLeaves = new ArrayList<>();

        Set<Instance> vertexSet = g4.vertexSet();
        for (Instance vertex : vertexSet) {
            if (gOut.degreeOf(vertex) == 1 && steinerNodes.indexOf(vertex) == -1) {
                nonSteinerLeaves.add(vertex);
            }
        }

        Instance source, target;

        for (int i = 0; i < nonSteinerLeaves.size(); i++) {
            source = nonSteinerLeaves.get(i);
            do {
                DefaultWeightedEdge e = gOut.edgesOf(source).toArray(new DefaultWeightedEdge[0])[0];
                target = g.getEdgeTarget(e);

                // this should not happen, but just in case of ...
                if (target.equals(source))
                    target = g.getEdgeSource(e);

                gOut.removeVertex(source);
                source = target;
            } while (gOut.degreeOf(source) == 1 && steinerNodes.indexOf(source) == -1);

        }
        return gOut;
    }
}
