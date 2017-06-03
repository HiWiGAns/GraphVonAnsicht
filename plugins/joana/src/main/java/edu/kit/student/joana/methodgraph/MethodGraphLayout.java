package edu.kit.student.joana.methodgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.student.graphmodel.DefaultVertex;
import edu.kit.student.joana.InterproceduralEdge;
import edu.kit.student.plugin.EdgeFilter;
import edu.kit.student.plugin.VertexFilter;
import edu.kit.student.sugiyama.LayoutedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.student.graphmodel.DirectedSupplementEdgePath;
import edu.kit.student.graphmodel.EdgePath;
import edu.kit.student.graphmodel.FastGraphAccessor;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.graphmodel.directed.DefaultDirectedEdge;
import edu.kit.student.graphmodel.directed.DefaultDirectedSupplementEdgePath;
import edu.kit.student.graphmodel.directed.DirectedEdge;
import edu.kit.student.joana.FieldAccess;
import edu.kit.student.joana.FieldAccessGraph;
import edu.kit.student.joana.JoanaEdge;
import edu.kit.student.joana.JoanaVertex;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.LayoutAlgorithm;
import edu.kit.student.sugiyama.AbsoluteLayerConstraint;
import edu.kit.student.sugiyama.LayerContainsOnlyConstraint;
import edu.kit.student.sugiyama.RelativeLayerConstraint;
import edu.kit.student.sugiyama.SugiyamaLayoutAlgorithm;
import edu.kit.student.sugiyama.steps.LayerAssigner;
import edu.kit.student.util.DoublePoint;
import edu.kit.student.util.IntegerPoint;
import javafx.scene.paint.Color;


/**
 * Implements hierarchical layout with layers for {@link MethodGraph}.
 * This graph contains field access subgraphs.
 */
public class MethodGraphLayout implements LayoutAlgorithm<MethodGraph> {
	
    private final Logger logger = LoggerFactory.getLogger(MethodGraphLayout.class);
	private SugiyamaLayoutAlgorithm<MethodGraph> sugiyamaLayoutAlgorithm;
	private Integer dummyId = -1;
	
	public MethodGraphLayout() {
		this.sugiyamaLayoutAlgorithm = new SugiyamaLayoutAlgorithm<>();
	}

	
	@Override
	public Settings getSettings() {
		return sugiyamaLayoutAlgorithm.getSettings();
	}

	/**
	 * Layouts a single {@link MethodGraph} with the configured settings.
	 * 
	 * @param graph The {@link MethodGraph} to layout.
	 */
	public void layout(MethodGraph graph) {
		logger.info("Graph before: Vertices: "+graph.getVertexSet().size()+", Edges: "+graph.getEdgeSet().size());
		graph.invalidateProperties(); //invalidates all saved properties of the methodGraph's data structures that have to be reset before relayouting
		this.layoutFieldAccessGraphs(graph);
		List<FieldAccess> collapsedFAs = graph.collapseFieldAccesses();
		
		//remove interprocedural edges and their dummies from this graph
		graph.removeInterproceduralEdges();

	    
	  	//create absoluteLayerConstraints
        Set<AbsoluteLayerConstraint> absoluteLayerConstraints = new HashSet<>();
		Set<LayerContainsOnlyConstraint> layerContainsOnlyConstraints = new HashSet<>();
        
        //create absoluteLayerConstraint for Entry vertex
        if (graph.getVertexSet().contains(graph.getEntryVertex())) {
            Set<Vertex> firstLayer = new HashSet<>();
            firstLayer.add(graph.getEntryVertex());
            absoluteLayerConstraints.add(new AbsoluteLayerConstraint(firstLayer, 0));
            layerContainsOnlyConstraints.add(new LayerContainsOnlyConstraint(firstLayer, 0));
        }
        
        //create absoluteLayerConstraint for Method Parameters
        Set<Vertex> secondLayer = new HashSet<>();
        
        //create relativeLayerConstraints
        Set<RelativeLayerConstraint> relativeLayerConstraints = new HashSet<>();
        for (JoanaVertex v : graph.getVertexSet()) {
            //check if call node
            if (v.getNodeKind().equals(JoanaVertex.VertexKind.CALL)) {
                Set<Vertex> bottom = this.getParamVerticesOfCall(v, graph);
                Set<Vertex> top = new HashSet<>();
                top.add(v);
                
                relativeLayerConstraints.add(new RelativeLayerConstraint(top, bottom, true, 1));
            } else if (v.getNodeKind().equals(JoanaVertex.VertexKind.FRMI)
                    || v.getNodeKind().equals(JoanaVertex.VertexKind.FRMO)) {
                secondLayer.add(v);
            }
        }
        
        absoluteLayerConstraints.add(new AbsoluteLayerConstraint(secondLayer, 1));
		layerContainsOnlyConstraints.add(new LayerContainsOnlyConstraint(secondLayer, 1));
        
        LayerAssigner assigner = new LayerAssigner();
        assigner.addRelativeConstraints(relativeLayerConstraints);
        assigner.addAbsoluteConstraints(absoluteLayerConstraints);
		assigner.addLayerContainsOnlyConstraints(layerContainsOnlyConstraints);
	    
        sugiyamaLayoutAlgorithm.setLayerAssigner(assigner);
		sugiyamaLayoutAlgorithm.layout(graph);
		
		logger.info("Graph after: Vertices: "+graph.getVertexSet().size()+", Edges: "+graph.getEdgeSet().size());
		expandFieldAccesses(graph, collapsedFAs);
		collapsedFAs.forEach(this::checkIntegrity); //makes more sense here because expandFAs sets the coordinates of previous layouted FA-vertices, and -edges adequately

		collapsedFAs.forEach(fa->drawEdgesNew(graph, fa));	//new version
		
		//draws interprocedural vertices of this graph. 
		drawInterproceduralEdges(graph);
	}

	/**
	 * Layouts the given graph without any filter active.
	 * Then saves the graph's vertices and edges in a {@link LayoutedGraph} in the given {@link MethodGraph}.
	 *
	 * @param graph graph to layout and save
	 */
	public void layoutAndSaveWholeGraph(MethodGraph graph){
		//save active vertex- and edgefilter
		List<VertexFilter> vertexFilters = new LinkedList<>(graph.getActiveVertexFilter());
		List<EdgeFilter> edgeFilters = new LinkedList<>(graph.getActiveEdgeFilter());

		//clear active vertex- and edgefilter
		graph.setVertexFilter(new LinkedList<>());
		graph.setEdgeFilter(new LinkedList<>());

		//layout given methodgraph without filters
		this.layout(graph);

		//set the graphs active vertex- and edgefilter again
		graph.setVertexFilter(vertexFilters);
		graph.setEdgeFilter(edgeFilters);

		//export the state of the graph layouted
		LayoutedGraph lg = this.sugiyamaLayoutAlgorithm.exportLayoutedGraph();

		//save the exported graph in the given methodgraph
		graph.setLayoutedGraph(lg);

	}

