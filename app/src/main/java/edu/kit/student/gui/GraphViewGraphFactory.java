package edu.kit.student.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.student.graphmodel.Edge;
import edu.kit.student.graphmodel.Graph;
import edu.kit.student.graphmodel.SubGraph;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.graphmodel.ViewableGraph;
import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.graphmodel.ViewableVertex.VertexPriority;
import edu.kit.student.graphmodel.serialize.SerializedEdge;
import edu.kit.student.graphmodel.serialize.SerializedGraph;
import edu.kit.student.graphmodel.serialize.SerializedVertex;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.util.DoublePoint;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 * The GraphViewGraphFactory generates the visual representation of a given
 * {@link Graph} and gives access to the set {@link Graph}.
 * 
 * @author Nicolas
 */
public class GraphViewGraphFactory {

	// lenght of a hex color code
	private static int colorLength = 7;
	
	private ViewableGraph graph;
	private Map<VertexShape, ViewableVertex> vertices;
	private Map<EdgeShape, Edge> edges;
	private List<BackgroundShape> background;

	/**
	 * Constructor. Sets the graph and generates the vertices and edges for
	 * visualization.
	 * 
	 * @param graph
	 *            The graph data that will be shown.
	 */
	public GraphViewGraphFactory(ViewableGraph graph) {
		vertices = new HashMap<>();
		edges = new HashMap<EdgeShape, Edge>();
		background = new LinkedList<BackgroundShape>();
		this.graph = graph;
		
		createVertices();
		createEdges();
	}
	
	public ViewableGraph getGraph() {
		return this.graph;
	}

	/**
	 * Returns all graphical elements that have been generated by the factory.
	 * 
	 * @return All graphical elements generated by the factory.
	 */
	public List<GAnsGraphElement> getGraphicalElements() {
		List<GAnsGraphElement> elements = new LinkedList<GAnsGraphElement>();
		elements.addAll(background);
		elements.addAll(vertices.keySet());
		elements.addAll(edges.keySet());
		
		return elements;
	}
	
	public Set<VertexShape> getVertexShapes() {
		return vertices.keySet();
	}

	/**
	 * Returns the vertex element from the graph model that is being represented
	 * by the shape. Can be null if an {@link EdgeShape} is passed.
	 * 
	 * @param shape
	 *            The shape that represents the vertex.
	 * @return The Vertex being represented by the passed shape.
	 */
	public ViewableVertex getVertexFromShape(GAnsGraphElement shape) {
		return vertices.get(shape);
	}

	/**
	 * Returns the edge element from the graph model that is being represented
	 * by the shape. Can be null if an {@link VertexShape} is passed.
	 * 
	 * @param shape
	 *            The shape that represents the edge.
	 * @return The Edge being represented by the passed shape.
	 */
	public Edge getEdgeFromShape(GAnsGraphElement shape) {
		return edges.get(shape);
	}
	
	/**
	 * Calculates and returns the size of a vertex with the given text.
	 * 
	 * @param text The text which size the vertex depends on.
	 * @return A Pair of width and height of the vertex.
	 */
	public static DoublePoint getSizeOfVertex(String text) {
		VertexShape shape = new VertexShape();
		shape.setText(text);
		DoublePoint pair = new DoublePoint(shape.getWidth(), shape.getHeight());
		return pair;
	}
	
	private void createVertices() {
		Set<? extends ViewableVertex> set = graph.getVertexSet();
		for(ViewableVertex vertex : set) {
			if(vertex.getPriority() == VertexPriority.HIGH) {
				VertexShape shape = new VertexShape(vertex);
				vertices.put(shape, vertex);
			} else if(vertex.getPriority() == VertexPriority.LOW) {
				BackgroundShape shape = new BackgroundShape(vertex);
				background.add(shape);
			} else { // unknown priorities are interpreted as high.
				VertexShape shape = new VertexShape(vertex);
				vertices.put(shape, vertex);
			}
		}
		for(SubGraph subgraph : graph.getSubGraphs()) {
		    BackgroundShape shape = new BackgroundShape(subgraph);
		    background.add(shape);
		}
	}
	
	private void createEdges() {
		Set<? extends Edge> set = graph.getEdgeSet();
		for(Edge edge : set) {
			EdgeShape shape = new EdgeShape(edge);
			edges.put(shape, edge);
		}
	}
	
	public SerializedGraph serializeGraph() {
		return new SerializedGraph(new HashMap<String, String>(), new HashMap<String, String>(),
				serializeVertices(), serializeEdges());
	}
	
