package edu.kit.student.sugiyama.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.student.graphmodel.Edge;
import edu.kit.student.graphmodel.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.student.graphmodel.EdgePath;
import edu.kit.student.sugiyama.graph.IEdgeDrawerGraph;
import edu.kit.student.sugiyama.graph.ISugiyamaEdge;
import edu.kit.student.sugiyama.graph.ISugiyamaVertex;
import edu.kit.student.sugiyama.graph.SugiyamaGraph.SupplementPath;
import edu.kit.student.util.DoublePoint;

/**
 * This class takes a directed graph, as a {@link edu.kit.student.sugiyama.graph.SugiyamaGraph}.
 * It removes dummy vertices and reverses previously reversed edges.
 * Afterwards it assigns every edge points it must run through.
 * 
 */
public class EdgeDrawer implements IEdgeDrawer {
	private IEdgeDrawerGraph graph;
	private Set<SupplementPath> paths;
	private Set<ISugiyamaVertex> graphVertices;                        // all vertices of the graph, with dummy vertices
	private Set<ISugiyamaEdge> graphEdges;                             // all edges of the graph, with supplement edges

	private Set<ISugiyamaEdge> selfLoopEdges;	                       // edges with same source and target vertex
	private Set<ISugiyamaEdge> sameLayerEdges;	                       // edges that connect two vertices of the same layer, (for now without selfloops!)
	private Set<ISugiyamaEdge> normalEdges;	                           // edges that are not (supplement edges, selfLoopEdges or sameLayerEdges)

    private double[][] layerYoffset;                                   //describes lowest and highest y-coordiante of a vertex in a layer.
	private double[] spaceBetweenLayers;
	private double[] distancePerEdgeInLayer;

	
	private Map<Integer,int[]> inOutDeg;                               // maps vertex id, to an array, containing the indegree on index 0 and outdegree on index 1
	private Map<Integer, List<List<DoublePoint>>> inOutPoints;         // maps vertex id to Lists of incoming and outgoing points
	private Map<Integer, List<List<ISugiyamaVertex>>> inOutVertices;   // maps vertex id to incoming and outgoing vertices
	private Map<Integer, List<List<ISugiyamaEdge>>> vertexToEdges;	   // maps vertex id to incoming and outgoing edges
	private Map<Integer, List<DoublePoint>> edgeToInOutPoints;
	
    private final Logger logger = LoggerFactory.getLogger(EdgeDrawer.class);

	@Override
	public void drawEdges(IEdgeDrawerGraph graph) {
        inOutDeg = new HashMap<>();
        inOutPoints = new HashMap<>();
        inOutVertices = new HashMap<>();
        vertexToEdges = new HashMap<>();
        edgeToInOutPoints = new HashMap<>();

		logger.info("EdgeDrawer.drawEdges():");
		if(graph.getEdgeSet() == null || graph.getEdgeSet().isEmpty() || graph.getVertexSet() == null || graph.getVertexSet().isEmpty()){
			return;
		}
		initialize(graph);            //initializes the graph and it's sets, needed in the whole class!
		fillInOutDeg();               //fills mapping of vertex to degrees
		fillInOutPoints();            //fills mapping of vertex to points where edges come in or are going out
		fillInOutVertices();          //fills mapping of vertex to incoming and outgoing vertices
		sortInOutVertices();          //sorts the lists of in- and out vertices in ascending order of their X-coordinate
        calcLayerYoffset();
		calcSpaceBetweenLayers();     //fills the array that contains the space between two layers
		calcDistancePerEdgeInLayer(); //fills the array that contains the distance between two kinking edges. (if all edges are kinking, the distance between two adjacency edges is constant)

        fillVertexToEdges();
        fillEdgeToInOutPoints();
        drawAllEdges();
        adjustSupplementPaths();

		this.graphEdges.stream().filter(ISugiyamaEdge::isReversed).forEach(this::reverseEdgePath);	//reverses edge paths from reversed edges
		this.graphEdges.stream().filter(ISugiyamaEdge::isReversed).forEach(ISugiyamaEdge::reverse);
		for(SupplementPath p : this.paths){	//reverses edgepath of replaced edges in supplement paths
			ISugiyamaEdge e = p.getReplacedEdge();
			if(e.isReversed()){
				this.reverseEdgePath(e);
				p.reverse();
			}
		}
	    inOutDeg.clear();
	    inOutPoints.clear();
	    inOutVertices.clear();
	    vertexToEdges.clear();
	    edgeToInOutPoints.clear();
	}
	