	/**
	 * Redraws the edges of the given {@link MethodGraph}.
	 * Therefore uses the {@link LayoutedGraph} of the given graph.
	 *
	 *
	 * @param graph graph to relayout its edges
	 */
	public void relayoutEdges(MethodGraph graph){
		LayoutedGraph lg = graph.getLayoutedGraph();
		if(lg == null)
			return;
		Set<Vertex> vertices = lg.getVertices();
		Set<DirectedEdge> edges = lg.getEdges();
		Set<DirectedSupplementEdgePath> paths = lg.getPaths();

		//remove interprocedural edges from edgeset and their dummies from vertex set
		//(fieldAccesses should still be collapsed ???) so filter in the fieldAccesses and relayout the edges in there!

		//call drawEdgesNew with vertices, edges, paths

		//draw the not filtered ie's new,(let the dummies even if the edges are filtered!?!)TODO: ALWAYS let the dummies of an interprocedural edge even the edge itself is filtered!!!
	}
	
	/**
	 * Layouts every single FieldAccessGraph.
	 * Also sets the sizes of the vertex representing this FieldAccessGraph appropriate.
	 */
	private void layoutFieldAccessGraphs(MethodGraph graph){
	    SugiyamaLayoutAlgorithm<FieldAccessGraph> fieldAccessSugAlgo = new SugiyamaLayoutAlgorithm<>();
		for(FieldAccess fa : graph.getFieldAccesses()){
			FieldAccessGraph fag = fa.getGraph();
			fieldAccessSugAlgo.layout(fag);
		}
	}