	private Set<SerializedVertex> serializeVertices() {
		Set<SerializedVertex> set = new LinkedHashSet<SerializedVertex>();
		
	    //add background shapes as vertices
        for(BackgroundShape shape : background) {
            Map<String,String> shapeProperties = new HashMap<String,String>();
            shapeProperties.put("label", shape.getText());
            shapeProperties.put("color", GraphViewGraphFactory.toRGBCode(shape.getColor()));
            
            Bounds bounds = shape.getBoundsInParent();
            shapeProperties.put("minX", Double.toString(bounds.getMinX()));
            shapeProperties.put("minY", Double.toString(bounds.getMinY()));
            shapeProperties.put("maxX", Double.toString(bounds.getMaxX()));
            shapeProperties.put("maxY", Double.toString(bounds.getMaxY()));
            shapeProperties.put("arcWidth", Double.toString(shape.getElementShape().getArcWidth()));
            shapeProperties.put("arcHeight", Double.toString(shape.getElementShape().getArcHeight()));
            
            Map<String,String> metaProperties = new HashMap<String,String>();
            SerializedVertex serialized = new SerializedVertex(shapeProperties, metaProperties);
            set.add(serialized);
        }
		
		for(VertexShape shape : vertices.keySet()) {
			Map<String,String> shapeProperties = new HashMap<String,String>();
			shapeProperties.put("label", shape.getText());
			shapeProperties.put("color", GraphViewGraphFactory.toRGBCode(shape.getColor()));
			
			int begin = shape.getVertexStyle().indexOf('#');
			String borderColor = "";
			if(begin != -1) {
				borderColor = shape.getVertexStyle().substring(begin, begin + colorLength);
			}
			shapeProperties.put("border-color", borderColor);
			
			Bounds bounds = shape.getBoundsInParent();
			shapeProperties.put("minX", Double.toString(bounds.getMinX()));
			shapeProperties.put("minY", Double.toString(bounds.getMinY()));
			shapeProperties.put("maxX", Double.toString(bounds.getMaxX()));
			shapeProperties.put("maxY", Double.toString(bounds.getMaxY()));
			shapeProperties.put("arcWidth", Double.toString(shape.getElementShape().getArcWidth()));
			shapeProperties.put("arcHeight", Double.toString(shape.getElementShape().getArcHeight()));
			
			Map<String,String> metaProperties = new HashMap<String,String>();
			Vertex vertex = vertices.get(shape);
			for(GAnsProperty<?> property : vertex.getProperties()) {
				metaProperties.put(property.getName(), property.getValueAsString());
			}
			SerializedVertex serialized = new SerializedVertex(shapeProperties, metaProperties);
			set.add(serialized);
		}		
		
	    return set;
	}
	
	private Set<SerializedEdge> serializeEdges() {
		Set<SerializedEdge> set = new HashSet<SerializedEdge>();
		for(EdgeShape shape : edges.keySet()) {
			Map<String,String> shapeProperties = new HashMap<String,String>();
			Path path = shape.getElementShape();
			shapeProperties.put("label", shape.getText());
			// fragile, hashCode() is impl. dependent, could change in later releases
			shapeProperties.put("color", GraphViewGraphFactory.toRGBCode(shape.getColor()));
			
			for(int i = 0; i < path.getElements().size(); i++) {
				PathElement element = path.getElements().get(i);
				if(MoveTo.class.equals(element.getClass())) {
					MoveTo move = (MoveTo)element;
					shapeProperties.put(i + "x", Double.toString(move.getX()));
					shapeProperties.put(i + "y", Double.toString(move.getY()));
				} else if(LineTo.class.equals(element.getClass())) {
					LineTo line = (LineTo)element;
					shapeProperties.put(i + "x", Double.toString(line.getX()));
					shapeProperties.put(i + "y", Double.toString(line.getY()));
				} else {
					//TODO: throw exception, only straight lines allowed in path!
				}
			}
			
			Map<String,String> metaProperties = new HashMap<String,String>();
			Edge edge = edges.get(shape);
			for(GAnsProperty<?> property : edge.getProperties()) {
				metaProperties.put(property.getName(), property.getValueAsString());
			}
			SerializedEdge serialized = new SerializedEdge(shapeProperties, metaProperties);
			set.add(serialized);
		}
		
		return set;
	}
	
	// convert color of javafx.scene.paint.color to HexString
    public static String toRGBCode(Color color)
    {
        return String.format( "#%02X%02X%02X",
            (int)( color.getRed() * 255 ),
            (int)( color.getGreen() * 255 ),
            (int)( color.getBlue() * 255 ) );
    }
}
