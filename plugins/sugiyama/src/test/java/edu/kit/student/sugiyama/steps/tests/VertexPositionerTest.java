package edu.kit.student.sugiyama.steps.tests;

import edu.kit.student.graphmodel.DefaultVertex;
import edu.kit.student.graphmodel.directed.DefaultDirectedGraph;
import edu.kit.student.graphmodel.directed.DirectedEdge;
import edu.kit.student.sugiyama.graph.SugiyamaGraph;
import edu.kit.student.sugiyama.steps.VertexPositioner;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Sven on 05.07.2016.
 */
public class VertexPositionerTest {
    private VertexPositioner positioner;
    @Before
    public void setUp() throws Exception {
        this.positioner = new VertexPositioner();
    }

    @Test
    public void positionVertices() throws Exception {
        DefaultDirectedGraph<DefaultVertex, DirectedEdge<DefaultVertex>> DDGraph = new DefaultDirectedGraph<DefaultVertex, DirectedEdge<DefaultVertex>>("");
        DefaultVertex v1 = new DefaultVertex("v1", "0");
        DefaultVertex v2 = new DefaultVertex("v2", "0");
        DefaultVertex v3 = new DefaultVertex("v3", "1");
        DirectedEdge<DefaultVertex> e1 = new DirectedEdge<DefaultVertex>("e1","");
        DirectedEdge<DefaultVertex> e2 = new DirectedEdge<DefaultVertex>("e2","");
        e1.setVertices(v1, v2);
        e2.setVertices(v1, v3);
        DDGraph.addVertex(v1);
        DDGraph.addVertex(v2);
        DDGraph.addVertex(v3);
        DDGraph.addEdge(e1);
        DDGraph.addEdge(e2);

        SugiyamaGraph SGraph = new SugiyamaGraph(DDGraph);

        for (SugiyamaGraph.SugiyamaVertex vertex : SGraph.getVertexSet()) {
            SGraph.assignToLayer(vertex, Integer.parseInt(vertex.getLabel()));
        }

        positioner.positionVertices(SGraph);
    }

}