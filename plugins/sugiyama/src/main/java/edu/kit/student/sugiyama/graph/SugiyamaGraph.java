package edu.kit.student.sugiyama.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import edu.kit.student.graphmodel.DefaultGraphLayering;
import edu.kit.student.graphmodel.DefaultVertex;
import edu.kit.student.graphmodel.DirectedSupplementEdgePath;
import edu.kit.student.graphmodel.EdgeArrow;
import edu.kit.student.graphmodel.EdgePath;
import edu.kit.student.graphmodel.FastGraphAccessor;
import edu.kit.student.graphmodel.OrthogonalEdgePath;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.graphmodel.directed.DefaultDirectedGraph;
import edu.kit.student.graphmodel.directed.DirectedEdge;
import edu.kit.student.graphmodel.directed.DirectedGraph;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.util.DoublePoint;
import edu.kit.student.util.IdGenerator;
import edu.kit.student.util.IntegerPoint;
import edu.kit.student.util.Settings;
import javafx.scene.paint.Color;


/**
 * The SugiyamaGraph is a wrapper for a directed graph to enable easy and fast accessibility of
 * attributes and constructs needed during the computation of the hierarchical layout of a directed graph.
 * All vertices are assigned to a layer.
 * The positions of the vertices can be viewed as a grid (with varying widths per layer).
 */