	//checks the set coordinates of the vertices in the FA, especially checks if no vertex goes out of the FA
	private void checkIntegrity(FieldAccess fa){
		FieldAccessGraph fag = fa.getGraph();
		//assert(dEquals(fa.getX(), fag.getX())); //as the FieldAccess itself is layouted later this is normally not necessary here
		//assert(fa.getSize().equals(fag.getSize()));  fag calculates size new, might be not optional
		double faStartX = fa.getX();
		double faEndX = fa.getX() + fa.getSize().x;
		double faStartY = fa.getY();
		double faEndY = fa.getY() + fa.getSize().y;
		double fagStartX = fag.getX();
		double fagEndX = fag.getX() + fa.getSize().x;
		double fagStartY = fag.getY();
		double fagEndY = fag.getY() + fa.getSize().y;

		//System.out.println("FA-Points: X("+faStartX + "|" + faEndX + "), Y(" + faStartY + "|" + faEndY + ")");
		//System.out.println("FAG-Points: X("+fagStartX + "|" + fagEndX + "), Y(" + fagStartY + "|" + fagEndY + ")");
		for(JoanaVertex v : fa.getGraph().getVertexSet()){
			//System.out.println("Vertex-X(" + v.getX() + "|" + (v.getX() + v.getSize().x) + ")");
			assert(v.getX() > fagStartX && (v.getX() + v.getSize().x) < fagEndX);
			//System.out.println("Vertex-Y(" + v.getY() + "|" + (v.getY() + v.getSize().y) + ")");
			assert(v.getY() > fagStartY && (v.getY() + v.getSize().y) < fagEndY);
		}
	}
	
	
	//This method initializes everything necessary for drawing every edge in an FieldAccess new.
	//also sets the necessary dummies, supplementEdges, and paths in every layer.
	//For setting dummies: calc how much dummies per layer, position of vertices in this layer then add dummies, and random assign the dummies layers
	//maybe go over every edge that skips layer and search in every layer for a free dummy for assigning!
	/**
	 * Draws all edges contained in a FieldAccess and coming into and going out of a FieldAccess new.
	 * The coordinates of vertices stay the same.
	 */
	private void drawEdgesNew(MethodGraph graph, FieldAccess fa){
		//maybe split more functionality into private methods
		double boxYtop = fa.getY();
		double boxYbottom = fa.getY() + fa.getSize().y;
		System.out.println("fa-graph edges: " + fa.getGraph().getEdgeSet().size());
		//System.out.println("fa-graph vertices: " + fa.getGraph().getVertexSet().size());
		List<JoanaVertex> faVertices = new ArrayList<>(graph.removeFilteredVertices(fa.getGraph().getVertexSet()));
		List<JoanaEdge> faEdges = new ArrayList<>(graph.removeFilteredEdges(fa.getGraph().getEdgeSet()));
		System.out.println("faEdges: " + faEdges.size());
		//System.out.println("faVertices: " + faVertices.size());
		Set<DirectedEdge> newFAedges = new HashSet<>();	//new edges from outside to the next layer, not describing a path
		faVertices.sort(Comparator.comparingDouble(DefaultVertex::getY));	//sort vertices in ascending order of y values
		Map<Integer, List<Vertex>> layerNumToVertices = new HashMap<>();
		Map<Integer, Vertex> edgeToDummy = new HashMap<>();
		Set<JoanaEdge> fromOutEdges = graph.removeFilteredEdges(getEdgesFromOutside(graph, fa));	//edges from out in representing vertex
		List<Double> layerYvals = new ArrayList<>();
		//add y vals for layers
		layerYvals.add(boxYtop);
		for(Vertex v : faVertices){
			if(getIndex(layerYvals, v.getY()) == -1){
				layerYvals.add(v.getY());
			}
		}
		layerYvals.add(boxYbottom);
		layerYvals.sort(Double::compare);
		for(int i = 0; i < layerYvals.size(); i++){	//fills mapping for each layer
			layerNumToVertices.put(i, new ArrayList<>());
		}
		for(JoanaVertex v : faVertices){	//add field access vertices to mapping of layers
			int index = getIndex(layerYvals, v.getY());
			assert(index != -1);
			layerNumToVertices.get(index).add(v);
		}
		//now add for every layer enough dummies and sort the lists afterwards
		for(JoanaEdge e : faEdges){	//add dummies for normal edges in this field access
			double start = Math.min(e.getSource().getY(), e.getTarget().getY());
			double end = Math.max(e.getSource().getY(), e.getTarget().getY());
			int index1 = getIndex(layerYvals, start);
			int index2 = getIndex(layerYvals, end);
			
			assert(index1 != -1 && index2 != -1);	//start and end have to be found in this list!!!
			for(int i = index1 + 1; i < index2; i++){
				double layerVertexHeight = layerNumToVertices.get(i).get(0).getSize().y;//height from first vertex in layer. Asserting the height is the same from all vertices in this layer!
				layerNumToVertices.get(i).add(new JoanaDummyVertex("","", getDummyID(), new DoublePoint(2, layerVertexHeight)));
			}
		}
		//System.out.println("FA-Points: X("+fa.getX() + "|" + (fa.getX() + fa.getSize().x) + "), Y(" + fa.getY() + "|" + (fa.getY() + fa.getSize().y) + ")");
		for(JoanaEdge e : fromOutEdges){	//add dummy vertices for vertices from outside on top and bottom layer
			List<DoublePoint> points = e.getPath().getNodes();
			//System.out.println("First point's y:" + points.get(0).y);
			//System.out.println("Last point's y:" + points.get(points.size() - 1).y);
			DoublePoint borderDummySize = new DoublePoint(2, 5);//just a simple size for dummies on first or last layer
			//cases: top of box and bottom, in and out of the box.
			double start = -1;
			double end = -1;
			DoublePoint boxCross = null;
			int layerToAddDummy = 0;
			JoanaDummyVertex tempDummy;
			double yAddition = 0.0;	//adds this value to the new set y-coordinate of the new dummy(an other value than 0 is just needed on bottom of FA-box)
			//vertex boxes are not accurate so that the entry point in box is the same point as drawn by EdgeDrawer
			//in addition new dummy position is left on top, move them all half its length to the left!
			//on bottom: move them also it's length up!
			if(dEquals(points.get(points.size() - 1).y, boxYtop)){ //edge into box from top
				start = boxYtop;
				end = e.getTarget().getY();
				boxCross = points.get(points.size() - 1);
				layerToAddDummy = 0;	//add to first layer
			}else if(dEquals(points.get(0).y, boxYtop)){//edge out of box from top
				start = boxYtop;
				end = e.getSource().getY();
				boxCross = points.get(0);
				layerToAddDummy = 0;	//add to first layer
			}else if(dEquals(points.get(0).y, boxYbottom)){//edge out of box from bottom
				start = e.getSource().getY();
				end = boxYbottom;
				boxCross = points.get(0);
				layerToAddDummy = layerYvals.size() - 1;	//add to last layer
				yAddition = - borderDummySize.y;
			}else if(dEquals(points.get(points.size() - 1).y, boxYbottom)){//edge into box from bottom
				start = e.getTarget().getY();
				end = boxYbottom;
				boxCross = points.get(points.size() - 1);
				layerToAddDummy = layerYvals.size() - 1;	//add to last layer
				yAddition = - borderDummySize.y;
			}else{
				assert(false);
			}
			tempDummy = new JoanaDummyVertex("", "", getDummyID(), borderDummySize);
			tempDummy.setX((boxCross.x - borderDummySize.x / 2));
			tempDummy.setY(boxCross.y + yAddition);
			layerNumToVertices.get(layerToAddDummy).add(tempDummy);	//adds new dummy vertex on correct position 
			//add mapping of edge id to dummy that represents the new source or target on top or bottom of box!!!!!!
			edgeToDummy.put(e.getID(), tempDummy);
			//now add dummies on layers where layers are skipped by edges!
			int index1 = getIndex(layerYvals, start);
			int index2 = getIndex(layerYvals, end);
			assert(index1 != -1 && index2 != -1);	//start and end have to be found in this list!!!
			for(int i = index1 + 1; i < index2; i++){
				double layerVertexSize = layerNumToVertices.get(i).get(0).getSize().y;//every vertex in the FA got a layer,edges are not from outside!
				layerNumToVertices.get(i).add(new JoanaDummyVertex("","", getDummyID(), new DoublePoint(2, layerVertexSize)));
			}
		}
		layerNumToVertices.values().forEach(l->l.sort(Comparator.comparingDouble(Vertex::getX)));//sort all layers
		
		//now set the coordinates of dummies from layer 1...(max-1)
//		System.out.println("now assigning coordinates to dummies!");
		for(int i = 1; i < layerYvals.size() - 1; i++){	//just need to add coordinates to vertices of layer 1 to (max-1));
			assignCoordinatesToDummiesOnLayer(fa, layerNumToVertices.get(i));//should work so, coordinates are written into the vertices
		}
		//Test for printing all layers
//		for(int i = 0; i < layerNumToVertices.size(); i++){
//			System.out.println("layer:"+i);
//			layerNumToVertices.get(i).forEach(v->System.out.print(v.getID()+","));
//			System.out.print('\n');
//		}
		
		//now build paths, in the correct direction of the edge. edges will be turned later, if necessary
		Set<DirectedSupplementEdgePath> paths = getRandomPaths(graph,fa, newFAedges, fromOutEdges, layerNumToVertices, edgeToDummy, layerYvals);
		
		//now draw edges and adjust their edgepaths
		drawAndAdjustFieldAccessEdges(graph,fa, newFAedges , paths, fromOutEdges, edgeToDummy);
	}
	