	//tests every edge if its edgepath describes an orthogonal edge
	private void testEdgePaths(){
		for(ISugiyamaEdge e : this.graphEdges){
			List<DoublePoint> l = e.getPath().getNodes();
			assert(l.size() % 2 == 0);
			DoublePoint first = l.get(0);
			System.out.println("first: " + first.toString());
			DoublePoint second;
			for(int i = 1; i < l.size(); i++){
				second = l.get(i);
				System.out.println(second.toString());
				assert(dEquals(first.x, second.x) ^ dEquals(first.y,second.y));
				first=second;
			}
		}
	}

	private void printVertices(){
	    for(int i = 0; i < graph.getLayerCount() - 1; i++){
	        List<ISugiyamaVertex> vertices = graph.getSortedLayer(i);
	        StringBuilder out = new StringBuilder();
	        for(ISugiyamaVertex v : vertices){
	            out.append("pos:[").append(v.getX()).append(",").append(v.getY()).append("] size:(").append(v.getSize().x).append(",").append(v.getSize().y).append("); ");
            }
            System.out.println(out);
        }
    }

    private void printEdgePaths(){
		List<ISugiyamaEdge> edges = new LinkedList<>();
		edges.addAll(this.normalEdges);
		edges.addAll(this.selfLoopEdges);
		edges.addAll(this.sameLayerEdges);
		this.paths.forEach(p->edges.add(p.getReplacedEdge()));
		for(ISugiyamaEdge e : edges){
			StringBuilder out = new StringBuilder();
			out.append("(").append(e.getSource().getName()).append("->").append(e.getTarget().getName()).append(")");
			out.append(e.getPath().getNodes().size()).append("[");
			for(DoublePoint p : e.getPath().getNodes()){
				out.append("(").append(p.x).append(",").append(p.y).append(")|");
			}
			out.append("]");
			if(e.getPath().getNodes().isEmpty())
				System.out.println(out);
		}
	}
	
	/**
	 * Fills the mapping of Integer vertex-id to an Integer array.
	 * The array size is 2. In the index 0 is the indegree of this vertex, in the index 1 its outdegree.
	 */
	private void fillInOutDeg(){
        for(ISugiyamaEdge e :this.graphEdges){
			ISugiyamaVertex source = e.getSource();
			ISugiyamaVertex target = e.getTarget();
			if(!this.inOutDeg.containsKey(source.getID())){	//add source vertex to map, if not contained
				this.inOutDeg.put(source.getID(), new int[2]);
			}
			if(!this.inOutDeg.containsKey(target.getID())){	//add target vertex to map, if not contained
				this.inOutDeg.put(target.getID(), new int[2]);
			}
			this.inOutDeg.get(source.getID())[1]++;	//source vertex got one outgoing edge more
            if(Objects.equals(source.getID(), target.getID())){ //selfloop: add additional out point for source vertex. Will later be used as in point
                this.inOutDeg.get(source.getID())[1]++;
            }else if(source.getLayer() == target.getLayer()){ //same layer edge: add an outpoint to target vertex. Will later be used as an in point
                this.inOutDeg.get(target.getID())[1]++;
            }else{  //normal edge: add an in point for target vertex
                this.inOutDeg.get(target.getID())[0]++;
            }
		}
		//possibly isolated vertices
		//maybe simplify by searching for isolated vertices before
		for(ISugiyamaVertex v : this.graphVertices){
			if(!this.inOutDeg.containsKey(v.getID())){
				this.inOutDeg.put(v.getID(), new int[2]);
			}
		}
	}
	
	/**
	 * Fills mapping of Integer vertex-id to an List.
	 * This lists contains two Lists of Points. 
	 * The first list contains the Points at which edges go into the vertex, the second at which edges go out of this vertex.
	 */
	private void fillInOutPoints(){
		for(ISugiyamaVertex v : this.graphVertices){
				List<DoublePoint> inPoints = this.getInPoints(v);
				List<DoublePoint> outPoints = this.getOutPoints(v);
				List<List<DoublePoint>> list = new ArrayList<>(2);
				list.add(inPoints);
				list.add(outPoints);
				assert(!this.inOutPoints.containsKey(v.getID()));
				this.inOutPoints.put(v.getID(),list);
		}
	}
	