public class SugiyamaGraph
		implements ICycleRemoverGraph,
		ILayerAssignerGraph,
		ICrossMinimizerGraph,
		IVertexPositionerGraph,
		IEdgeDrawerGraph
{

	private List<Integer> layerPositions;
	private DefaultDirectedGraph<ISugiyamaVertex, ISugiyamaEdge> graph;
	private Set<SupplementPath> supplementPaths = new HashSet<>();
	private DefaultGraphLayering<ISugiyamaVertex> layering;

	/**
	 * Constructs a new SugiyamaGraph and sets the Graph which is the underlying representation.
	 * To fulfill the invariant that all vertices are assigned to a layer, all vertices
	 * will be assigned to layer 0.
	 *
	 * @param graph the graph used as underlying representation.
	 */
	public SugiyamaGraph(DirectedGraph graph)  {

		Map<Integer, ISugiyamaVertex> tmpVertexMap = new HashMap<>();
		HashSet<ISugiyamaVertex> vertices = new HashSet<>();
		HashSet<ISugiyamaEdge> edges = new HashSet<>();
		layering = new DefaultGraphLayering<>(new HashSet<>());
		for (Vertex vertex: graph.getVertexSet()) {
			SugiyamaVertex sugiyamaVertex = new SugiyamaVertex(vertex);
			vertices.add(sugiyamaVertex);	//fills vertexset with all wrapped vertices
			tmpVertexMap.put(vertex.getID(), sugiyamaVertex);
		}
		for(DirectedEdge edge: graph.getEdgeSet()){
			SugiyamaEdge sugiyamaEdge = new SugiyamaEdge(edge,
					tmpVertexMap.get(edge.getSource().getID()),
					tmpVertexMap.get(edge.getTarget().getID()));
			edges.add(sugiyamaEdge);	//fills edgeset with all wrapped edges
		}
		this.graph = new DefaultDirectedGraph<>(vertices, edges);
		layerPositions = new LinkedList<>();
		layerPositions.add(0);
	}

	/**
	 * Builds a SugiyamaGraph, consisting of vertices, edges and Supplementpaths, like SupplementPaths in SugiyamaGraph.
	 * The Set of paths is optional, it can be null or empty. 
	 * If paths is not empty the constructed SugiyamaGraph will be used just for EdgeDrawing. 
	 * 
	 * @param name name of this SugiyamaGraph
	 * @param vertices vertices to be contained in this SugiyamaGraph
	 * @param edges edges to be contained in this SugiyamaGraph
	 * @param paths Optional, can be empty or null. The paths replace a certain edge which is then not contained in the edgeset. 
	 * Just use these if you want to redraw given edges and vertices. not recommended for use in the whole sugiyama-steps.
	 */
	public SugiyamaGraph(String name, Set<Vertex> vertices, Set<DirectedEdge> edges, Set<DirectedSupplementEdgePath> paths) {
		
		HashSet<ISugiyamaVertex> sugyVertices = new HashSet<>();	//only SugiyamaVertices
		HashSet<ISugiyamaEdge> sugyEdges = new HashSet<>();	//only SugiyamaEdges
		Map<Integer, ISugiyamaVertex> tmpVertexMap = new HashMap<>();
		List<Double> yValsOfVertices = new ArrayList<>();
		layering = new DefaultGraphLayering<>(new HashSet<>());
		layerPositions = new LinkedList<>();
		layerPositions.add(0);
		
		//add here points for later layer assigning
		for (Vertex vertex: vertices){	//add every different position to list, to assign layers later
			if(!yValsOfVertices.contains(vertex.getY())){
				yValsOfVertices.add(vertex.getY());	
			}
		}
		yValsOfVertices.sort(Double::compare);	//sorts y vals in ascending order
		//assign every vertex a layer, vertices on first or last layer are normal SugiyamaVertex though they have a negative id
		//dummies will be assigned at the path building
		for(Vertex v : vertices){
			for(int i = 0; i < yValsOfVertices.size(); i++){
				if(yValsOfVertices.get(i).equals(v.getY())){	//assign here the layer
					if(i == 0 || i == yValsOfVertices.size() - 1 || v.getID() >= 0){//first or last layer, or normal vertex -> SugiyamaVertex
						SugiyamaVertex sugiyamaVertex = new SugiyamaVertex(v);
						sugyVertices.add(sugiyamaVertex);	//fills vertexset with all wrapped vertices
						tmpVertexMap.put(v.getID(), sugiyamaVertex);
						this.assignToLayer(sugiyamaVertex, i);
						break;
					}
				}
			}
		}
		
		//only wrap edges that are normal edges (from or to last layer with layer diff ==1 or between two normal vertices with layer diff == 1)
		//also yet turn the edge if it has the wrong direction
		for(DirectedEdge edge: edges){	//given edges, wrap them to SugiyamaEdges
//			System.out.println("edges: source y: "+edge.getSource().getY()+", id: "+edge.getSource().getID()+ ", layer: "+ tmpVertexMap.get(edge.getSource().getID()).getLayer() +", target: "+edge.getTarget().getY()+", id: "+edge.getTarget().getID()+", layer: "+tmpVertexMap.get(edge.getTarget().getID()).getLayer());
			Vertex source = edge.getSource();
			Vertex target = edge.getTarget();
			if(tmpVertexMap.containsKey(source.getID()) && tmpVertexMap.containsKey(target.getID())
					&& Math.abs(tmpVertexMap.get(source.getID()).getLayer() - tmpVertexMap.get(target.getID()).getLayer()) == 1){
				SugiyamaEdge sugiyamaEdge = new SugiyamaEdge(edge,
						tmpVertexMap.get(edge.getSource().getID()),
						tmpVertexMap.get(edge.getTarget().getID()));
				sugyEdges.add(sugiyamaEdge);
				if(sugiyamaEdge.getSource().getLayer() > sugiyamaEdge.getTarget().getLayer()){
					sugiyamaEdge.reverse();
				}
			}
		}
		//created dummies, supplementEdges and supplementPaths are added to the graph in the method which is creating them
		this.graph = new DefaultDirectedGraph<>(sugyVertices, sugyEdges);

		//now iterate over all paths and construct the correct SupplementPath, also construct:
		//representingVertex->sugyVertex, assign it a layer!,  dummies->dummyVertex(also add them to sugyVertices set), 
		//supplementEdges->SupplementEdge(also add them to sugyEdges set)
		for(DirectedSupplementEdgePath p : paths){//TODO: add dummies and supp edges and watch out for direction!
			DirectedEdge replaced = p.getReplacedEdge();
			List<Vertex> pathDummies = p.getDummyVertices();
			ISugiyamaEdge tempReplacedEdge = new SugiyamaEdge(replaced, tmpVertexMap.get(replaced.getSource().getID()), tmpVertexMap.get(replaced.getTarget().getID()));

			if(tempReplacedEdge.getSource().getLayer() > tempReplacedEdge.getTarget().getLayer()){
				tempReplacedEdge.reverse();
				Collections.reverse(pathDummies);// reverse direction of dummies 
			}
			List<ISugiyamaVertex> tempDummies = new LinkedList<>();
			int layer = tempReplacedEdge.getSource().getLayer() + 1;
			for(Vertex v : pathDummies){
				ISugiyamaVertex tempDummy = new DummyVertex("", "", layer, v.getSize(), v.getID() );
				tempDummy.setX(v.getX());
				tempDummy.setY(v.getY());
				this.assignToLayer(tempDummy, layer);
				tempDummies.add(tempDummy);
				layer++;
			}
			this.graph.addAllVertices(new HashSet<>(tempDummies));
			assert(layer == tempReplacedEdge.getTarget().getLayer());	//to ensure correct layer numbers for source, target and dummies!
			List<ISugiyamaEdge> tempSupplementEdges = new LinkedList<>();
			//TODO: turn the replaced edge if necessary and turn also dummies, watch out to set their layers correctly
//			int upOrDown = tempReplacedEdge.getSource().getLayer() > tempReplacedEdge.getTarget().getLayer() ? -1 : 1;
			//TODO: differ between turned and not turned replaced edges of the supplement path!!!
			SupplementEdge newSuppE = new SupplementEdge("","", tempReplacedEdge.getSource(), tempDummies.get(0));
			tempSupplementEdges.add(newSuppE);
			for(int i = 0; i < tempDummies.size() - 1; i++){
				newSuppE = new SupplementEdge("","", tempDummies.get(i), tempDummies.get(i+1));
				tempSupplementEdges.add(newSuppE);
			}
			newSuppE = new SupplementEdge("","", tempDummies.get(tempDummies.size() - 1), tempReplacedEdge.getTarget());
			tempSupplementEdges.add(newSuppE);
			
			this.graph.addAllEdges(new HashSet<>(tempSupplementEdges));
			SupplementPath tempSupplementPath = this.createSupplementPath(tempReplacedEdge, tempDummies, tempSupplementEdges);
			this.supplementPaths.add(tempSupplementPath);
//			sugyVertices.addAll(tempDummies);	//add dummies to normal vertex set
//			sugyEdges.addAll(tempSupplementEdges);	//add SupplementEdges to normal edge set
//			checkSupplementPath(tempSupplementPath);
		}
	}
	
//	private void checkSupplementPath(SupplementPath p){
//		ISugiyamaEdge replaced = p.getReplacedEdge();
//		List<ISugiyamaVertex> vertices = p.getDummyVertices();
//		List<ISugiyamaEdge> edges = p.getSupplementEdges();
//		System.out.println("replaced: source: y: "+replaced.getSource().getY()+", id: "+replaced.getSource().getID()+ ", layer: "+ replaced.getSource().getLayer() +", target: "+replaced.getTarget().getY()+", id: "+replaced.getTarget().getID()+", layer: "+replaced.getTarget().getLayer());
//		for(ISugiyamaVertex v : vertices){
//			System.out.println("vertices y: "+v.getY()+", id: "+v.getID()+ ", layer: "+ v.getLayer());
//		}
//		for(ISugiyamaEdge e : edges){
//			System.out.println("edges: source: y: "+e.getSource().getY()+", id: "+e.getSource().getID()+ ", layer: "+ e.getSource().getLayer() +"->; target: "+e.getTarget().getY()+", id: "+e.getTarget().getID()+", layer: "+e.getTarget().getLayer());
//		}
//	}
	

//	/**
//	 * Replaces the specified edge with a path of dummy vertices of the specified length.
//	 * Replaced edges are removed from the set of edges but saved for later retrieval
//	 * with {@code getReplacedEdges()} or restored with {@code restoreReplacedEdges}.
//	 *
//	 * @param edge the edge to be replaced
//	 * @param length the length of the path which replaces the edge
//	 */
//	private void replaceWithSupplementPath(ISugiyamaEdge edge, int length) {
//		//TODO implement ? or unnecessary ?! edges are yet replaced with createSupplementpath()
//	}

	public Set<SupplementPath> getSupplementPaths() {
		return supplementPaths;
	}

	@Override
	public int getLayerCount() {
		return layering.getLayerCount();
	}

	@Override
	public int getVertexCount(int layerNum) {
		return layering.getVertexCount(layerNum);
	}

	@Override
	public void insertLayers(int position, int numberOfLayers) {
		this.layering.insertLayers(position, numberOfLayers);
	}

	@Override
	public void reverseEdge(ISugiyamaEdge edge) {
		graph.removeEdge(edge);
		edge.reverse();
		graph.addEdge(edge);
	}

	@Override
	public boolean isReversed(ISugiyamaEdge edge) {
		return edge.isReversed();
	}

	@Override
	public void swapVertices(ISugiyamaVertex first, ISugiyamaVertex second) {
		assert (this.getLayer(first)==this.getLayer(second)); //both vertices have to be on the same layer!

		IntegerPoint tmp = layering.getPosition(first);
		layering.setPosition(first, layering.getPosition(second));
		layering.setPosition(second, tmp);
	}

	@Override
	public int getLayer(ISugiyamaVertex vertex) {
		return vertex.getLayer();
	}

	private int getLayerById(int vertexID) {
		ISugiyamaVertex v = this.getVertexByID(vertexID);
		if (v == null) {
			return -1;
		}
		return v.getLayer();
	}

	@Override
	public ISugiyamaVertex getVertexByID(int vertexID) {
		for (ISugiyamaVertex vertex : this.getVertexSet()) {
			if (vertex.getID() == vertexID) {
				return vertex;
			}
		}
		return null;
	}

	@Override
	public void cleanUpEmtpyLayers() {
		this.layering.cleanUpEmptyLayers();
	}

	public List<ISugiyamaVertex> getSortedLayer(int layerIndex){
		return layering.getSortedLayer(layerIndex);
	}

	@Override
	public List<ISugiyamaVertex> getLayer(int layerNum) {
		return layering.getLayer(layerNum);
	}

	public List<List<ISugiyamaVertex>> getSortedLayers(){
		return layering.getSortedLayers();
	}
	
	@Override
	public List<List<ISugiyamaVertex>> getLayers() {
		return layering.getLayers();
	}

	@Override
	public DummyVertex createDummy(String name, String label, int layer) {
        DummyVertex dummyVertex = new DummyVertex(name, label, layer);
        this.addVertex(dummyVertex);
        return dummyVertex;
    }

	@Override
	public SupplementEdge createSupplementEdge(String name, String label, ISugiyamaVertex source, ISugiyamaVertex target) {
		SupplementEdge supplementEdge = new SupplementEdge(name, label, source, target);
        this.addEdge(supplementEdge);
		return supplementEdge;
	}

	public SupplementPath createSupplementPath(ISugiyamaEdge replacedEdge, List<ISugiyamaVertex> dummies, List<ISugiyamaEdge> supplementEdges) {
		SupplementPath supplementPath = new SupplementPath(replacedEdge, dummies, supplementEdges);
		this.supplementPaths.add(supplementPath);
		this.removeEdge(replacedEdge);
		return supplementPath;
	}

	private void removeEdge(ISugiyamaEdge edge) {
		graph.removeEdge(edge);
	}

	private void addEdge(ISugiyamaEdge edge) {
		graph.addEdge(edge);
	}

	private void addVertex(ISugiyamaVertex vertex) {
		graph.addVertex(vertex);
	}

	@Override
	public void setLayerY(int layerNum, int y) {
		if (layerNum >= layerPositions.size()) {
			throw new IndexOutOfBoundsException();
		}
		layerPositions.set(layerNum, y);
	}

	@Override
	public void setX(ISugiyamaVertex vertex, double x) {
		vertex.setX(x);

	}

	@Override
	public void assignToLayer(ISugiyamaVertex vertex, int layerNum) {
		this.layering.setLayer(vertex, layerNum);
	}

	@Override
	public Set<ISugiyamaEdge> getReversedEdges() {
		Set<ISugiyamaEdge> result = new HashSet<>();
		for(ISugiyamaEdge edge : this.getEdgeSet()){
			if(isReversed(edge)){
				result.add(edge);
			}
		}
		return result;
	}

	@Override
	public int getHeight() {
		return layering.getHeight();
	}

	@Override
	public int getMaxWidth() {
		return layering.getMaxWidth();
	}

	@Override
	public int getLayerWidth(int layerN) {
		return layering.getLayerWidth(layerN);
	}

	@Override
	public int getLayerFromVertex(Vertex vertex) {
		return getLayerById(vertex.getID());
	}

	public IntegerPoint getPosition(ISugiyamaVertex vertex) {
		return layering.getPosition(vertex);
	}

	@Override
	public String toString(){
		String out = "Vertices: {";
		for(ISugiyamaVertex v : this.getVertexSet()){
			out+= v.getName() + ", ";
		}
		out = out.substring(0, out.length()-2);
		out+= "}";
		out+= '\n';
		out+= "Edges:{";
		out+= '\n';
		for(ISugiyamaEdge e : this.getEdgeSet()){
			out+= e.getName() + "[" + e.getSource().getName() +"->"+ e.getTarget().getName()+"] ";
			if(this.isReversed(e)){
				out+="reversed";
			}
			out+=",";
			out+= '\n';
		}
		out=out.substring(0, out.length()-2);
		return out + "}";
	}
	
	@SuppressWarnings("unused")
	private boolean dEquals(double a, double b){
		return Math.abs(a-b) < Math.pow(10, -6);
	}

	/**
	 * A supplement path for connecting vertices, which are more than one layer apart.
	 * They are stored in the ISugiyamaEdge along with the substituted edge.
	 */
	public static class SupplementPath {
		private final ISugiyamaEdge replacedEdge;
		private final List<ISugiyamaVertex> dummies;
		private final List<ISugiyamaEdge> supplementEdges;

		SupplementPath(ISugiyamaEdge replacedEdge, List<ISugiyamaVertex> dummies, List<ISugiyamaEdge> supplementEdges) {
			this.replacedEdge = replacedEdge;
			this.dummies = dummies;
			this.supplementEdges = supplementEdges;
		}

		/**
		 * Returns the number of vertices including source and target.
		 * @return the length of the path
		 */
		public int getLength() {
			return dummies.size() + 2;
		}

		/**
		 * Returns the list of vertices on the path sorted from source to target excluding the source and target.
		 * @return the list of vertices
		 */
		public List<ISugiyamaVertex> getDummyVertices() {
			return this.dummies;
		}

		/**
		 * Returns the list of supplement edges in the path sorted form source to target.
		 * @return the list of supplement edges
		 */
		public List<ISugiyamaEdge> getSupplementEdges(){
			return this.supplementEdges;
		}

		/**
		 * Returns the edge which is substituted by this path
		 * @return the replaced edge
		 */
		public ISugiyamaEdge getReplacedEdge() {
			return this.replacedEdge;
		}

		/**
		 * Reverses a supplement path by reversing the replaced edge and reversing the order of the dummy vertices on this path.
		 */
		public void reverse(){	//TODO: test if correct or not!!
			Collections.reverse(this.dummies);
			this.supplementEdges.forEach(ISugiyamaEdge::reverse);
			Collections.reverse(this.supplementEdges);
			this.replacedEdge.reverse();
		}

        @Override
        public int hashCode() {
            return getReplacedEdge().hashCode();
        }

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SupplementPath that = (SupplementPath) o;

			if (replacedEdge != null ? !replacedEdge.equals(that.replacedEdge) : that.replacedEdge != null)
				return false;
			if (dummies != null ? !dummies.equals(that.dummies) : that.dummies != null) return false;
			return supplementEdges != null ? supplementEdges.equals(that.supplementEdges) : that.supplementEdges == null;
		}
	}
	/**
	 * A supplement edge which is part of a {@link SupplementPath}.
	 */
	public class SupplementEdge implements ISugiyamaEdge {

		private String name;
		private String label;
		private ISugiyamaVertex source;
		private ISugiyamaVertex target;
		private boolean isReversed;
		private EdgePath path;
		private Integer id;

		SupplementEdge(String name, String label) {
			this.name=name;
			this.label=label;
			this.path = new OrthogonalEdgePath();
			this.id = IdGenerator.getInstance().createId();
		}

		SupplementEdge(String name, String label, ISugiyamaVertex source, ISugiyamaVertex target) {
			this(name,label);
			this.source=source;
			this.target=target;
			this.isReversed=false;
		}

		// a SupplementEdge is a SupplementEdge
		public boolean isSupplementEdge(){
			return true;
		}

		@Override
		public void setVertices(ISugiyamaVertex source, ISugiyamaVertex target) {
			this.source=source;
			this.target=target;
		}

		@Override
		public DirectedEdge getWrappedEdge() {
			return null;
		}

		@Override
		public ISugiyamaVertex getSource() {
			return this.source;
		}

		@Override
		public ISugiyamaVertex getTarget() {
			return this.target;
		}

		@Override
		public List<ISugiyamaVertex> getVertices() {
			List<ISugiyamaVertex> list = new LinkedList<>();
			list.add(this.source);
			list.add(this.target);
			return list;
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
		public void addToFastGraphAccessor(FastGraphAccessor fga) {
			// TODO implement!!!

		}

		@Override
		public EdgePath getPath() {
			return this.path;
		}

		/**
		 * Reverses this edge.
		 */
		public void reverse() {
			ISugiyamaVertex tmp = source;
			source = target;
			target = tmp;
			this.isReversed = !this.isReversed;
		}

		@Override
		public boolean isReversed() {
			return this.isReversed;
		}

		@Override
		public List<GAnsProperty<?>> getProperties() {
			return new LinkedList<>();
		}

		@Override
		public String toString(){
			return this.name + "[S" + this.getID() + "](" + this.source.toString() + "->" + this.target.toString() + ")";
		}

		@Override
		public Color getColor() {
			// TODO necessary ?
			return null;
		}

		@Override
		public EdgeArrow getArrowHead() {
			// TODO necessary?
			return null;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}

	/**
	 * A supplement vertex which is part of a {@link SupplementPath}.
	 */
	public class DummyVertex implements ISugiyamaVertex {

		private String name;
		private String label;
		private final boolean custom;
		private DoublePoint size;
		private Integer id;
		private double x;
		private double y;

        DummyVertex(String name, String label, int layer) {
			//super(name, label);
            this.name = name;
            this.label = label;
            this.id = IdGenerator.getInstance().createId();
			layering.addVertex(this, layer);
			custom = false;
		}
		
		//custom dummy if size and id were set before creating a dummy
		DummyVertex(String name, String label, int layer, DoublePoint size, Integer id){
			//super(name, label);
			this.name = name;
			this.label = label;
			this.size = size;
			this.id = id;
			layering.addVertex(this, layer);
			custom = true;
		}

		@Override
		public String getName() {
			return this.name;
		}

		public Integer getID(){
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

		public DoublePoint getSize(){
			return custom ? this.size : Settings.getSize(this.label,true);
		}

		@Override
		public void setLeftRightMargin(IntegerPoint newMargin) {
            //not needed here
		}

		@Override
		public IntegerPoint getLeftRightMargin() {
			return new IntegerPoint(2,2);
		}

		@Override
		public Color getColor() {
			return null;    //don't need a color for these vertices
		}

		public boolean isDummy() {
			return true;
		}

		@Override
		public Vertex getVertex() {
			return null;
		}

		@Override
		public int getLayer() {
			return layering.getLayerFromVertex(this);
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

		}

		@Override
		public List<GAnsProperty<?>> getProperties() {
			return null;    //a dummy vertex has no GAnsProperties
		}

		@Override
		public void setLayer(int layerNum) {
			layering.setLayer(this, layerNum);
		}
		
		@Override
		public boolean equals(Object o){
		    if (o==null) {
		         return false;
		    }
			if (this==o) {
		         return true;
		    }
		    if(!(o instanceof ISugiyamaVertex)){
		    	return false;
		    }

		    return Objects.equals(((ISugiyamaVertex) o).getID(), this.getID());
		}

		@Override
		public String toString(){
			return this.getName() + "[D" + this.getID() + "]";
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}

	/**
	 * A wrapper class for vertices used in the sugiyama framework.
	 * A ISugiyamaVertex can be a {@link DefaultVertex} or a {@link DummyVertex}
	 */
	public class SugiyamaVertex implements ISugiyamaVertex {
		private final Vertex vertex;

		SugiyamaVertex(Vertex vertex) {
			this.vertex = vertex;
			layering.addVertex(this, 0);
		}

		public boolean isDummy() {
			return false;
		}

		public Vertex getVertex() {
			return vertex;
		}

		public int getLayer() {
			return layering.getLayerFromVertex(this);
		}

		public void setLayer(int layer) {
			layering.setLayer(this, layer);
		}

		@Override
		public String getName() {
			return this.vertex.getName();
		}

		@Override
		public Integer getID() {
			return this.vertex.getID();
		}

		@Override
		public String getLabel() {
			return this.vertex.getLabel();
		}

		@Override
		public double getX() {
			return this.vertex.getX();
		}

		@Override
		public double getY() {
			return this.vertex.getY();
		}

		@Override
		public void setX(double x) {
			this.vertex.setX(x);

		}

		@Override
		public void setY(double y) {
			this.vertex.setY(y);

		}

		@Override
		public void addToFastGraphAccessor(FastGraphAccessor fga) {
			this.vertex.addToFastGraphAccessor(fga);

		}

		@Override
		public List<GAnsProperty<?>> getProperties() {
			return this.vertex.getProperties();
		}

		@Override
		public String toString(){
			return this.getName() + "[" + this.getID() + "]";
		}

		@Override
		public DoublePoint getSize() {
			return this.vertex.getSize();
		}
		
		@Override
		public void setLeftRightMargin(IntegerPoint newMargin) {
			this.vertex.setLeftRightMargin(newMargin);
		}
		
		@Override
		public IntegerPoint getLeftRightMargin() {
			return this.vertex.getLeftRightMargin();
		}

		@Override
		public Color getColor() {
			return this.vertex.getColor();
		}
		
		@Override
		public boolean equals(Object o){
		    if (o==null) {
		         return false;
		    }
			if (this==o) {
		         return true;
		    }
		    if(!(o instanceof ISugiyamaVertex)){
		    	return false;
		    }
		    
		    return Objects.equals(((ISugiyamaVertex) o).getID(), this.getID());
		}

        @Override
        public int hashCode() {
            return getID().hashCode();
        }

	}

	/**
	 * A wrapper class for directed edges to implement additional functionality
	 * to apply the sugiyama layout to the SugiyamaGraph containing them.
	 */
	public class SugiyamaEdge implements ISugiyamaEdge {
		List<Vector<Integer>> corners;
		private boolean isReversed;

		// TODO: is this really necessary?
//		private boolean isSupplement;
		private DirectedEdge wrappedEdge;
		private ISugiyamaVertex source;
		private ISugiyamaVertex target;

		private SugiyamaEdge(DirectedEdge edge, ISugiyamaVertex source, ISugiyamaVertex target) {
			this.wrappedEdge = edge;
			this.isReversed = false;
			this.source = source;
			this.target = target;
		}

		//private E getEdge() { return null; }
		/**
		 * Returns true, if this edge has been reversed in order to break cycles in the first step of sugiyama, false otherwise.
		 * @return
		 * 		true if this edge is reversed, false otherwise
		 */
		public boolean isReversed() {
			return this.isReversed;
		}

		/**
		 * Reverses this edge.
		 */
		public void reverse() {
			ISugiyamaVertex tmp = source;
			source = target;
			target = tmp;
			this.isReversed=!this.isReversed;
		}

		//a SugiyamaEdge is not a SupplementEdge
		public boolean isSupplementEdge(){
			return false;
		}
//		/**
//		 * Returns true, if this edge was replaced by a {@link SupplementPath} that contains source and target vertices and at least one dummy vertex.
//		 * @return
//		 * 		True if this edge is an supplement path, false otherwise
//		 */
//		private boolean isReplaced() {
//			return false;
//		}
//
//		/**
//		 * Returns the {@link SupplementPath} which this {@link ISugiyamaEdge} represents.
//		 * @return
//		 */
//		private SupplementPath getSupplementPath() {
//			if(this.isSupplement){
//				return this.supplementpath;
//			}
////			SupplementPath supplement = new SupplementPath();//TODO implement, if necessary ?
////			return supplement;
//		    return null;
//		}
//
//		private boolean isSupplementPath(){
//			return this.isSupplement;
//		}
//
//		private void setSupplementPath(int length){
//			//this.supplementpath=new SupplementPath(this,length);	//TODO check if it would be wiser to add a constructor to SupplementPath to
//			//SupplementPath(String, String, ISugiyamaEdge edge, int length)
//		}

		@Override
		public String getName() {
			return wrappedEdge.getName();
		}

		@Override
		public Integer getID() {
			return wrappedEdge.getID();
		}

		@Override
		public String getLabel() {
			return wrappedEdge.getLabel();
		}

		@Override
		public void addToFastGraphAccessor(FastGraphAccessor fga) {
			wrappedEdge.addToFastGraphAccessor(fga);
		}

		@Override
		public ISugiyamaVertex getSource() {
			return this.source;
		}

		@Override
		public ISugiyamaVertex getTarget() {
			return this.target;
		}

		@Override
		public List<ISugiyamaVertex> getVertices() {
			List<ISugiyamaVertex> vertices = new LinkedList<>();
			vertices.add(source);
			vertices.add(target);
			return vertices;
		}

		@Override
		public OrthogonalEdgePath getPath() {
			return (OrthogonalEdgePath) wrappedEdge.getPath();
			//TODO: really not good !!!!!
		}

		@Override
		public List<GAnsProperty<?>> getProperties() {
			return wrappedEdge.getProperties();
		}

		@Override
		public void setVertices(ISugiyamaVertex source, ISugiyamaVertex nv) {
			this.source = source;
			this.target = nv;
		}

		@Override
		public String toString(){
			return this.getName() + "[" + this.getID() + "](" + this.source.toString() + "->" + this.target.toString() + ")";
		}

		@Override
		public Color getColor() {
			return this.wrappedEdge.getColor();
		}

		@Override
		public EdgeArrow getArrowHead() {
			return this.wrappedEdge.getArrowHead();
		}

		@Override
		public DirectedEdge getWrappedEdge() {
			return wrappedEdge;
		}

        @Override
        public int hashCode() {
            return this.getID().hashCode();
        }
	}

	@Override
	public Integer outdegreeOf(Vertex vertex) {
		return graph.outdegreeOf(vertex);
	}

	@Override
	public Integer indegreeOf(Vertex vertex) {
		return graph.indegreeOf(vertex);
	}

	@Override
	public Integer selfLoopNumberOf(Vertex vertex) {
		return graph.selfLoopNumberOf(vertex);
	}

	@Override
	public FastGraphAccessor getFastGraphAccessor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addToFastGraphAccessor(FastGraphAccessor fga) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<ISugiyamaEdge> edgesOf(Vertex vertex) {
		return graph.edgesOf(vertex);
	}

	@Override
	public Set<ISugiyamaEdge> outgoingEdgesOf(Vertex vertex) {
		return graph.outgoingEdgesOf(vertex);
	}

	@Override
	public Set<ISugiyamaEdge> incomingEdgesOf(Vertex vertex) {
		return graph.incomingEdgesOf(vertex);
	}

	@Override
	public Set<ISugiyamaEdge> selfLoopsOf(Vertex vertex) {
		return graph.selfLoopsOf(vertex);
	}

	@Override
	public Set<ISugiyamaVertex> getVertexSet() {
		return graph.getVertexSet();
	}

	@Override
	public Set<ISugiyamaEdge> getEdgeSet() {
		return graph.getEdgeSet();
	}

	@Override
	public void setPositionsOnLayer(int layer, List<ISugiyamaVertex> newLayer) {
		List<Integer> newOrder = new LinkedList<>();
		newLayer.forEach(v->newOrder.add(this.layering.getPosition(v).x));
		this.layering.swapVertices(layer, newOrder);

		//int x = 0;
		//for (ISugiyamaVertex vertex : newLayer) {
		//	if (!(this.layering.getLayerFromVertex(vertex) == layer)) {
		//		throw new IllegalArgumentException("All vertices have to be on the specified layer, when calling setPositionOnLayer");
		//	}
		//	this.layering.setPosition(vertex, new IntegerPoint(x, layer));
		//	x++;
		//}
	}
}