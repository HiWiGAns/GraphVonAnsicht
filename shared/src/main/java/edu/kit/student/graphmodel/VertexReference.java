package edu.kit.student.graphmodel;

/**
 * Class whose objects represent an immutable reference to a {@link Vertex} in a {@link GraphModel}.
 *
 * If {@code target == null}, the object represents a reference to a graph.
 * {@code graph} should not be {@code null}.
 */
public class VertexReference {

    private final ViewableGraph graph;
    private final ViewableVertex target;

    public VertexReference(ViewableGraph graph, ViewableVertex target) {
        this.graph = graph;
        this.target = target;
    }

    public ViewableGraph getGraph() {
        return graph;
    }

    public ViewableVertex getTarget() {
        return target;
    }
}