	/**
	 * Fills mapping of Integer vertex.id to an list that contains two lists of ISugiyamaVertex.
	 * The first list contains vertices that are going in this vertex, the second ones going out of this vertex.
	 */
	private void fillInOutVertices(){
		for(ISugiyamaEdge e : this.graphEdges){
			if(!this.inOutVertices.containsKey(e.getSource().getID())){	// add new entry(in- and out- lists) for source id
				List<List<ISugiyamaVertex>> list = new LinkedList<>();
				list.add(new LinkedList<>());
				list.add(new LinkedList<>());
				this.inOutVertices.put(e.getSource().getID(), list);
			}
			if(!this.inOutVertices.containsKey(e.getTarget().getID())){ // add new entry(in- and out- lists) for target id
				List<List<ISugiyamaVertex>> list = new LinkedList<>();
				list.add(new LinkedList<>());
				list.add(new LinkedList<>());
				this.inOutVertices.put(e.getTarget().getID(), list);
			}
			this.inOutVertices.get(e.getSource().getID()).get(1).add(e.getTarget());	//source vertex got one outgoing vertex, the target of this edge
			if(e.getSource().getLayer() != e.getTarget().getLayer()){	//no selfloop, no same layer edge
				this.inOutVertices.get(e.getTarget().getID()).get(0).add(e.getSource());	//target vertex got one incoming vertex, the source of this edge
			}
		}
	}
	
	/**
	 * Sorts all lists of the mapping of vertex-id to in- and out vertices in ascending order of their X-coordinate (from left to right)
	 */
	private void sortInOutVertices(){
		for(List<List<ISugiyamaVertex>> list : this.inOutVertices.values()){
			list.get(0).sort(Comparator.comparingDouble(Vertex::getX));
			list.get(1).sort(Comparator.comparingDouble(Vertex::getX));
		}
	}

	private void calcLayerYoffset(){
        for(int i = 0; i < graph.getLayerCount(); i++) {
            List<ISugiyamaVertex> layer = graph.getSortedLayer(i);
            double lowest = Double.MAX_VALUE; //lowest y-point in layer
            double highest = Double.MIN_VALUE; //highest y-point in layer
            for (ISugiyamaVertex v : layer) {
                highest = Math.max(highest, v.getY() + v.getSize().y);
                lowest = Math.min(lowest, v.getY());
            }
            this.layerYoffset[i][0] = lowest;
            this.layerYoffset[i][1] = highest;
        }
    }
	
	/**
	 * Calculates the space between two layers that are among themselves. 
	 * The calculated space depends on the point of a box of a vertex that has the highest Y-coordinate of the upper layer and the 
	 * vertex with the lowest Y-coordinate of it's box on the lower layer.
	 * The space between these layers is the difference between both points.
	 * The first entry in the array spaceBetweenLayers with index 0 describes the space between layer 0 and 1, and so on.
	 */
	private void calcSpaceBetweenLayers(){
	    for(int i = 0; i < graph.getLayerCount() - 1; i++){
	        this.spaceBetweenLayers[i] = this.layerYoffset[i+1][0] - this.layerYoffset[i][1];
	        assert(spaceBetweenLayers[i] > 0);
        }
	}
	
	/**
	 * Calculates the minimum distance between two edges going horizontally above each other between two layers. 
	 * If all edges are going horizontally above each other, the distance between two neighbor edges is always the same.
	 */
	private void calcDistancePerEdgeInLayer(){
	    //As in out deg was modified, samelayer and selfloops have 2 points on upper layer but just one edge will be drawn.
        //-> between two layers are: sum(indeg lower layer) + (sum(outdeg upper layer) - sum(indeg lower layer)) / 2 edges!
		for(int i = 0; i <= graph.getLayerCount() - 2; i++){
			List<ISugiyamaVertex> upperLayer = graph.getSortedLayer(i);
			List<ISugiyamaVertex> lowerLayer = graph.getSortedLayer(i+1);
			int edgesCount;
			int upperLayerOutCount = 0;
			int lowerLayerInCount = 0;
            for(ISugiyamaVertex u : upperLayer){
                upperLayerOutCount += this.inOutDeg.get(u.getID())[1];
            }
			for(ISugiyamaVertex l : lowerLayer){
                lowerLayerInCount += this.inOutDeg.get(l.getID())[0];
			}
			assert((upperLayerOutCount - lowerLayerInCount) % 2 == 0); //on upper layer there are 2 outpoints for each selfloop and same layer edge. These points have no corresponding in point in lower layer.
			edgesCount = lowerLayerInCount + (upperLayerOutCount - lowerLayerInCount) / 2;
			this.distancePerEdgeInLayer[i] = (this.spaceBetweenLayers[i] / (edgesCount + 1.0));
		}
		this.distancePerEdgeInLayer[graph.getLayerCount() - 1] = 5; //under the last layer there is no normal edge, but selfloops are possible
	}

