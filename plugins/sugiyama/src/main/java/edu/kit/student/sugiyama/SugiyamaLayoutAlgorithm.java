package edu.kit.student.sugiyama;

import edu.kit.student.graphmodel.DefaultVertex;
import edu.kit.student.graphmodel.DirectedSupplementEdgePath;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.graphmodel.directed.DefaultDirectedGraph;
import edu.kit.student.graphmodel.directed.DirectedEdge;
import edu.kit.student.graphmodel.directed.DirectedGraph;
import edu.kit.student.parameter.Parameter;
import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.LayoutAlgorithm;
import edu.kit.student.sugiyama.graph.SugiyamaGraph;
import edu.kit.student.sugiyama.steps.CrossMinimizer;
import edu.kit.student.sugiyama.steps.CycleRemover;
import edu.kit.student.sugiyama.steps.EdgeDrawer;
import edu.kit.student.sugiyama.steps.ICrossMinimizer;
import edu.kit.student.sugiyama.steps.ICycleRemover;
import edu.kit.student.sugiyama.steps.IEdgeDrawer;
import edu.kit.student.sugiyama.steps.ILayerAssigner;
import edu.kit.student.sugiyama.steps.IVertexPositioner;
import edu.kit.student.sugiyama.steps.LayerAssigner;
import edu.kit.student.sugiyama.steps.VertexPositioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * This class supports a customizable implementation of the Sugiyama-framework.
 * The single stages of the framework can be chosen individually.
 * Additionally this class tries to follow the given constraints, if possible.
 * @param <G> 
 */
public class SugiyamaLayoutAlgorithm<G extends DirectedGraph> 
	implements LayoutAlgorithm<G> {
	private ICycleRemover remover;
	private ILayerAssigner assigner;
	private ICrossMinimizer minimizer;
	private IVertexPositioner positioner;
	private IEdgeDrawer drawer;
	private Set<RelativeLayerConstraint> relativeLayerConstraints;
	private Set<AbsoluteLayerConstraint> absoluteLayerConstraints;
	private Settings settings;
	private SugiyamaGraph sugyGraph;
	
    final Logger logger = LoggerFactory.getLogger(SugiyamaLayoutAlgorithm.class);

	/**
	 * Creates a SugiyamaLayoutAlgorithm
	 */
	public SugiyamaLayoutAlgorithm() {
		relativeLayerConstraints = new HashSet<>();
		absoluteLayerConstraints = new HashSet<>();
		this.remover = new CycleRemover();
		this.assigner = new LayerAssigner();
		this.minimizer = new CrossMinimizer();
		this.positioner = new VertexPositioner();
		this.drawer = new EdgeDrawer();
		this.sugyGraph = null;
	}
	//TODO check if it would be wise to set the first steps in the constructor in order to avoid nullpointer exceptions
	//they can still be replaced in the setter
	

	/**
	 * Sets the algorithm to remove all cycles for a graph to layout used when applying the layout
	 * @param remover the algorithm
	 */
	public void setCycleRemover(ICycleRemover remover) {
		this.remover = remover;
	}
	
	/**
	 * Sets the algorithm for layer assigning used when applying the layout
	 * @param assigner the layer assign algorithm
	 */
	public void setLayerAssigner(ILayerAssigner assigner) {
		this.assigner = assigner;
	}
	
	/**
	 * Sets the algorithm for cross minimization used when applying the layout
	 * @param minimizer the cross minimization algorithm
	 */
	public void setCrossMinimizer(ICrossMinimizer minimizer) {
		this.minimizer = minimizer;
	}
	
	/**
	 * Sets the algorithm for vertex positioning used when applying the layout
	 * @param positioner the positioning algorithm
	 */
	public void setVertexPositioner(IVertexPositioner positioner) {
		this.positioner = positioner;
	}
	
	/**
	 * Sets the algorithm for edge drawing used when applying the layout
	 * @param drawer the edge drawing algorithm
	 */
	public void setEdgeDrawer(IEdgeDrawer drawer) {
		this.drawer = drawer;
	}
	
	/**
	 * Adds an {@link AbsoluteLayerConstraint} to the set of constraints which should be followed.
	 * @param constraint the constraint to follow
	 */
	public void addAbsoluteLayerConstraint(AbsoluteLayerConstraint constraint) {
		absoluteLayerConstraints.add(constraint);
	}
	
	/**
	 * Adds an {@link RelativeLayerConstraint} to the set of constraints which should be followed.
	 * @param constraint the constraint to follow
	 */
	public void addRelativeLayerConstraint(RelativeLayerConstraint constraint) {
		relativeLayerConstraints.add(constraint);
	}

	@Override
	public Settings getSettings() {
		if (this.settings != null) {
			return this.settings;
		}
		LinkedList<Parameter<?,?>> parameter = new LinkedList<>();

		settings = new Settings("Sugiyama-Layout", parameter);
		settings.addSubSetting(minimizer.getSettings());
		return settings;
	}

    @Override
    public void layout(DirectedGraph graph) {
		SugiyamaGraph wrappedGraph = new SugiyamaGraph(graph);
		long timeBefore = (new Date()).getTime();
        remover.removeCycles(wrappedGraph);
		assigner.assignLayers(wrappedGraph);
		minimizer.minimizeCrossings(wrappedGraph);
		positioner.positionVertices(wrappedGraph);
		drawer.drawEdges(wrappedGraph);
		this.sugyGraph = wrappedGraph;
		logger.info("runs in " + ((new Date()).getTime() - timeBefore) + "ms");
    }

	public void layout(DefaultDirectedGraph<DefaultVertex, DirectedEdge> graph) {
		SugiyamaGraph wrappedGraph = new SugiyamaGraph(graph);

		logger.info("removing edges");
		remover.removeCycles(wrappedGraph);
		logger.info("assigning layers");
		assigner.assignLayers(wrappedGraph);
		logger.info("minimizing crossings");
		minimizer.minimizeCrossings(wrappedGraph);
		logger.info("positioning vertices");
		positioner.positionVertices(wrappedGraph);
		logger.info("drawing edges");
		drawer.drawEdges(wrappedGraph);
		this.sugyGraph = wrappedGraph;
	}


	/**
	 * Draws given edges new, also take paths into account.
	 * Positions of every vertex is set and should not be changed.
	 * Also assignees every vertex a layer, as well as every vertex in the given paths.
	 *
	 * @param vertices given vertices with set coordinates
	 * @param edges edges that connect vertices of adjacent layers
	 * @param paths paths describing edges connecting vertices of two not adjacent layers
	 */
	public void drawEdgesNew(Set<Vertex> vertices, Set<DirectedEdge> edges, Set<DirectedSupplementEdgePath> paths){
		SugiyamaGraph sugyGraph = new SugiyamaGraph(vertices, edges, paths);
		drawer.drawEdges(sugyGraph);	//draw edges
		this.sugyGraph = sugyGraph;
	}

	/**
	 * Exports the state of the last layouted {@link SugiyamaGraph} as a {@link LayoutedGraph}.
	 *
	 * @return the LayoutedGraph that represents the state of a SugiyamaGraph
	 * 		or null, if no layout has been applied yet
	 */
	public LayoutedGraph exportLayoutedGraph(){
		SugiyamaGraph sg = this.sugyGraph;
		if(sg == null)
			return null;
		return new LayoutedGraph(sg.exportVertices(), sg.exportEdges(), sg.exportPaths());
	}
}