	//assigns every vertex with id <0(dummy vertex) in the given list that represents vertices in this layer a x- and y-coordinate
	/**
	 * Assigns every dummy in the given layer a coordinate so that they don't collide with other dummies or normal vertices.
	 * Dummies are identified by a negative id.
	 * Dummies on the first and last layer should'nt be assigned coordinates because their coordinates are set through the points an edge is coming in or 
	 * going out of a FieldAccess
	 * 
	 * @param layer all vertices contained in this layer. Contains at least one normal vertex(id >= 0) and any amount of dummies(id < 0)
	 * @param fa the FieldAccess. Necessary because of its x- and y-coordinates
	 */
	private void assignCoordinatesToDummiesOnLayer(FieldAccess fa, List<Vertex> layer){
		//watch out that the boxes are placed correctly (coordinates are left on top!!!)
        //calc positions of dummies with a bit less distance between each other
		List<Vertex> dummies = layer.stream().filter(v->v.getID() < 0).collect(Collectors.toList());
		if(dummies.isEmpty()) return;
		int dummyCount = dummies.size();
		//System.out.println("dummies in actual layer: " + dummyCount);
		DoublePoint dummySize = dummies.get(0).getSize(); //dummy size is the same for all dummies without set x- and y-coordinates!
		List<Vertex> normalVertices = layer.stream().filter(v->v.getID() >= 0).collect(Collectors.toList());
		normalVertices.sort(Comparator.comparingDouble(Vertex::getX));
		assert(!normalVertices.isEmpty()); //each layer should have at least one normal vertex!
		List<Double> distancePoints = new ArrayList<>(2 * normalVertices.size() + 2); //points describing free space for positioning dummies. space between points with indices (2n) and (2n+1) is free, space between (2n+1) and (2n) occupied!
		distancePoints.add(fa.getX() + 1);
		for(Vertex v : normalVertices){
			distancePoints.add(v.getX() - 1);
			distancePoints.add(v.getX() + v.getSize().x + 1);
		}
		distancePoints.add(fa.getX() + fa.getSize().x - 1);
        //calculate for every space between two points how much dummies there should be (dependent on size of the space, use percentages of space size from whole free space size)
		double freeSpace = 0.0; //amount of total free space horizontally in this layer
		double[] freeSegmentsSpace = new double[distancePoints.size() / 2];
		int[] amountDummiesPerSegment = new int[distancePoints.size() / 2];
		for(int i = 0; i < distancePoints.size() / 2; i++){
			freeSegmentsSpace[i] = distancePoints.get(2 * i + 1) - distancePoints.get(2 * i);
			//System.out.println("distancePoints: "+distancePoints.get(2*i+1) + "|" + distancePoints.get(2*i));
			assert(freeSegmentsSpace[i] > 0);
			freeSpace += freeSegmentsSpace[i];//count the width of total available space in this layer for positioning dummies
		}
		int assignedDummies = 0;
		for(int i = 0; i < freeSegmentsSpace.length - 1; i++){
		    amountDummiesPerSegment[i] = Math.toIntExact(Math.round((freeSegmentsSpace[i] / freeSpace) * dummyCount));
		    assert(amountDummiesPerSegment[i] >= 0);
		    assert(amountDummiesPerSegment[i] <= dummyCount);
		    assignedDummies += amountDummiesPerSegment[i];
        }
        amountDummiesPerSegment[amountDummiesPerSegment.length - 1] = dummyCount - assignedDummies; //to be sure there was no dummy lost through rounding
		assert(amountDummiesPerSegment[amountDummiesPerSegment.length - 1] >= 0);
		//for(int i = 0; i < distancePoints.size()/2; i++){
		//	System.out.println("amountDummiesPerSegment " + i + ": " + amountDummiesPerSegment[i]);
		//}
        //calculate for every free space distances between dummies and thus there positions! (maybe set dummies x-size to 1)
        double yVal = normalVertices.get(0).getY(); //y-coord from all normal vertices on the layer should be the same. Also used for setting dummies' y-coordinate
        int dummyIdx = 0; //index for access in dummy list
        for(int i = 0; i < distancePoints.size() / 2; i++){
            double xVal = distancePoints.get(2 * i); //most left x-value of the free space segment
            double newFreeSpace = freeSegmentsSpace[i] - amountDummiesPerSegment[i] * dummySize.x;
            double distBetweenDummies = newFreeSpace / (amountDummiesPerSegment[i] + 1.0);
            for(int j = 1; j <= amountDummiesPerSegment[i]; j++){
                xVal += distBetweenDummies;
                Vertex dummy = dummies.get(dummyIdx);
                dummy.setX(xVal);
                dummy.setY(yVal);
                xVal += dummy.getSize().x;
                dummyIdx++;
            }
        }
        assert(dummyIdx == dummies.size()); //because dummyIdx is increased after the last dummy was processed

		//finally sort the layer again according to their x-values
		layer.sort(Comparator.comparingDouble(Vertex::getX));
//		System.out.println("layer: ");
//		layer.forEach(v->System.out.print(("["+v.getID()+"]("+v.getX()+","+v.getY()+")size:("+v.getSize().x+ ","+v.getSize().y +"); ")));
//		System.out.print('\n');
		//TODO: assertion might be not possible in FA's with many edges. The size of a FA should be set according to the number of edges in it!
/*		for(int i = 0; i < layer.size() - 1;i++){
			Vertex first = layer.get(i);
			Vertex second = layer.get(i + 1);
			System.out.println("first: "+first.getID()+", second: "+second.getID());
			assert(first.getX() + first.getSize().x < second.getX());
		}*/
	}
	
