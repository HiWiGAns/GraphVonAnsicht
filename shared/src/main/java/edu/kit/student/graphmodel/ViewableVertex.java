package edu.kit.student.graphmodel;

public interface ViewableVertex extends Vertex {
    
    /**
     * Returns the id of the graph this linked vertex links to.
     * Returns -1 when no link is set.
     * @return the id of the graph
     */
    VertexReference getLink();
    
    VertexPriority getPriority();
    
    /**
     * Indicates the priority of a vertex.
     * A LOW priority vertex will be drawn in the background and will not be selectable.
     * A HIGH priority vertex will be drawn in the foreground and is selectable.
     * If extended the GraphViewGraphFactory must be extended as well, 
     * unknown priorities will be interpreted as HIGH-
     */
    enum VertexPriority {
    	HIGH, LOW
    }
}
