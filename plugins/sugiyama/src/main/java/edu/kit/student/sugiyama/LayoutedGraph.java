package edu.kit.student.sugiyama;


import edu.kit.student.graphmodel.DirectedSupplementEdgePath;
import edu.kit.student.graphmodel.EdgePath;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.graphmodel.directed.DefaultDirectedSupplementEdgePath;
import edu.kit.student.graphmodel.directed.DirectedEdge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A completely layouted graph.
 * Every vertex given has a coordinate and a certain layer number,
 * every edge given has a valid {@link EdgePath}.
 * The same has to be valid for the given vertices and edges contained in the given paths.
 *
 */
public class LayoutedGraph {


    private Set<? extends Vertex> vertices;
    private Set<? extends DirectedEdge> edges;
    private Set<? extends DirectedSupplementEdgePath> paths;

    public LayoutedGraph(Set<? extends Vertex> vertices, Set<? extends DirectedEdge> edges, Set<? extends DirectedSupplementEdgePath> paths){
        this.vertices = new HashSet<>(vertices);
        this.edges = new HashSet<>(edges);
        this.paths = new HashSet<>(paths);
        checkValidity();
    }

    private void checkValidity(){
        //TODO: maybe check if coordinates of vertices are set(but default is 0 and 0 can occur)

        for(DirectedEdge e : this.edges){
            if(e.getPath().getNodes().isEmpty())
                throw new IllegalArgumentException("EdgePath must not be empty !");
            if(!this.vertices.contains(e.getSource()))
                throw new IllegalArgumentException("Source of edge " + e.toString() + "has to be contained in vertex-to-layer-number-mapping!");
            if(!this.vertices.contains(e.getTarget()))
                throw new IllegalArgumentException("Target of edge " + e.toString() + "has to be contained in vertex-to-layer-number-mapping!");
        }

        for(DirectedSupplementEdgePath p : this.paths){
            DirectedEdge edge = p.getReplacedEdge();
            if(edge.getPath().getNodes().isEmpty())
                throw new IllegalArgumentException("EdgePath of path's replaced edge must not be empty !");
            if(!this.vertices.contains(edge.getSource()))
                throw new IllegalArgumentException("Source of path's replaced edge " + edge.toString() + " has to be contained in vertex-to-layer-number-mapping!");
            if(!this.vertices.contains(edge.getTarget()))
                throw new IllegalArgumentException("Target of path's replaced edge " + edge.toString() + " has to be contained in vertex-to-layer-number-mapping!");
        }
    }

    public Set<? extends Vertex> getVertices() {
        return vertices;
    }

    public Set<? extends DirectedEdge> getEdges() {
        return edges;
    }

    public Set<? extends DirectedSupplementEdgePath> getPaths() {
        return paths;
    }
}
