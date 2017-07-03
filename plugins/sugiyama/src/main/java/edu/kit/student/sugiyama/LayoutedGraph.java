package edu.kit.student.sugiyama;


import edu.kit.student.graphmodel.DirectedSupplementEdgePath;
import edu.kit.student.graphmodel.EdgePath;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.graphmodel.directed.DirectedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A completely layouted graph.
 * Every vertex given has a coordinate,
 * every edge given has a valid {@link EdgePath}.
 * The same has to be valid for the given vertices and edges contained in the given paths.
 *
 */
public class LayoutedGraph {


    private Set<Vertex> vertices;
    private Set<DirectedEdge> edges;
    private Set<DirectedSupplementEdgePath> paths;
    private final Logger logger = LoggerFactory.getLogger(LayoutedGraph.class);

    public LayoutedGraph(Set<Vertex> vertices, Set<DirectedEdge> edges, Set<DirectedSupplementEdgePath> paths){
        this.vertices = new HashSet<>(vertices);
        this.edges = new HashSet<>(edges);
        this.paths = new HashSet<>(paths);
        logger.info("Created LayoutedGraph with " + vertices.size() + " vertices, " + edges.size() + " edges, and " + paths.size() + " paths!");
        checkValidity();
    }

    private void checkValidity(){
        //TODO: maybe check if coordinates of vertices are set(but default is 0 and 0 can occur)

        for(DirectedEdge e : this.edges){
            if(e.getPath().getNodes().isEmpty())
                throw new IllegalArgumentException("EdgePath must not be empty !");
            if(!this.vertices.contains(e.getSource()))
                throw new IllegalArgumentException("Source of edge " + e.toString() + "has to be contained in the vertex set!");
            if(!this.vertices.contains(e.getTarget()))
                throw new IllegalArgumentException("Target of edge " + e.toString() + "has to be contained in the vertex set!");
        }

        for(DirectedSupplementEdgePath p : this.paths){
            DirectedEdge edge = p.getReplacedEdge();
            if(edge.getPath().getNodes().isEmpty())
                throw new IllegalArgumentException("EdgePath of path's replaced edge must not be empty !");
            if(!this.vertices.contains(edge.getSource()))
                throw new IllegalArgumentException("Source of path's replaced edge " + edge.toString() + " has to be contained in the vertex set!");
            if(!this.vertices.contains(edge.getTarget()))
                throw new IllegalArgumentException("Target of path's replaced edge " + edge.toString() + " has to be contained in the vertex set!");
        }
    }

    public Set<Vertex> getVertices() {
        return new HashSet<>(vertices);
    }

    public Set<DirectedEdge> getEdges() {
        return new HashSet<>(edges);
    }

    public Set<DirectedSupplementEdgePath> getPaths() {
        return new HashSet<>(paths);
    }
}
