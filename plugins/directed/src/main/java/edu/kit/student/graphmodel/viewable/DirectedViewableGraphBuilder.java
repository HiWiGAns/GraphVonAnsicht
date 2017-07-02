package edu.kit.student.graphmodel.viewable;

import edu.kit.student.graphmodel.DefaultVertex;
import edu.kit.student.graphmodel.VertexReference;
import edu.kit.student.graphmodel.builder.GraphBuilderException;
import edu.kit.student.graphmodel.builder.GraphBuilderException.BuilderType;
import edu.kit.student.graphmodel.builder.IEdgeBuilder;
import edu.kit.student.graphmodel.builder.IGraphBuilder;
import edu.kit.student.graphmodel.builder.IVertexBuilder;
import edu.kit.student.graphmodel.directed.DefaultDirectedEdge;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectedViewableGraphBuilder implements IGraphBuilder {

	public final String name;
	List<DirectedEdgeBuilder> edgeBuilders = new LinkedList<>();
	List<DefaultVertexBuilder> vertexBuilders = new LinkedList<>();
	List<DirectedViewableGraphBuilder> subraphBuilders = new LinkedList<>();
	private Map<String, String> data = new HashMap<>();
	private Map<DirectedViewableGraphBuilder, List<DirectedEdgeBuilder>> edgesIntoGraphs = new HashMap<>();
	private Map<DirectedViewableGraphBuilder, List<DirectedEdgeBuilder>> edgesFromGraphs = new HashMap<>();
	private Map<DirectedViewableGraphBuilder, List<DirectedEdgeBuilder>> edgesBetweenGraphsSource = new HashMap<>();
	private Map<DirectedViewableGraphBuilder, List<DirectedEdgeBuilder>> edgesBetweenGraphsTarget = new HashMap<>();
	List<DirectedViewableGraph> subGraphs;
	DirectedViewableGraph graph;
	
	public DirectedViewableGraphBuilder(String name) {
		this.name = name;
		subGraphs = new LinkedList<>();
	}
	
	/**
	 * Returns true if the GraphBuilder contains an VertexBuilder, which will create
	 * a vertex with the specified id.
	 * @param id the id of the vertex
	 * @return true if the GraphBuilder contains the VertexBuilder
	 */
	private boolean containsVertexFlat(String id) {
		for (DefaultVertexBuilder vb : vertexBuilders) {
			if (vb.getID().equals(id))
				return true;
		}
		return false;
	}

	/**
	 * Returns true if the GraphBuilder contains an VertexBuilder, which will create
	 * a vertex with the specified id, or if one of the GraphBuilder building a subgraph,
	 * for this graph contains such a VertexBuilder.
	 * @param id the id of the vertex
	 * @return true if the GraphBuilder contains the VertexBuilder
	 */
	private boolean containsVertex(String id) {
		if (containsVertexFlat(id))
			return true;
		for (DirectedViewableGraphBuilder sgb : subraphBuilders) {
			if (sgb.containsVertex(id))
				return true;
		}
		return false;
	}

	@Override
	public IEdgeBuilder getEdgeBuilder(String sourceId, String targetId) {
		// marks if source/target is inside this graph (flat) or in a subgraph
		DirectedEdgeBuilder b =  new DirectedEdgeBuilder(sourceId, targetId);
		this.edgeBuilders.add(b);
		return b;
	}

	@Override
	public IVertexBuilder getVertexBuilder(String vertexID) {
		DefaultVertexBuilder b =  new DefaultVertexBuilder(vertexID);
		this.vertexBuilders.add(b);
		return b;
	}

	@Override
	public IGraphBuilder getGraphBuilder(String graphID) {
		DirectedViewableGraphBuilder sgb = new DirectedViewableGraphBuilder(graphID);
		subraphBuilders.add(sgb);
		return sgb;
	}

	@Override
	public String getId() {
	    return this.name;
	}

	@Override
	public void addData(String keyname, String value) throws IllegalArgumentException {
	    this.data.put(keyname, value);
	}


	private List<DirectedEdgeBuilder> resolveEdgeLocations() throws GraphBuilderException {
		List<DirectedEdgeBuilder> flatEdges = new LinkedList<>();
		for (DirectedEdgeBuilder edgeBuilder : edgeBuilders) {
			boolean sFlat = this.containsVertexFlat(edgeBuilder.getSource());
			boolean tFlat = this.containsVertexFlat(edgeBuilder.getTarget());

			if (sFlat && tFlat) {
				flatEdges.add(edgeBuilder);
			} else {
				DirectedViewableGraphBuilder targetsgb = null;
				DirectedViewableGraphBuilder sourcesgb = null;
				for (Iterator<DirectedViewableGraphBuilder> itSgb = subraphBuilders.iterator();
					itSgb.hasNext();)	 {
					DirectedViewableGraphBuilder sgb = itSgb.next();

					if (!sFlat && sgb.containsVertex(edgeBuilder.getSource())) 
						sourcesgb = sgb;

					if (!tFlat && sgb.containsVertex(edgeBuilder.getTarget())) 
						targetsgb = sgb;
					
					if ((sFlat || sourcesgb != null) && (tFlat || targetsgb != null)) {
						// Found subgraphs
						break;
					}
				}
				assert (sourcesgb != null || targetsgb != null); // else wise the edge would be flat
				if (sourcesgb == targetsgb) {
					// Edge fully contained in subgraph
					sourcesgb.edgeBuilders.add(edgeBuilder);
				} else if (sourcesgb == null) {
					if (edgesIntoGraphs.containsKey(targetsgb)) {
						edgesIntoGraphs.get(targetsgb).add(edgeBuilder);
					} else {
						edgesIntoGraphs.put(targetsgb, new LinkedList<>(Collections.singletonList(edgeBuilder)));
					}
				} else if (targetsgb == null) {
					if (edgesFromGraphs.containsKey(sourcesgb)) {
						edgesFromGraphs.get(sourcesgb).add(edgeBuilder);
					} else {
						edgesFromGraphs.put(sourcesgb, new LinkedList<>(Collections.singletonList(edgeBuilder)));
					}
				} else {
					// Edge from one subgraph into an other.
					if (edgesBetweenGraphsSource.containsKey(sourcesgb)) {
						edgesBetweenGraphsSource.get(sourcesgb).add(edgeBuilder);
					} else {
						edgesBetweenGraphsSource.put(sourcesgb, new LinkedList<>(Collections.singletonList(edgeBuilder)));
					}
					if (edgesBetweenGraphsTarget.containsKey(targetsgb)) {
						edgesBetweenGraphsTarget.get(targetsgb).add(edgeBuilder);
					} else {
						edgesBetweenGraphsTarget.put(targetsgb, new LinkedList<>(Collections.singletonList(edgeBuilder)));
					}

				}
			}
		}
		return flatEdges;
	}
	
	/**
	 * Builds the graph with the previously given informations.
	 * @return the finished graph
	 * @throws GraphBuilderException if the graph could not be build,
	 * 				because of inconsistencies or missing information.
	 */
	public DirectedViewableGraph build() throws GraphBuilderException {
		// Build vertices
		Set<DefaultVertex> vertices = vertexBuilders.stream().map(b -> b.build()).collect(Collectors.toSet());
		
		this.edgeBuilders = resolveEdgeLocations();

		// Build edges
		Set<DefaultDirectedEdge<DefaultVertex>> edges = new HashSet<>();
		for (DirectedEdgeBuilder eb : edgeBuilders) {
			edges.add(eb.build(vertices));
		}

		// Add alias vertex for each subgraph
		for (DirectedViewableGraphBuilder sb : subraphBuilders) {
			DirectedViewableGraph subgraph = sb.build();
			DefaultVertex v = new DefaultVertex(subgraph.getName(), subgraph.getName());
			v.setLink(new VertexReference(subgraph, null));
			subGraphs.add(subgraph);
			vertices.add(v);
			if (edgesFromGraphs.containsKey(sb)) {
				for (DirectedEdgeBuilder eb : edgesFromGraphs.get(sb)) {
					// Transform direct edge to edge to alias vertex
					DirectedEdgeBuilder aliasEb = new DirectedEdgeBuilder(v.getName(), eb.getTarget());
					for (Entry<String, String> keyValue : eb.getEdgeData().entrySet()) {
						aliasEb.addData(keyValue.getKey(), keyValue.getValue());
					}
					aliasEb.setID(eb.getId());
					edges.add(aliasEb.build(vertices));
				}
			}
			if (edgesIntoGraphs.containsKey(sb)) {
				for (DirectedEdgeBuilder eb : edgesIntoGraphs.get(sb)) {
					// Transform direct edge to edge to alias vertex
					DirectedEdgeBuilder aliasEb = new DirectedEdgeBuilder(eb.getSource(), v.getName());
					for (Entry<String, String> keyValue : eb.getEdgeData().entrySet()) {
						aliasEb.addData(keyValue.getKey(), keyValue.getValue());
					}
					aliasEb.setID(eb.getId());
					edges.add(aliasEb.build(vertices));
				}
			}
			if (edgesBetweenGraphsSource.containsKey(sb)) {
				for (DirectedEdgeBuilder eb : edgesBetweenGraphsSource.get(sb)) {
					eb.setEndpoints(v.getName(), eb.getTarget());
				}
			}
			if (edgesBetweenGraphsTarget.containsKey(sb)) {
				for (DirectedEdgeBuilder eb : edgesBetweenGraphsTarget.get(sb)) {
					eb.setEndpoints(eb.getSource(), v.getName());
				}
			}
		}
		// Build edges between subgraphs
		for (List<DirectedEdgeBuilder> ebList : edgesBetweenGraphsSource.values()) {
			for (DirectedEdgeBuilder eb : ebList) {
				edges.add(eb.build(vertices));
			}
		}

		try {
			graph = new DirectedViewableGraph(new HashSet<>(vertices), new HashSet<>(edges), name);
		} catch (IllegalArgumentException e) {
			throw new GraphBuilderException(BuilderType.GRAPH, "Graph-Constructor threw exception:" + e.getMessage());
		}
		return graph;
	}
	
	@Override
	public String toString() {
		return "GraphBuilder: " + this.name;
	}

}
