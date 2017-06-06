package edu.kit.student.joana.callgraph;

import edu.kit.student.graphmodel.CompoundVertex;
import edu.kit.student.graphmodel.Edge;
import edu.kit.student.graphmodel.InlineSubGraph;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.graphmodel.action.EdgeAction;
import edu.kit.student.graphmodel.action.SubGraphAction;
import edu.kit.student.graphmodel.action.VertexAction;
import edu.kit.student.graphmodel.directed.DefaultDirectedGraph;
import edu.kit.student.joana.CallGraphVertex;
import edu.kit.student.joana.JoanaCollapsedVertex;
import edu.kit.student.joana.JoanaCollapser;
import edu.kit.student.joana.JoanaEdge;
import edu.kit.student.joana.JoanaGraph;
import edu.kit.student.joana.JoanaPlugin;
import edu.kit.student.joana.JoanaVertex;
import edu.kit.student.joana.graphmodel.DirectedOnionPath;
import edu.kit.student.joana.graphmodel.JoanaCompoundVertex;
import edu.kit.student.joana.methodgraph.MethodGraph;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.plugin.LayoutOption;
import edu.kit.student.util.LanguageManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a specified graph representation for the Callgraph in Joana.
 */
public class CallGraph extends JoanaGraph {

    private List<MethodGraph> methodGraphs = new LinkedList<>();
    private DefaultDirectedGraph<JoanaVertex, JoanaEdge> graph;

    private Map<JoanaCollapsedVertex, VertexAction> expandActions;
    private JoanaCollapser collapser;
    
    public CallGraph(String name, Set<CallGraphVertex> vertices, Set<JoanaEdge> edges) {
        super(name, new HashSet<>(vertices), edges);
        this.graph = new DefaultDirectedGraph<>(new HashSet<JoanaVertex>(vertices), edges);
        for (CallGraphVertex vertex : vertices) {
            methodGraphs.add(vertex.getGraph());
        }

        Map<JoanaEdge, DirectedOnionPath<JoanaEdge, JoanaCompoundVertex>> onionEdges = new HashMap<>();
        collapser = new JoanaCollapser(graph, onionEdges);
        this.expandActions = new HashMap<>();
        this.applyDefaultFilters();
    }

    public CallGraph(String name) {
        this(name, new HashSet<>(), new HashSet<>());
    }

    public List<MethodGraph> getMethodGraphs() {
    	return methodGraphs;
    }
    @Override
    public List<VertexAction> getVertexActions(Vertex vertex) {
        List<VertexAction> actions = new LinkedList<>();
        if (this.collapser.getCollapsedVertices().contains(vertex) && this.getVertexSet().contains(vertex)) {
            actions.add(expandActions.get(vertex));
        }
        return actions;
    }

    @Override
    public List<SubGraphAction> getSubGraphActions(Set<ViewableVertex> vertices) {
        List<SubGraphAction> actions = new LinkedList<>();
        if (getVertexSet().containsAll(vertices) && 
        		vertices.size() > 1) {
            actions.add(newCollapseAction(vertices));
        }

        return actions;
    }

    @Override
    public List<EdgeAction> getEdgeActions(Edge edge) {
        return new LinkedList<>();
    }

    @Override
    public Set<? extends InlineSubGraph> getInlineSubGraphs() {
        return new HashSet<>();
    }

	private VertexAction newExpandAction(CompoundVertex vertex) {
		//TODO: add translation to string if needed in the future
	    return new VertexAction(LanguageManager.getInstance().get("ctx_expand"), 
	            "Adds all vertices contained in this Summary-Vertex to the graph and removes the Summary-Vertex.") {
            
            @Override
            public void handle() {
                expand(vertex);
            }
        };
	}
	
	private SubGraphAction newCollapseAction(Set<ViewableVertex> vertices) {
		//TODO: add translation to string if needed in the future
	    return new SubGraphAction(LanguageManager.getInstance().get("ctx_collapse"), 
	    		"Collapses all vertices into one Summary-Vertex.") {
            
            @Override
            public void handle() {
                collapse(vertices);
            }
        };
	}
	public JoanaCollapsedVertex collapse(Set<ViewableVertex> subset) {
        Set<JoanaVertex> directedSubset = new HashSet<>();
	    for (Vertex v : subset) {
	        if (!graph.contains(v)) {
                throw new IllegalArgumentException("Cannot collapse vertices, not contained in this graph.");
	        } else {
	            directedSubset.add(graph.getVertexById(v.getID()));
	        }
	    }
	    JoanaCollapsedVertex collapsed = collapser.collapse(directedSubset);
		expandActions.put(collapsed, newExpandAction(collapsed));
		return collapsed;
    }
	
    private Set<JoanaVertex> expand(CompoundVertex vertex) {
        return collapser.expand(vertex);
    }

    @Override
    public List<LayoutOption> getRegisteredLayouts() {

        // Retrieve callgraphLayouts from register
        List<CallGraphLayoutOption> callGraphLayouts = new LinkedList<>();
        if (JoanaPlugin.getCallGraphLayoutRegister().getLayoutOptions() != null) {
            callGraphLayouts.addAll(JoanaPlugin.getCallGraphLayoutRegister().getLayoutOptions());
        }
        for (CallGraphLayoutOption option : callGraphLayouts) {
            option.setGraph(this);
        }

        // Add default directed graph layouts;
        List<LayoutOption> layoutOptions = new LinkedList<>(callGraphLayouts);
        layoutOptions.addAll(super.getRegisteredLayouts());

        return layoutOptions;
    }
    
    @Override
	public LayoutOption getDefaultLayout() {
		return new CallGraphLayoutOption() {
			{
                this.setName("Call-Graph-Layout");
                this.setId("CGL");
                this.setGraph(CallGraph.this);
            }
            
            @Override
            public void chooseLayout() {
                this.setLayout(new CallGraphLayout());
            }
		};
	}

    @Override
    public List<GAnsProperty<?>> getStatistics() {
        return super.getStatistics();
    }

    @Override
    public Integer outdegreeOf(Vertex vertex) {
        return removeFilteredEdges(graph.outgoingEdgesOf(vertex)).size();
    }

    @Override
    public Integer indegreeOf(Vertex vertex) {
        return removeFilteredEdges(graph.incomingEdgesOf(vertex)).size();
    }

    @Override
    public Integer selfLoopNumberOf(Vertex vertex) {
        return graph.selfLoopNumberOf(vertex);
    }

    @Override
    public Set<JoanaEdge> outgoingEdgesOf(Vertex vertex) {
        return removeFilteredEdges(graph.outgoingEdgesOf(vertex));
    }

    @Override
    public Set<JoanaEdge> incomingEdgesOf(Vertex vertex) {
        return removeFilteredEdges(graph.incomingEdgesOf(vertex));
    }

    @Override
    public Set<JoanaEdge> selfLoopsOf(Vertex vertex) {
        return graph.selfLoopsOf(vertex);
    }

    @Override
    public Set<JoanaVertex> getVertexSet() {
        return removeFilteredVertices(graph.getVertexSet());
    }

    @Override
    public Set<JoanaEdge> getEdgeSet() {
        return removeFilteredEdges(graph.getEdgeSet());
    }

    @Override
    public Set<JoanaEdge> edgesOf(Vertex vertex) {
        return removeFilteredEdges(graph.edgesOf(vertex));
    }
}