    //vertex to normal edges(no selfloops, no same layer edges)
	private void fillVertexToEdges(){
	    //use normal edges(sugy edges without self loops and same layer edges) and supplement edges (from supplement paths) only.
        //watch out, through this there will be more points in point mapping than edges. so the normal edges take points from left, the other edges take the remaining points from right later
		for(ISugiyamaEdge e : this.graphEdges){
		    if(e.getSource().getLayer() == e.getTarget().getLayer()) continue; //vertex to edges mapping just filled for edges that don't have their vertices on the same layer(no selfloops, no same layer edges)
			ISugiyamaVertex source = e.getSource();
			ISugiyamaVertex target = e.getTarget();
			if(!this.vertexToEdges.containsKey(source.getID())){
				List<List<ISugiyamaEdge>> list = new LinkedList<>();
				list.add(new LinkedList<>());
				list.add(new LinkedList<>());
				this.vertexToEdges.put(source.getID(),list);
			}
			if(!this.vertexToEdges.containsKey(target.getID())){
				List<List<ISugiyamaEdge>> list = new LinkedList<>();
				list.add(new LinkedList<>());
				list.add(new LinkedList<>());
				this.vertexToEdges.put(target.getID(),list);
			}
			this.vertexToEdges.get(source.getID()).get(1).add(e);
			this.vertexToEdges.get(target.getID()).get(0).add(e);
		}
		//first sorting edges by their id then stable by their source or targets x-coordinates.
        //incoming edges sorted by their source's x-coordinate, and outgoing edges sorted by their target's x-coordinate.
		//sorting also by id is necessary for many edges having the same source and target vertex
		for(List<List<ISugiyamaEdge>> lists : this.vertexToEdges.values()){
			lists.get(0).sort(Comparator.comparingInt(Edge::getID));
			lists.get(0).sort(Comparator.comparingDouble(e -> e.getSource().getX()));
			lists.get(1).sort(Comparator.comparingInt(Edge::getID));
			lists.get(1).sort(Comparator.comparingDouble(e -> e.getTarget().getX()));
		}
	}

	private void printVertexToEdgeMapping(){
	    for(ISugiyamaVertex v : this.graphVertices){
	        if(!this.vertexToEdges.containsKey(v.getID())) continue;
	        StringBuilder out = new StringBuilder("[");
	        for(ISugiyamaEdge e : vertexToEdges.get(v.getID()).get(0)){
	            out.append(e.getID()).append(",");
            }
            out = new StringBuilder(out.substring(0, out.length() - 1));
	        out.append("](").append(v.getID()).append(")[");
	        for(ISugiyamaEdge e : vertexToEdges.get(v.getID()).get(1)){
                out.append(e.getID()).append(",");
            }
            out = new StringBuilder(out.substring(0, out.length() - 1));
	        out.append("]");
            System.out.println(out);
        }
    }