	/**
	 * Builds Paths for every edge in the FieldAccess and edges coming in and going out of a FieldAccess if they skip at least one layer. If so, the edge
	 * is added to a path, dummies are added for every layer the edge skips and SupplementEdges are built that connect the source with the first dummy,
	 * the dummies with each other and the last dummy with the target vertex.
	 * Dummies on each layer are assigned randomly but every dummy just once.
	 * 
	 * 
	 * @param fa the FieldAccess
	 * @param newFAedges an empty set to add edges that replaces an edge coming into or going out of the FieldAccess, because source and target
	 * 		have to be completely in a FieldAccess and mustn't be a vertex out of the FieldAccess
	 * @param fromOutEdges edges coming from out or going into the FieldAccess
	 * @param layerNumToVertices mapping of layer number to vertices contained in this layer
	 * @param edgeToDummy mapping of edge id to the dummy that replaces the vertex of this edge being outside of the FieldAccess
	 * @param layerYvals y-values of all layers
	 * @return a set of all SupplementPath that have been built
	 */
	private Set<DirectedSupplementEdgePath> getRandomPaths(MethodGraph graph, FieldAccess fa, Set<DirectedEdge> newFAedges, Set<JoanaEdge> fromOutEdges, Map<Integer, List<Vertex>> layerNumToVertices, Map<Integer, Vertex> edgeToDummy ,List<Double> layerYvals){
		double boxYtop = fa.getY();
		double boxYbottom = fa.getY() + fa.getSize().y;
		Set<JoanaEdge> faEdges = graph.removeFilteredEdges(fa.getGraph().getEdgeSet());
		Set<DirectedSupplementEdgePath> paths = new HashSet<>();
		List<List<Vertex>> tempLayerNumToVertices = new ArrayList<>();
		for(int i = 0; i<layerNumToVertices.size(); i++){	//init temp layering for removing them in it
			List<Vertex> newList = new ArrayList<>();
			layerNumToVertices.get(i).stream().filter(v->v.getID() < 0).forEach(newList::add);
			tempLayerNumToVertices.add(newList);
		}
		for(JoanaEdge e : faEdges){
			double start = e.getSource().getY();
            double end = e.getTarget().getY();
			int upOrDown = start < end ? 1 : -1;
			int index1 = getIndex(layerYvals, start);
			int index2 = getIndex(layerYvals, end);
			assert(index1 != -1 && index2 != -1);	//start and end have to be found in this list!!!
			if(Math.abs(index1 - index2) > 1){	//edge describes a supplementPath
				List<DirectedEdge> supplementEdges = new ArrayList<>();
				List<Vertex> dummies = new ArrayList<>();
				for(int i = index1 + upOrDown; Math.abs(i - index2) > 0; i+=upOrDown){//adds dummies to the list
//					List<Vertex> layerDummies = layerNumToVertices.get(i).stream().filter(v->v.getID() < 0).collect(Collectors.toList());
					List<Vertex> layerDummies = tempLayerNumToVertices.get(i);
					//int randomIndex = (int)Math.round(Math.floor(Math.random()*layerDummies.size()));
					//dummies.add(layerDummies.get(randomIndex));
					//layerDummies.remove(randomIndex);
                    dummies.add(layerDummies.get(0));
                    layerDummies.remove(0);
				}
				supplementEdges.add(new DefaultDirectedEdge<>("","",e.getSource(), dummies.get(0)));
				for(int i = 0; i < dummies.size() - 1; i++){
					supplementEdges.add(new DefaultDirectedEdge<>("","",dummies.get(i),dummies.get(i + 1)));
				}
				supplementEdges.add(new DefaultDirectedEdge<>("","",dummies.get(dummies.size() - 1),e.getTarget()));
				paths.add(new DefaultDirectedSupplementEdgePath(e, dummies, supplementEdges));
			}
		}
		for(JoanaEdge e : fromOutEdges){
			List<DoublePoint> points = e.getPath().getNodes();
            double start = 0;
            double end = 0;
			int upOrDown = 0;	//+1 is down (increase layer), -1 is up(decrease layer)
			Vertex source = null;	//one of these will be a dummy instead of being a
			Vertex target = null;	//source or target because the vertex out of the fa is replaced by a dummy while layouting
			if(dEquals(points.get(points.size() - 1).y, boxYtop)){ //edge into box from top
				start = boxYtop;
				end = e.getTarget().getY();
				upOrDown = 1;
				source = edgeToDummy.get(e.getID());
				target = e.getTarget();
			}else if(dEquals(points.get(0).y, boxYtop)){//edge out of box from top
				start = e.getSource().getY();
				end = boxYtop;
				upOrDown = -1;
				source = e.getSource();
				target = edgeToDummy.get(e.getID());
			}else if(dEquals(points.get(0).y, boxYbottom)){//edge out of box from bottom
				start = e.getSource().getY();
				end = boxYbottom;
				upOrDown = 1;
				source = e.getSource();
				target = edgeToDummy.get(e.getID());
			}else if(dEquals(points.get(points.size() - 1).y, boxYbottom)){//edge into box from bottom
				start = boxYbottom;
				end = e.getTarget().getY();
				upOrDown = -1;
				source = edgeToDummy.get(e.getID());
				target = e.getTarget();
			}else{
				assert(false);
			}
			int index1 = getIndex(layerYvals, start);
			int index2 = getIndex(layerYvals, end);
			assert(index1 != -1 && index2 != -1);	//start and end have to be found in this list!!!
			if(Math.abs(index1 - index2) > 1){	//edge describes a supplementPath
				List<DirectedEdge> supplementEdges = new ArrayList<>();
				List<Vertex> dummies = new ArrayList<>();
				for(int i = index1 + upOrDown; Math.abs(i - index2) > 0; i+=upOrDown){//adds dummies the list
//					List<Vertex> layerDummies = layerNumToVertices.get(i).stream().filter(v->v.getID() < 0).collect(Collectors.toList());
					List<Vertex> layerDummies = tempLayerNumToVertices.get(i);
					//int randomIndex = (int)Math.round(Math.floor(Math.random()*layerDummies.size()));
					//dummies.add(layerDummies.get(randomIndex));
					//layerDummies.remove(randomIndex);
                    dummies.add(layerDummies.get(0));
                    layerDummies.remove(0);
				}
				supplementEdges.add(new DefaultDirectedEdge<>("","",source, dummies.get(0)));
				for(int i = 0; i < dummies.size() - 1; i++){
					supplementEdges.add(new DefaultDirectedEdge<>("","",dummies.get(i),dummies.get(i + 1)));
				}
				supplementEdges.add(new DefaultDirectedEdge<>("","",dummies.get(dummies.size() - 1),target));
				paths.add(new DefaultDirectedSupplementEdgePath(new DefaultDirectedEdge<>("","",source, target), dummies, supplementEdges));
			}else{//edge from outside goes direct into the first vertex (start layer +1 or last layer - 1), so just build a normal edge!
				assert(Math.abs(index1 - index2) == 1);	//should be so !
				newFAedges.add(new DefaultDirectedEdge<>("","",source,target));
			}
		}
		return paths;
	}
	
