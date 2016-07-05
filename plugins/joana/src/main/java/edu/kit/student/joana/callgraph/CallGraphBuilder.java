package edu.kit.student.joana.callgraph;

import edu.kit.student.graphmodel.builder.IEdgeBuilder;
import edu.kit.student.graphmodel.builder.IGraphBuilder;
import edu.kit.student.graphmodel.builder.IVertexBuilder;
import edu.kit.student.joana.JoanaCompoundVertex;
import edu.kit.student.joana.JoanaEdge;
import edu.kit.student.joana.JoanaEdgeBuilder;
import edu.kit.student.joana.JoanaVertex;
import edu.kit.student.joana.methodgraph.MethodGraph;
import edu.kit.student.joana.methodgraph.MethodGraphBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The CallGraphBuilder implements an {@link IGraphBuilder} and builds 
 * one {@link CallGraph}.
 */
public class CallGraphBuilder implements IGraphBuilder {

    Set<MethodGraphBuilder> methodGraphBuilders = new HashSet<>();
    Set<MethodGraph> methodGraphs = new HashSet<>();
    String name;
    
    public CallGraphBuilder(String name) {
        this.name = name;
    }
    
    @Override
    public IEdgeBuilder getEdgeBuilder() {
        return new JoanaEdgeBuilder();
    }

    @Override
    public IVertexBuilder getVertexBuilder(String vertexId) {
        //TODO: throw exception
        return null;
    }

    @Override
    public IGraphBuilder getGraphBuilder(String graphId) {
        MethodGraphBuilder builder = new MethodGraphBuilder(graphId);
        methodGraphBuilders.add(builder);
        return builder;
    }
    
    /**
     * Builds a new CallGraph with the given information, added before this call.
     * @return the callgraph
     */
    public CallGraph build() {
        for (MethodGraphBuilder b : methodGraphBuilders) {
            methodGraphs.add(b.build());
        }
        HashMap<String, JoanaCompoundVertex> vertices = new HashMap<>();
        HashMap<JoanaCompoundVertex, Set<JoanaEdge<JoanaCompoundVertex>>> edges = new HashMap<>();

        // Generate Callgraph
        // Generate method vertices.
        for (MethodGraph b : methodGraphs) {
            vertices.put(b.getName(), new JoanaCompoundVertex(b.getName(), b.getName(), b));
        }
        // Add call edges between vertices.
        for (MethodGraph b : methodGraphs) {
            JoanaCompoundVertex source = vertices.get(b.getName());
            edges.put(source, new HashSet<JoanaEdge<JoanaCompoundVertex>>());

            // Search for method calls.
            for (JoanaEdge<JoanaVertex> e : b.getEdgeSet()) {
                if (e.getEdgeKind() == edu.kit.student.joana.JoanaEdge.Kind.CL) {
                    if (vertices.containsKey(e.getName())) {
                        JoanaCompoundVertex target = vertices.get(e.getName());
                        if (edges.get(source).contains(target)) {
                            // Second call from this function. Skip.
                            continue;
                        }
                        JoanaEdge<JoanaCompoundVertex> edge = new JoanaEdge<JoanaCompoundVertex>(e.getName(),
                                                            e.getLabel(), e.getEdgeKind());
                        edge.setVertices(source, target);
                        edges.get(source).add(edge);
                    } 
                }
            }
        }
        HashSet<JoanaEdge<JoanaCompoundVertex>> merged = new HashSet<>();
        for (Set<JoanaEdge<JoanaCompoundVertex>> edgeSet : edges.values()) {
            merged.addAll(edgeSet);
            
        }
        CallGraph graph = new CallGraph(this.name, 
                new HashSet<JoanaCompoundVertex>(vertices.values()), merged);
        return graph;
    }

}