	private void fillEdgeToInOutPoints(){
        //edges like (samelayer edges, selfloops) should not be added in the first loop, map them to points in another if clause
        //for every vertex add for its incoming edges the inpoint to the mapping, and for the outgoing edges the correct out point to the corresponding edge
		for(ISugiyamaVertex v : this.graphVertices){
		    if(!this.vertexToEdges.containsKey(v.getID())) continue;
            //List<List<ISugiyamaEdge>> edgeLists : this.vertexToEdges.values()
			List<ISugiyamaEdge> inEdges = this.vertexToEdges.get(v.getID()).get(0);//edges going into the vertex
			List<ISugiyamaEdge> outEdges = this.vertexToEdges.get(v.getID()).get(1); //edges coming out of this vertex
			if(!inEdges.isEmpty()){
				//now map the sourceOutPoint to the edges in sourceEdges in the same order
				List<DoublePoint> inPoints = this.inOutPoints.get(v.getID()).get(0);
				assert(inEdges.size() <= inPoints.size()); //possibly there are more out points at source vertex than outgoing edges, because some out points are in points for same layer edges or loops.
                Iterator<ISugiyamaEdge> edgeIt = inEdges.iterator();
                Iterator<DoublePoint> pointIt = inPoints.iterator();
                while(edgeIt.hasNext()){
                    ISugiyamaEdge tmpEdge = edgeIt.next();
                    DoublePoint tmpPoint = pointIt.next();
                    if(!this.edgeToInOutPoints.containsKey(tmpEdge.getID())){
                        this.edgeToInOutPoints.put(tmpEdge.getID(),new LinkedList<>());
                    }
                    this.edgeToInOutPoints.get(tmpEdge.getID()).add(0,tmpPoint);//edge goes into the vertex, so its the edge's in point
                }
			}
			if(!outEdges.isEmpty()){
			    //now map targetOutPoint to the edges in targetEdges in the same order
                List<DoublePoint> outPoints = this.inOutPoints.get(v.getID()).get(1);
                assert(outEdges.size() <= outPoints.size());
                Iterator<ISugiyamaEdge> edgeIt = outEdges.iterator();
                Iterator<DoublePoint> pointIt = outPoints.iterator();
                while(edgeIt.hasNext()){
                    ISugiyamaEdge tmpEdge = edgeIt.next();
                    DoublePoint tmpPoint = pointIt.next();
                    if(!this.edgeToInOutPoints.containsKey(tmpEdge.getID())){
                        this.edgeToInOutPoints.put(tmpEdge.getID(),new LinkedList<>());
                    }
                    this.edgeToInOutPoints.get(tmpEdge.getID()).add(tmpPoint);//edge goes out of the vertex, so its the edge's out point. Add it at the back of the list
                }
            }
		}
        //TODO: think, if its necessary that normal edges also remove the taken points from the vertex to points mapping(not really necessary but would be more consistent)
		for(ISugiyamaEdge loop : this.selfLoopEdges){
		    assert(Objects.equals(loop.getSource().getID(), loop.getTarget().getID()));
		    if(!this.edgeToInOutPoints.containsKey(loop.getID())){
                this.edgeToInOutPoints.put(loop.getID(),new LinkedList<>());
            }
            List<DoublePoint> points = this.inOutPoints.get(loop.getSource().getID()).get(1);
            //now take the 2 rightmost points, map it to the edgeToPoint mapping and remove them. Because after all loops the same layer edges can take the rightmost point remaining
            this.edgeToInOutPoints.get(loop.getID()).add(points.get(points.size()-1));
            this.edgeToInOutPoints.get(loop.getID()).add(points.get(points.size()-2));
            points.remove(points.size()-1);
            points.remove(points.size()-1);
        }
        for(ISugiyamaEdge slEdge : this.sameLayerEdges){ //take from source the rightmost outpoint and from target the rightmost outpoint
            if(!this.edgeToInOutPoints.containsKey(slEdge.getID())){
                this.edgeToInOutPoints.put(slEdge.getID(),new LinkedList<>());
            }
		    ISugiyamaVertex source = slEdge.getSource();
		    ISugiyamaVertex target = slEdge.getTarget();
		    List<DoublePoint> sourceOutPoints = this.inOutPoints.get(source.getID()).get(1);
		    List<DoublePoint> targetOutPoints = this.inOutPoints.get(target.getID()).get(1);
		    this.edgeToInOutPoints.get(slEdge.getID()).add(targetOutPoints.get(targetOutPoints.size() - 1));
		    this.edgeToInOutPoints.get(slEdge.getID()).add(sourceOutPoints.get(sourceOutPoints.size() - 1));
		    sourceOutPoints.remove(sourceOutPoints.size() - 1);
		    targetOutPoints.remove(targetOutPoints.size() - 1);
        }
	}




