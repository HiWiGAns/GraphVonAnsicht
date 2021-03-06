package edu.kit.student.graphmodel;

import java.util.List;

public interface GraphLayering<V extends Vertex> {

	/**
	 * Get the amount of layers.
	 * @return the amount of layers that contain at least one vertex
	 */
	public int getLayerCount();
	
	/**
	 * Get the number of vertices which are on a  certain layer
	 * 
	 * @param layerNum the layer number to get the vertex count from
	 * @return 		   the number of vertices which are on this layer
	 */
	public int getVertexCount(int layerNum);
	
	/**
	 * Get the layer from the vertex if vertex is not contained in the graph -1 is returned.
	 * 
	 * @param vertex the vertex to get its layer from
	 * @return  	 the layer number from this vertex
	 */
	public int getLayerFromVertex(Vertex vertex);
	
	/**
	 * Get all vertices from a certain layer.
	 * 
	 * @param layerNum the index of the layer
	 * @return 		 a list of all vertices which are on this layer
	 */
	public List<V> getLayer(int layerNum);
	
	/**
	 * Get all layers that contain vertices.
	 * 
	 * @return a list of lists of vertices which are on this layer
	 */
	public List<? extends List<V>> getLayers();
	
	/**
	 * Returns the height, i.e. the number of layers.
	 * @return the height
	 */
	public int getHeight();
	
	/**
	 * Returns the width of the layer specified by its index, i.e. the number of vertices in the layer.
	 * @param layerN the index of the layer
	 * @return 		 the width of the layer
	 */
	public int getLayerWidth(int layerN);

	/**
	 * Returns the width of the widest layer, i.e. the number of vertices the layer with the most vertices contains.
	 * @return the maximum width 
	 */
	public int getMaxWidth();
	
//	/**
//	 * Returns all subgraphs contained in this graph.
//	 * All subgraphs of layered graphs have to be layered graphs with equal parameters themselves.
//	 * @return subgraphs in this layered graph
//	 */
//	public List<? extends LayeredGraph> getSubgraphs();
}
