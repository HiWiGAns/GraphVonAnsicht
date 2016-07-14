package edu.kit.student.graphmodel;

import java.util.List;
import java.util.Set;

import edu.kit.student.plugin.LayoutOption;


/**
 * This graph interface specifies a graph. A graph contains edges and vertices.
 */
public interface Graph {

	/**
	 * Returns the name of the Graph.
	 * 
	 * @return The name of the graph.
	 */
	public String getName();

	/**
	 * Returns the ID of the graph.
	 * 
	 * @return The id of the graph.
	 */
	public Integer getID();

	/**
	 * Returns all vertices of the graph.
	 * 
	 * @return A set of all vertices of the graph.
	 */
	public Set<? extends Vertex> getVertexSet();

	/**
	 * Returns all edges of the graph.
	 * 
	 * @return A set of all edges of the graph.
	 */
	public Set<? extends Edge> getEdgeSet();

	/**
	 * Returns a list of all edges of a vertex.
	 * 
	 * @param vertex the vertex which edges will be returned.
	 * @return All edges which are connected with the supplied vertex.
	 */
	public Set<? extends Edge> edgesOf(Vertex vertex);

	/**
	 * Returns the FastGraphAccessor of this Graph.
	 *
	 * @return the FastGraphAccessor of this Graph
     */
	public FastGraphAccessor getFastGraphAccessor();

	/**
	 * Adds the graph to a {@link FastGraphAccessor}.
	 * 
	 * @param fga the {@link FastGraphAccessor} to whom this graph will be added.
	 */
	public void addToFastGraphAccessor(FastGraphAccessor fga);
	
	/**
	 * Returns a list of layouts which have been registered at the corresponding
	 * LayoutRegister for this graph type. The graph implementing this interface
	 * will be set as target of the LayoutOption.
	 * 
	 * @return A list of layouts which have been registered at the corresponding
	 *         LayoutRegister for this graph type.
	 */
	public List<LayoutOption> getRegisteredLayouts();

	/**
	 * Returns the default layout for this graph.
	 * This can be called when to quickly get a suiting layout without
	 * having to decide between multiple options.
	 * @return the default layout for this graph
	 */
	public LayoutOption getDefaultLayout();
}