	//drawn from left to right. No influence on drawing of selfloops or samelayer edges.
	private void drawAllEdges(){
        for(int i = 0; i < graph.getLayerCount(); i++){ //normal vertices on last layer have no outgoing edges, but selfloops in last layer might be possible
            List<ISugiyamaVertex> layer = graph.getSortedLayer(i);
            int bottomIdx = 1;
            int topIdx = drawSelfLoops(i); //drawing self loops and adjusting index
            topIdx = drawSameLayerEdges(i,topIdx); //drawing same layer edges and adjusting index
            for(ISugiyamaVertex v : layer){ //iterate over vertices of this layer and draw its edges from left to right,
                if(!this.vertexToEdges.containsKey(v.getID())) continue; //isolated vertices have no vertex to edge mapping
                List<ISugiyamaEdge> outEdges = this.vertexToEdges.get(v.getID()).get(1);
                for(ISugiyamaEdge e : outEdges){
                    ISugiyamaVertex source = e.getSource();
                    ISugiyamaVertex target = e.getTarget();
                    assert(target.getLayer() - source.getLayer() == 1);
                    EdgePath path = e.getPath();
                    path.clear();
                    DoublePoint sPoint = this.edgeToInOutPoints.get(e.getID()).get(1);
                    DoublePoint tPoint = this.edgeToInOutPoints.get(e.getID()).get(0);
                    assert(dEquals(sPoint.y,source.getY()+source.getSize().y));
                    assert(dEquals(tPoint.y, target.getY()));
                    double yKink;
                    if(sPoint.x < tPoint.x){ //draw such edges from bottom to top
                        yKink = this.layerYoffset[i+1][0] - bottomIdx * this.distancePerEdgeInLayer[i];
                        bottomIdx++;
                    }else{ //draw from top to bottom
                        yKink = this.layerYoffset[i][1] + topIdx * this.distancePerEdgeInLayer[i];
                        topIdx++;
                    }
                    path.addPoint(sPoint);
                    if(!dEquals(sPoint.x, tPoint.x)){ //otherwise don't need to kink
                        path.addPoint(new DoublePoint(sPoint.x,yKink));
                        path.addPoint(new DoublePoint(tPoint.x,yKink));
                    }
                    path.addPoint(tPoint);
                }
            }
        }
    }

    //draws selfloops of the given layer num in descending order of the corresponding vertex' x-coordinate from top to bottom
    //gives back the last index a selfloop was drawn to +1 (~ #selfloops in the given layer + 1)
    private int drawSelfLoops(int layer){
        List<ISugiyamaEdge> loops = this.selfLoopEdges.stream().filter(sl->sl.getSource().getLayer() == layer).collect(Collectors.toList()); //selfloops of this layer
        loops.sort(Comparator.comparingDouble(l->l.getSource().getX()));
        Collections.reverse(loops);
        int idx = 1;
        for(ISugiyamaEdge l : loops){
            assert(Objects.equals(l.getSource().getID(), l.getTarget().getID()));
            assert(l.getSource().getLayer() == l.getTarget().getLayer());
            EdgePath path = l.getPath();
            path.clear();
            DoublePoint sPoint = this.edgeToInOutPoints.get(l.getID()).get(1);
            DoublePoint tPoint = this.edgeToInOutPoints.get(l.getID()).get(0);
            double yKink = sPoint.y + idx * this.distancePerEdgeInLayer[layer];
            path.addPoint(sPoint);
            path.addPoint(new DoublePoint(sPoint.x,yKink));
            path.addPoint(new DoublePoint(tPoint.x,yKink));
            path.addPoint(tPoint);
            idx++;
        }
        return idx;
    }

    //draws same layer edges of the given layer num in descending order of their targets x-coordinate from top to bottom starting at the given index
    //gives back the last index a same layer edge was drawn to +1 (~ #same layer edges in the given layer + 1)
    private int drawSameLayerEdges(int layer, int index){
        List<ISugiyamaEdge> slEdges = this.sameLayerEdges.stream().filter(sle->sle.getSource().getLayer() == layer).collect(Collectors.toList());
        slEdges.sort(Comparator.comparingDouble(sle->sle.getTarget().getX()));
        Collections.reverse(slEdges);
        for(ISugiyamaEdge sle : slEdges){
            assert(sle.getSource().getLayer() == sle.getTarget().getLayer());
            EdgePath path = sle.getPath();
            path.clear();
            DoublePoint sPoint = this.edgeToInOutPoints.get(sle.getID()).get(1);
            DoublePoint tPoint = this.edgeToInOutPoints.get(sle.getID()).get(0);
            double yKink = sPoint.y + index * this.distancePerEdgeInLayer[layer];
            path.addPoint(sPoint);
            path.addPoint(new DoublePoint(sPoint.x,yKink));
            path.addPoint(new DoublePoint(tPoint.x,yKink));
            path.addPoint(tPoint);
            index++;
        }
        return index;
    }

