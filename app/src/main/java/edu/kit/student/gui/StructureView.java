package edu.kit.student.gui;

import edu.kit.student.graphmodel.GraphModel;
import edu.kit.student.graphmodel.ViewableGraph;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.util.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.LinkedList;
import java.util.List;

/**
 * The StructureView regulates the access and representation of the elements in
 * the StructureView of GAns.
 * 
 * @author Nicolas
 */
public class StructureView extends TreeView<ViewableGraph> implements GAnsPane {

    private ObservableList<GAnsProperty<?>> graphStatistics = FXCollections.observableArrayList();

	/**
	 * Constructor.
	 */
	StructureView(GAnsApplication application) {
		setShowRoot(false);
		setCellFactory(param -> new GraphCell());
		getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		ContextMenu contextMenu = new ContextMenu();

		MenuItem openGraph = new MenuItem(LanguageManager.getInstance().get("ctx_open"));
		openGraph.setOnAction(e
				-> application.openGraph(getSelectionModel().getSelectedItem().getValue()));

		addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
			// Open graph on double click
			if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
				application.openGraph(getSelectedGraph());
			} else {
				// Display statistics of graph on selection
				ViewableGraph graph = getSelectedGraph();
				graphStatistics.clear();

				if (graph != null) {
					graphStatistics.addAll(graph.getStatistics());
				}
				mouseEvent.consume();
			}
		});

		contextMenu.getItems().add(openGraph);
		setContextMenu(contextMenu);
	}

	/**
	 * Creates a tree like representation from a given graph and its subgraphs.
	 * Should be called, before calling other methods, because there could be a
	 * dummy root-node in the View.
	 * 
	 * @param graphModel
	 *            The graph which should be represented.
	 */
	void showGraphModel(GraphModel graphModel) {
		TreeItem<ViewableGraph> root = new TreeItem<>();
		addGraphsToItem(graphModel, graphModel.getRootGraphs(), root);
		setRoot(root);
	}

	ViewableGraph getSelectedGraph() {
	    return selectionModelProperty().get().getSelectedItem().getValue();
	}

	@Override
	public boolean hasInformation() {
		return true;
	}

	@Override
	public ObservableList<GAnsProperty<?>> getInformation() {
		return graphStatistics;
	}

	private void addGraphsToItem(GraphModel model, List<? extends ViewableGraph> graphs, TreeItem<ViewableGraph> parent) {
		List<TreeItem<ViewableGraph>> items = new LinkedList<>();
		for(ViewableGraph graph : graphs) {
			TreeItem<ViewableGraph> graphItem = new TreeItem<>(graph);
			graphItem.setExpanded(true);
			items.add(graphItem);
			addGraphsToItem(model, model.getChildGraphs(graph), graphItem);
		}
		parent.getChildren().addAll(items);
	}


	private class GraphCell extends TreeCell<ViewableGraph> {

		@Override
		protected void updateItem(ViewableGraph item, boolean empty) {
			super.updateItem(item, empty);
            setText(item == null ? "" : item.getName());
		}
	}

}