	/**
	 * Finally draws all edges new. Adds all required edges and vertices to sets that are given to the sugiyamaLayoutAlgorithm.
	 * Also adjusts EdgePaths of edges coming into or going out of the FieldAccess because these edges have been replaced by an edge that is completely in the FieldAccess.
	 * Adds the new drawn edgepath to the existing one of the edges.
	 * 
	 * @param fa the FieldAccess
	 * @param newFAedges new edges to add to faedges
	 * @param paths the paths
	 * @param fromOutEdges edges from outside into or out of the FieldAccess
	 * @param edgeToDummy mapping of edge id to the dummy that replaces the vertex being outside of the FieldAccess.
	 */
	private void drawAndAdjustFieldAccessEdges(MethodGraph graph, FieldAccess fa, Set<DirectedEdge> newFAedges, Set<DirectedSupplementEdgePath> paths, Set<JoanaEdge> fromOutEdges, Map<Integer, Vertex> edgeToDummy){
//		this.printGraph(fa.getGraph().getVertexSet(), fa.getGraph().getEdgeSet());
		double boxYtop = fa.getY();
		double boxYbottom = fa.getY() + fa.getSize().y;
		Set<Vertex> vertices = new HashSet<>(graph.removeFilteredVertices(fa.getGraph().getVertexSet()));
		Set<DirectedEdge> edges = new HashSet<>();
		edges.addAll(graph.removeFilteredEdges(fa.getGraph().getEdgeSet()));
		edges.addAll(newFAedges);	//edges from outside going to the next vertex in FA, not describing a path (not skipping a layer)
		for(DirectedEdge edge : newFAedges){
			vertices.add(edge.getSource());
			vertices.add(edge.getTarget());
		}
		for(DirectedSupplementEdgePath p : paths){
			vertices.addAll(p.getDummyVertices());
			vertices.add(p.getReplacedEdge().getSource());
			vertices.add(p.getReplacedEdge().getTarget());
			edges.addAll(p.getSupplementEdges());
			edges.remove(p.getReplacedEdge());//should not be in here !
		}
		this.sugiyamaLayoutAlgorithm.drawEdgesNew(vertices, edges, paths);
		//now add the additional EdgePath to fromOutEdges
		for(JoanaEdge e : fromOutEdges){
			List<DoublePoint> points = e.getPath().getNodes();
			Vertex dummy = edgeToDummy.get(e.getID());
			assert(vertices.contains(dummy));
			Vertex source = null;
			Vertex target = null;
			boolean newPathInsertionAfter = false;	//tells if new path should be inserted behind or before the actual path
			if(dEquals(points.get(points.size() - 1).y, boxYtop)){ //edge into box from top
				source = dummy;
				target = e.getTarget();
				newPathInsertionAfter = true;
			}else if(dEquals(points.get(0).y, boxYtop)){//edge out of box from top
				source = e.getSource();
				target = dummy;
				newPathInsertionAfter = false;
			}else if(dEquals(points.get(0).y, boxYbottom)){//edge out of box from bottom
				source = e.getSource();
				target = dummy;
				newPathInsertionAfter = false;
			}else if(dEquals(points.get(points.size() - 1).y, boxYbottom)){//edge into box from bottom
				source = dummy;
				target = e.getTarget();
				newPathInsertionAfter = true;
			}else{
				assert(false);
			}
			//merge EdgePath from edges in the FieldAccess and edges going from out into the FieldAccess
			List<DirectedEdge> faEdges = new LinkedList<>();
			faEdges.addAll(edges);
			paths.forEach(p->faEdges.add(p.getReplacedEdge()));

			for(DirectedSupplementEdgePath p : paths){	//look here for matching and finding correct drawn edges for every fromOutEdge!!!
				DirectedEdge replaced = p.getReplacedEdge();
				if(replaced.getSource().getID().equals(dummy.getID()) || replaced.getTarget().getID().equals(dummy.getID())){
					EdgePath op = e.getPath();
					EdgePath np = replaced.getPath();
					mergeEdgePaths(np,op,newPathInsertionAfter);
				}
			}
			for(DirectedEdge de : edges){
				if(dummy.getID().equals(de.getSource().getID()) && target.getID().equals(de.getTarget().getID())
						|| dummy.getID().equals(de.getTarget().getID()) && source.getID().equals(de.getSource().getID())){
					EdgePath op = e.getPath();
					EdgePath np = de.getPath();
					mergeEdgePaths(np,op,newPathInsertionAfter);
				}
			}
		}
	}

	/**
	 * Merges both EdgePaths together.
	 * Boolean tells if new path has to be added behind the old path, or the old path behind new path.
	 * In addition adjusts the x coordinate of the last two points of the first path with the x-coordinate of the first point of the second path, so there is no unnecessary kink.
	 */
	private void mergeEdgePaths(EdgePath newPath, EdgePath oldPath, boolean newPathInsertionAfter){
		assert(newPath.getSegmentsCount() >= 2);
		assert(oldPath.getSegmentsCount() >= 2);
		DoublePoint np1,np2;
		if(newPathInsertionAfter){ //also replace np's first two elements
			np1 = new DoublePoint(oldPath.getNodes().get(oldPath.getNodes().size()-1).x,newPath.getNodes().get(0).y);
			np2 = new DoublePoint(oldPath.getNodes().get(oldPath.getNodes().size()-1).x,newPath.getNodes().get(1).y);
			newPath.getNodes().remove(0);
			newPath.getNodes().remove(0);
			oldPath.addPoint(np1);
			oldPath.addPoint(np2);
			newPath.getNodes().forEach(oldPath::addPoint);
		} else{ //also replace np's last two elements
			List<DoublePoint> newPoints = new LinkedList<>();
			np1 = new DoublePoint(oldPath.getNodes().get(0).x,newPath.getNodes().get(newPath.getNodes().size() - 2).y);
			np2 = new DoublePoint(oldPath.getNodes().get(0).x,newPath.getNodes().get(newPath.getNodes().size() - 1).y);
			newPoints.addAll(newPath.getNodes().subList(0,newPath.getNodes().size() - 2)); //remove last two elements
			newPoints.add(np1);
			newPoints.add(np2);
			newPoints.addAll(oldPath.getNodes());
			oldPath.clear();
			newPoints.forEach(oldPath::addPoint);
		}
		//TODO: later: check if point replacing/removing is still necessary or yet possible when drawing path for the first time
	}
	
	/**
	 * Draws every InterproceduralEdge of this MethodGraph.
	 * There is enough space right and left from every normal vertex that is connected with dummy vertices of InterproceduralEdges
	 * Dummies of a InterproceduralEdge are drawn as following: (Example of two normal vertices with each having 3 dummies)
	 * __   __   ____   __         ____
     *|__| |__| |    | |__|   __  |    |  __   __
     *          |____|       |__| |____| |__| |__|
     *
	 * 
	 * Whether the dummy vertices are drawn on the top or the bottom side of the normal vertex depends on the kind of the normal vertex.
     * Vertices of kind FRMI, FRMO, EXIT have dummies on top, every other vertex on their bottom line
     *
     * Whether the dummy vertices are drawn left or right of the normal vertex depends on the direction of the edge that connects them.
     * InterproceduralEdges going into a normal vertex are drawn left of the normal vertex, IE's with incoming edges are drawn on the right.
	 * 
	 */
	private void drawInterproceduralEdges(MethodGraph mg){
		Map<Integer, Set<InterproceduralEdge>> idToIEs = mg.getInterproceduralEdges();
		Map<Integer, Set<InterproceduralEdge>> newIdToIEs = new HashMap<>();
		//adjust each normal vertex' interprocedural edges depending on actual vertex filter settings
		for(int idx : idToIEs.keySet()){
			newIdToIEs.put(idx, mg.removeFilteredInterproceduralEdges(idToIEs.get(idx)));
		}
		mg.calcLeftRightMarginNew(newIdToIEs);
		for(Set<InterproceduralEdge> ies : idToIEs.values()){
		    if(ies.isEmpty()) continue;
		    JoanaVertex normalVertex = ies.iterator().next().getNormalVertex();
            Position actPosition = Position.BOTTOM; //default value
            if(normalVertex.getNodeKind() == JoanaVertex.VertexKind.FRMI || normalVertex.getNodeKind() == JoanaVertex.VertexKind.FRMO || normalVertex.getNodeKind() == JoanaVertex.VertexKind.EXIT){
                actPosition = Position.TOP;
            }
            this.drawInterproceduralEdgesAtPosition(ies,actPosition);
            mg.addInterproceduralEdges(ies);
        }
	}