    //sums up the paths of supplement paths' supplement edges
    private void adjustSupplementPaths(){
        for(SupplementPath path : this.paths){
            EdgePath replacedEdgePath = path.getReplacedEdge().getPath();
            replacedEdgePath.clear();
            List<ISugiyamaEdge> sEdges =  path.getSupplementEdges();
            List<DoublePoint> points = new LinkedList<>();
            sEdges.forEach(edge->points.addAll(edge.getPath().getNodes()));	//puts together all edge paths
            points.forEach(replacedEdgePath::addPoint);
        }
    }

    /**
	 * Returns the points on top of a vertex-box where the incoming edges go into this vertex.
	 * Points are sorted from left to right.
	 */
	private List<DoublePoint> getInPoints(ISugiyamaVertex vertex){
		List<DoublePoint> points = new LinkedList<>();
		double width = vertex.getSize().x;
		double x = vertex.getX();
		double y = vertex.getY();
		int inDeg = this.inOutDeg.get(vertex.getID())[0];
		for(int i = 1; i <= inDeg; i++){
			points.add(new DoublePoint(x + (i /( inDeg + 1.0)) * width, y));
		}
		return points;
	}
	
	/**
	 * Returns the points on bottom of a vertex-box where the incoming edges go into this vertex.
	 * Points are sorted from left to right.
	 */
	private List<DoublePoint> getOutPoints(ISugiyamaVertex vertex){
		List<DoublePoint> points = new LinkedList<>();
		double width = vertex.getSize().x;
		double height = vertex.getSize().y;
		double x = vertex.getX();
		double y = vertex.getY();
		int outDeg = this.inOutDeg.get(vertex.getID())[1];
		for(int i = 1; i <= outDeg; i++){
			points.add(new DoublePoint(x + (i / (outDeg + 1.0)) * width, y + height));
		}
		return points;
	}
	
	private boolean dEquals(double a, double b){
		return Math.abs(a-b) < Math.pow(10, -6);
	}
	
	
	/**
	 * Reverses the EdgePath of this edge.
	 */
	private void reverseEdgePath(ISugiyamaEdge edge){
		Collections.reverse(edge.getPath().getNodes());
	}
	
	/**
	 * Initializes the graph this class works with and also sets some sets that access is often necessary. 
	 * Also initializes: 
	 */
	private void initialize(IEdgeDrawerGraph graph){
		this.graph = graph;
		this.paths = graph.getSupplementPaths();
		this.graphVertices = this.graph.getVertexSet();	//all graph vertices, with dummy vertices
		this.graphEdges = this.graph.getEdgeSet();	//all graph edges, with supplement edges

		this.selfLoopEdges = new HashSet<>();
		this.sameLayerEdges = new HashSet<>();
		Set<ISugiyamaEdge> sugiEdges = this.graphEdges.stream().filter(edge -> !edge.isSupplementEdge()).collect(Collectors.toSet()); //edges without supplement edges (also without their replaced edges)

		for(ISugiyamaEdge e : sugiEdges){
			if(e.getSource().getID().equals(e.getTarget().getID())){//selfloop //TODO: maybe get selfloops from graph.selfLoopsOf(vertex)
				this.selfLoopEdges.add(e);
			}
			if(e.getSource().getLayer() == e.getTarget().getLayer() && !e.getSource().getID().equals(e.getTarget().getID())){	//source and target on same layer, but no selfloops!
				this.sameLayerEdges.add(e);
			}
		}
		this.normalEdges = sugiEdges.stream().filter(edge->!this.selfLoopEdges.contains(edge)&&!this.sameLayerEdges.contains(edge)).collect(Collectors.toSet());//edge that is not supplement edge(because sugi edge is used), selfLoopEdge or sameLayerEdge
		
		this.spaceBetweenLayers = new double[graph.getLayerCount()];
		this.distancePerEdgeInLayer = new double[graph.getLayerCount()];
		this.layerYoffset = new double[graph.getLayerCount()][2];
		assert(sugiEdges.size() == selfLoopEdges.size() + sameLayerEdges.size() + normalEdges.size());
	}
}
