package edu.kit.student.gui;

import edu.kit.student.graphmodel.Edge;
import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.objectproperty.GAnsProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * The selection model for the {@link GraphView}, that supports multiple
 * selection of vertices and edges.
 *
 * @author Nicolas Boltz, Lucas Steinmann
 */
class GraphViewSelectionModel {

    private final Logger logger = LoggerFactory.getLogger(GraphViewSelectionModel.class);
	private ObservableSet<VertexShape> selectedVertexShapes;
	private ObservableSet<EdgeShape> selectedEdgeShapes;
	private ObservableSet<GAnsGraphElement> selectedShapes;
	private ObservableList<GAnsProperty<?>> selectionInformation;


	GraphViewSelectionModel(GraphView graphView) {
		selectedVertexShapes = FXCollections.observableSet(new HashSet<VertexShape>());
		selectedEdgeShapes = FXCollections.observableSet(new HashSet<EdgeShape>());
		selectedShapes = FXCollections.observableSet(new HashSet<GAnsGraphElement>());
		selectionInformation = FXCollections.observableArrayList();

		selectedShapes.addListener((SetChangeListener<GAnsGraphElement>) change -> {
			GraphViewGraphFactory factory = graphView.getFactory();
            // Urgh.. Add common interface for edge and vertex
            if (change.wasAdded()) {
                Edge edge = factory.getEdgeFromShape(change.getElementAdded());
                if (edge != null)
                    selectionInformation.addAll(edge.getProperties());
                Vertex vertex = factory.getVertexFromShape(change.getElementAdded());
                if (vertex != null)
                    selectionInformation.addAll(vertex.getProperties());
            } else if (change.wasRemoved()) {
				Edge edge = factory.getEdgeFromShape(change.getElementRemoved());
				if (edge != null)
					selectionInformation.removeAll(edge.getProperties());
				Vertex vertex = factory.getVertexFromShape(change.getElementRemoved());
				if (vertex != null)
					selectionInformation.removeAll(vertex.getProperties());
			}
		});

		// Sync super set with individual sets.
		SetChangeListener<GAnsGraphElement> syncSetListener = change -> {
			selectedShapes.add(change.getElementAdded());
			selectedShapes.remove(change.getElementRemoved());
		};
		selectedVertexShapes.addListener(syncSetListener);
		selectedEdgeShapes.addListener(syncSetListener);
	}

	void addVertex(VertexShape node) {
		//node.setStyle("-fx-effect: dropshadow(three-pass-box, red, 2, 2, 0, 0);");
		if (selectedVertexShapes.add(node)) {
			VertexShape.Border border = node.addBorder(Color.RED);
			selectedVertexShapes.addListener((SetChangeListener<? super VertexShape>) change -> {
				if (change.getElementRemoved() == node) {
					node.removeBorder(border);
				}
			});
		}
	}

	void addEdge(EdgeShape edge) {
		edge.setStyle("-fx-effect: dropshadow(three-pass-box, red, 0.5, 2, 0, 0);");
		selectedEdgeShapes.add(edge);
	}

	void addAllVertices(Collection<VertexShape> nodes) {
		for (VertexShape node : nodes) {
			addVertex(node);
		}
	}

	void addAllEdges(Collection<EdgeShape> edges) {
		for (EdgeShape edge : edges) {
			addEdge(edge);
		}
	}

	void removeVertex(VertexShape node) {
		selectedVertexShapes.remove(node);
	}

	void removeEdge(EdgeShape edge) {
		edge.setStyle(edge.getEdgeStyle());
		selectedEdgeShapes.remove(edge);
	}

	void clear() {
		while (!selectedVertexShapes.isEmpty()) {
			removeVertex(selectedVertexShapes.iterator().next());
		}
		while (!selectedEdgeShapes.isEmpty()) {
			removeEdge(selectedEdgeShapes.iterator().next());
		}
	}

	boolean contains(VertexShape node) {
		return selectedVertexShapes.contains(node);
	}

	boolean contains(EdgeShape edge) {
		return selectedEdgeShapes.contains(edge);
	}

	/**
	 * Returns true if this {@link GraphViewSelectionModel} doesn't contain
	 * any {@link VertexShape}s or {@link EdgeShape}s.
	 * @return true if no {@link VertexShape}s or {@link EdgeShape}s are in this selection model, false otherwise.
	 */
	boolean isEmpty() {
		return selectedVertexShapes.isEmpty() && selectedEdgeShapes.isEmpty();
	}

	private void log() {
//	    logger.debug("Selected rectangle " + rubberband.rect.toString());
        logger.debug("Items in model: Vertices {" + Arrays.asList(selectedVertexShapes.toArray()) + "}" +
				" Edges {" + Arrays.asList(selectedEdgeShapes.toArray()) + "}");
	}

	ObservableSet<VertexShape> getSelectedVertexShapes() {
		return selectedVertexShapes;
	}

	ObservableSet<EdgeShape> getSelectedEdgeShapes() {
		return selectedEdgeShapes;
	}

	ObservableSet<GAnsGraphElement> getSelectedShapes() {
		return selectedShapes;
	}

	ObservableList<GAnsProperty<?>> getSelectionInformation() {
	    return selectionInformation;
	}
}