	//draws interprocedural edges, for every edge: set its edgepath and the position of its dummy vertex
    //all interprocedural edges of this set have the same normal vertex
	private void drawInterproceduralEdgesAtPosition(Set<InterproceduralEdge> ieSet, Position position){
	    assert(!ieSet.isEmpty());
	    ieSet.forEach(ie->ie.getPath().clear()); //clears edgepath before relayouting
	    JoanaVertex normalVertex = ieSet.iterator().next().getNormalVertex();
        Set<InterproceduralEdge> sourceIEs = ieSet.stream().filter(ie->ie.getDummyLocation() == InterproceduralEdge.DummyLocation.SOURCE).collect(Collectors.toSet());
        Set<InterproceduralEdge> targetIEs = ieSet.stream().filter(ie->ie.getDummyLocation() == InterproceduralEdge.DummyLocation.TARGET).collect(Collectors.toSet());
        double leftEdgeDist = 0;
        double rightEdgeDist = 0;
        if(!sourceIEs.isEmpty()){
            leftEdgeDist = (normalVertex.getSize().y - sourceIEs.iterator().next().getDummyVertex().getSize().y) / (sourceIEs.size()+1);//vertical distance between 2 edges on left side (asserting that all dummies have the same size)
        }
        if(!targetIEs.isEmpty()){
            rightEdgeDist = (normalVertex.getSize().y - targetIEs.iterator().next().getDummyVertex().getSize().y) / (targetIEs.size()+1);//vertical distance between 2 edges on right side (asserting that all dummies have the same size)
        }
        double xVal = normalVertex.getX();
        double yVal;
        int idx = 1; //number of IE in the loop. First one has number 1!
        for(InterproceduralEdge ie : sourceIEs){//draw IE's with source dummy to the left of the normalVertex
            JoanaVertex dummyVertex = ie.getDummyVertex();
            if(position == Position.TOP){
                yVal = normalVertex.getY();
            }else{
                yVal = normalVertex.getY() + normalVertex.getSize().y - dummyVertex.getSize().y;
            }
            xVal -= dummyVertex.getLeftRightMargin().y + dummyVertex.getSize().x; //leftmost x value of this iv
            dummyVertex.setX(xVal);
            dummyVertex.setY(yVal);
            xVal -= dummyVertex.getLeftRightMargin().x; //set to end of actual iv left margin

            EdgePath path = ie.getPath();
            if(position == Position.TOP){
                path.addPoint(new DoublePoint(normalVertex.getX(),dummyVertex.getY() + dummyVertex.getSize().y + idx*leftEdgeDist));//first point: on the Vertex's left side
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2,dummyVertex.getY() + dummyVertex.getSize().y + idx*leftEdgeDist));//second point: below the middle of the IV's bottom
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2,dummyVertex.getY() + dummyVertex.getSize().y)); //third point: middle of the IV's bottom
            }else{ //case: edges being on bottom line of connected vertex
                path.addPoint(new DoublePoint(normalVertex.getX(),dummyVertex.getY() - idx*leftEdgeDist));//first point: on the Vertex's left side
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2,dummyVertex.getY() - idx*leftEdgeDist));//second point: on top of the IV's top
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2, dummyVertex.getY())); //third point: middle of the IV's top
            }
            if(ie.getDummyLocation() == InterproceduralEdge.DummyLocation.SOURCE){//from dummyVertex to normalVertex -> wrong direction: turn EdgePath
                Collections.reverse(path.getNodes());
            }
            idx++;
        }
        xVal = (normalVertex.getX() + normalVertex.getSize().x); //rightmost x coordinate of the normalVertex
        idx = 1;
        for(InterproceduralEdge ie : targetIEs){//draw IE's with target dummy to the right side of the normalVertex
            JoanaVertex dummyVertex = ie.getDummyVertex();
            if(position == Position.TOP){
                yVal = normalVertex.getY();
            }else{
                yVal = normalVertex.getY() + normalVertex.getSize().y - dummyVertex.getSize().y;
            }
            xVal += dummyVertex.getLeftRightMargin().x; //rightmost value of dummie's left margin/its x-coordinte
            dummyVertex.setX(xVal);
            dummyVertex.setY(yVal);
            xVal += dummyVertex.getSize().x + dummyVertex.getLeftRightMargin().y; //rightmost value of its right margin
            EdgePath path = ie.getPath();
            if(position == Position.TOP){
                path.addPoint(new DoublePoint(normalVertex.getX() + normalVertex.getSize().x,dummyVertex.getY() + dummyVertex.getSize().y + idx*rightEdgeDist));//first point: on the normal's right side
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2,dummyVertex.getY() + dummyVertex.getSize().y + idx*rightEdgeDist));//second point: below the middle of the dummy's's bottom
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2,dummyVertex.getY() + dummyVertex.getSize().y)); //third point: middle of the dummy's bottom
            }else{ //case: edges being on bottom line of connected vertex
                path.addPoint(new DoublePoint(normalVertex.getX() + normalVertex.getSize().x,dummyVertex.getY() - idx*rightEdgeDist));//first point: on the normal's right side
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2,dummyVertex.getY() - idx*rightEdgeDist));//second point: on top of the dummy's top
                path.addPoint(new DoublePoint(dummyVertex.getX() + dummyVertex.getSize().x / 2, dummyVertex.getY())); //third point: middle of the dummy's top
            }
            if(ie.getDummyLocation() == InterproceduralEdge.DummyLocation.SOURCE){//from dummyVertex -> wrong direction: turn EdgePath
                Collections.reverse(path.getNodes());
            }
            idx++;
        }
    }
	
	/**
	 * Get index of value in list.
	 * Index 0 is the first index.
	 * -1 is returned, if value is not contained in list.
	 */
	private int getIndex(List<Double> list, double value){
		int index = 0;
		for(double d : list){
			if(dEquals(d, value)){
				return index;
			}
			index++;
		}
		return -1;
	}
	
//	private void printGraph(Set<Vertex> vertices, Set<DirectedEdge> edges){
//		String res = "Vertices:";
//		for(Vertex v : vertices){
//			res += v.getID() +", ";
//		}
//		System.out.println(res);
//		res="Edges: ";
//		for(DirectedEdge e : edges){
//			res += e.getSource().getID()+"->"+e.getTarget().getID()+", ";
//		}
//		System.out.println(res);
//	}
//	
//	private void printGraph(List<Vertex> vertices, List<DirectedEdge> edges){
//		String res = "Vertices:";
//		for(Vertex v : vertices){
//			res += v.getY() +", ";
//		}
//		System.out.println(res);
//		res="Edges: ";
//		for(DirectedEdge e : edges){
//			res += e.getSource().getY()+"->"+e.getTarget().getY()+", ";
//		}
//		System.out.println(res);
//	}
	
	private Integer getDummyID(){
		Integer temp = this.dummyId;
		dummyId--;
		return temp;
	}
	
	
	
	
	private Set<JoanaEdge> getEdgesFromOutside(MethodGraph graph, FieldAccess fa){
		Set<JoanaEdge> fromOutEdges = new HashSet<>();
		Set<JoanaEdge> graphEdges = graph.getEdgeSet();
		Set<JoanaVertex> graphVertices = graph.getVertexSet();
		Set<JoanaVertex> faVertices = fa.getGraph().getVertexSet();
		for(JoanaEdge e : graphEdges){
			if((graphVertices.contains(e.getSource()) && faVertices.contains(e.getTarget()) && !faVertices.contains(e.getSource()))
					|| (graphVertices.contains(e.getTarget()) && faVertices.contains(e.getSource()) && !faVertices.contains(e.getTarget()))){
				fromOutEdges.add(e);
			}
		}
//		logger.debug("from out edges: "+fromOutEdges.size());
//		logger.debug("box pos: "+fa.getX()+","+fa.getY()+" size: "+fa.getSize().x+","+fa.getSize().y);
//		for(JoanaVertex v : faVertices){
//			logger.debug("vertex pos: "+v.getX()+","+v.getY()+" size: "+v.getSize().x+","+v.getSize().y);
//		}
		return fromOutEdges;
	}
	
	private List<FieldAccess> expandFieldAccesses(MethodGraph graph, List<FieldAccess> fasToExpand) {

		// Fill a map from id to edge for all outgoing edges of field accesses to match them later
		// with the new edges of the graph to the specific vertices in the field access
		Map<Integer, JoanaEdge> idToEdge = new HashMap<>();
		for (FieldAccess fa : fasToExpand) {
		    for (JoanaEdge e : graph.edgesOf(fa)) {
		        idToEdge.put(e.getID(), e);
		    }
		}

		List<FieldAccess> fas = graph.expandFieldAccesses(fasToExpand);
		Set<JoanaEdge> copied = new HashSet<>();

		for (FieldAccess fa : fas) {

		    // Offset vertices contained in field access
            for(JoanaVertex v : fa.getGraph().getVertexSet()){
                v.setX(v.getX() + fa.getX() + FieldAccessGraph.paddingx/2d);
                v.setY(v.getY() + fa.getY() + FieldAccessGraph.paddingy/2d);
            }
            Set<JoanaEdge> edges = graph.removeFilteredEdges(fa.getGraph().getEdgeSet());
			for(JoanaEdge e : edges){
				List<DoublePoint> points = e.getPath().getNodes();
				List<DoublePoint> newPoints = new LinkedList<>();
				assert(!points.isEmpty());
				points.forEach(p->newPoints.add(new DoublePoint(p.x + fa.getX() + FieldAccessGraph.paddingx/2d,
				                                                p.y + fa.getY() + FieldAccessGraph.paddingy/2d)));
				points.clear();
				points.addAll(newPoints);
			}
			for (JoanaVertex v : fa.getGraph().getVertexSet()) {
			    for (JoanaEdge e : graph.edgesOf(v)) {
			        if (!copied.contains(e)) {

                        copied.add(e);
                        Set<JoanaVertex> faVertices = fa.getGraph().getVertexSet();
                        if (!faVertices.contains(e.getTarget()) || !faVertices.contains(e.getSource())) {
                            JoanaEdge matchingEdge = idToEdge.get(e.getID());
                            for (DoublePoint point : matchingEdge.getPath().getNodes()) {
                                e.getPath().addPoint(point);
                            }
                        }
			        }
			    }
			}
        }
		for (JoanaEdge e : graph.getEdgeSet()) {
		    List<DoublePoint> points = e.getPath().getNodes();
		    DoublePoint first = points.get(0);
		    Iterator<DoublePoint> it = points.iterator();
		    it.next();
		    while (it.hasNext()) {
		        DoublePoint p = it.next();
                assert (!first.equals(p));
		    }
		}
		return fas;
	}
	
	
	//private Method to get all param vertices of a joana call vertex
    private Set<Vertex> getParamVerticesOfCall(JoanaVertex call, MethodGraph graph) {
        Set<Vertex> result = new HashSet<>();
        
        for (JoanaEdge e : graph.outgoingEdgesOf(call)) {
            //check if edge is Control_dep_expr
            if (e.getEdgeKind().equals(JoanaEdge.EdgeKind.CE)) {
                //check if edge is act-in or act-out
                JoanaVertex v = e.getTarget();
                if (v.getNodeKind().equals(JoanaVertex.VertexKind.ACTI)
                        || v.getNodeKind().equals(JoanaVertex.VertexKind.ACTO)) {
                   result.add(v); 
                }
            }
        }
        return result;
    }
    
    private boolean dEquals(double a, double b){
		return Math.abs(a-b) < Math.pow(10, -6);
	}

    /**
     * Position of a smaller dummy vertex relative to a bigger vertex connected with him
     */
	private enum Position{
        TOP, BOTTOM
    }
    
    /**
     * For representing dummy vertices with fixed size and given id.
     */
    private static class JoanaDummyVertex implements Vertex{
    	private double x;
    	private double y;
    	private final Integer id;
    	private final String name;
    	private final String label;
    	private final DoublePoint size;

    	JoanaDummyVertex(String name, String label, int id, DoublePoint size){
    		this.name = name;
    		this.label = label;
    		this.id = id;
    		this.size = size;
    	}
    	
		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Integer getID() {
			return this.id;
		}

		@Override
		public String getLabel() {
			return this.label;
		}

		@Override
		public double getX() {
			return this.x;
		}

		@Override
		public double getY() {
			return this.y;
		}

		@Override
		public void setX(double x) {
			this.x = x;
			
		}

		@Override
		public void setY(double y) {
			this.y = y;
			
		}

		@Override
		public void addToFastGraphAccessor(FastGraphAccessor fga) {
			//not necessary
		}

		@Override
		public List<GAnsProperty<?>> getProperties() {
			// not necessary
			return null;
		}

		@Override
		public DoublePoint getSize() {
			return this.size;
		}
		
		@Override
		public IntegerPoint getLeftRightMargin() {
			return getDefaultLeftRightMargin();
		}

        @Override
        public IntegerPoint getDefaultLeftRightMargin(){
            return new IntegerPoint(2,2);
        }
		
		@Override
		public void setLeftRightMargin(IntegerPoint newMargin) {
			//don't need this here
		}

		@Override
		public Color getColor() {
			// not necessary
			return null;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
    }

}
