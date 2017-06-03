package edu.kit.student.gui;

import edu.kit.student.graphmodel.Edge;
import edu.kit.student.graphmodel.ViewableGraph;
import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.graphmodel.action.Action;
import edu.kit.student.plugin.EdgeFilter;
import edu.kit.student.plugin.LayoutOption;
import edu.kit.student.plugin.VertexFilter;
import edu.kit.student.util.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A view used for showing and creating a graph in GAns. It supports zooming and
 * other general navigation features.
 * 
 * @author Nicolas
 */
public class GraphView extends Pane {

	private GraphViewSelectionModel selectionModel;
	private GraphViewGraphFactory graphFactory;
	private LayoutOption layout;

	private ContextMenu contextMenu;
    private List<MenuItem> dynamicMenuListItems = new LinkedList<>();
    
	private GroupManager groupManager;
	
	private final GAnsMediator mediator;

	private final Collection<VertexFilter> lastVertexFilter = new LinkedList<>();
	private final Collection<EdgeFilter> lastEdgeFilter = new LinkedList<>();

	/**
	 * Constructor.
	 * 
	 * @param mediator to connect with context menu
	 */
	public GraphView(GAnsMediator mediator, GraphViewSelectionModel selectionModel) {
		this.mediator = mediator;
		this.contextMenu = new ContextMenu();
		this.selectionModel = selectionModel;
		selectionModel.getSelectedShapes().addListener(onSelectionChanged);
	}
	
	/**
	 * Sets a graph. Every element in the graph will be generated and then
	 * shown.
	 * 
	 * @param graph
	 *            The graph to be visualized in the view.
	 */
	public void setGraph(ViewableGraph graph) {
	    // TODO: When analyzing usages of setGraph, it shows that a graph is only set after
		// 		 creating a new GraphView. It should be discussed if a GraphView exists only
		//		 to hold one graph or can be reused. If it should not be reused, the graph
		//		 could be set in the constructor.
		graphFactory = new GraphViewGraphFactory(graph);
		groupManager = new GroupManager();

		lastEdgeFilter.addAll(graphFactory.getGraph().getActiveEdgeFilter());
		lastVertexFilter.addAll(graphFactory.getGraph().getActiveVertexFilter());

		lastVertexFilter.clear();
		lastVertexFilter.addAll(graph.getActiveVertexFilter());
		lastEdgeFilter.clear();
		lastEdgeFilter.addAll(graph.getActiveEdgeFilter());

		getChildren().addAll(graphFactory.getGraphicalElements());
	}
	
	public void reloadGraph() {
		graphFactory.refreshGraph();

		lastVertexFilter.clear();
		lastVertexFilter.addAll(graphFactory.getGraph().getActiveVertexFilter());
		lastEdgeFilter.clear();
		lastEdgeFilter.addAll(graphFactory.getGraph().getActiveEdgeFilter());

		getChildren().clear();
		getChildren().addAll(graphFactory.getGraphicalElements());
		double maxX = 0;
		double maxY = 0;
		for (Node element : getChildren()) {
		    maxX = maxX > element.getBoundsInParent().getMaxX() ? maxX : element.getBoundsInLocal().getMaxX();
		    maxY = maxY > element.getBoundsInParent().getMaxY() ? maxY : element.getBoundsInLocal().getMaxY();
		}
		setPrefSize(maxX, maxY);
	}

	/**
	 * Returns the current {@link GraphViewGraphFactory} from the view.
	 * 
	 * @return The current {@link GraphViewGraphFactory}.
	 */
	public GraphViewGraphFactory getFactory() {
		return graphFactory;
	}
	
	/**
	 * Returns the selection model of the GraphView.
	 *
	 * @return The selection model of the GraphView.
	 */
	public GraphViewSelectionModel getSelectionModel() {
		return this.selectionModel;
	}

	public ContextMenu getContextMenu() {
		return contextMenu;
	}

	public void setCurrentLayoutOption(LayoutOption layout) {
		this.layout = layout;
	}
	
	public LayoutOption getCurrentLayoutOption() {
		return this.layout;
	}
	
	/**
	 * Fetches the applicable actions from the current selection and
	 * adds them as menu items.
	 */
    private SetChangeListener<GAnsGraphElement> onSelectionChanged = change -> {
        GraphView.this.contextMenu.getItems().removeAll(dynamicMenuListItems);
        dynamicMenuListItems.clear();

        // Map the current selection shapes back to the actual GraphElements
        Set<ViewableVertex> vertices = new HashSet<>();
		Set<Edge> edges = new HashSet<>();
        for(VertexShape shape : getSelectionModel().getSelectedVertexShapes()) {
            vertices.add(getFactory().getVertexFromShape(shape));
        }
		for(EdgeShape shape : getSelectionModel().getSelectedEdgeShapes()) {
			edges.add(getFactory().getEdgeFromShape(shape));
		}

        int menuIdx = 0;
        addDynamicMenuItemsForActions(getFactory().getGraph().getSubGraphActions(vertices), contextMenu, menuIdx);
        // Set menu idx to 0 to add following items to the start of the menu.
        menuIdx = 0;
        if (vertices.size() + edges.size() == 1) {
            // If only one element is selected show the actions applicable to this element.
			if (vertices.size() == 1) {
			    // If said element is a vertex
				ViewableVertex vertex = vertices.iterator().next();
				// Display link if edge has a link
				int linkId = vertex.getLink();
				if (linkId != -1) {
					MenuItem item = new MenuItem(LanguageManager.getInstance().get("ctx_open_graph"));
					dynamicMenuListItems.add(item);
					// set action to open new graph
					item.setOnAction(event -> GraphView.this.mediator.openGraph(linkId));
					GraphView.this.contextMenu.getItems().add(menuIdx++, item);
				}
				menuIdx = addDynamicMenuItemsForActions(getFactory().getGraph().getVertexActions(vertex), contextMenu, menuIdx);
			} else {
				// If said element is an edge
				Edge edge = edges.iterator().next();
				addDynamicMenuItemsForActions(getFactory().getGraph().getEdgeActions(edge), contextMenu, menuIdx);
			}
		}
		if (vertices.size() > 0) {
        	// Display grouping action
			createGroupMenuItem(vertices, menuIdx);
		}
    };

    private int addDynamicMenuItemsForActions(Collection<? extends Action> actions, ContextMenu menu, int idx) {
        for (Action action : actions) {
			MenuItem item = createMenuItem(action);
			dynamicMenuListItems.add(item);
			menu.getItems().add(idx++, item);
		}
		return idx;
	}

    private MenuItem createMenuItem(Action action) {
		MenuItem item = new MenuItem(action.getName());
		item.setOnAction(event -> {
			selectionModel.clear();
			action.handle();
			getCurrentLayoutOption().chooseLayout();
			layoutGraph();
		});
		return item;
	}

	private void createGroupMenuItem(Set<ViewableVertex> vertices, int menuIdx) {
		MenuItem group = new MenuItem(LanguageManager.getInstance().get("ctx_group"));
		group.setOnAction(e ->
		{
			if(groupManager.openAddGroupDialog(vertices.stream().map(graphFactory::getShapeFromVertex).collect(Collectors.toSet()),
                                               this.getScene().getWindow())) {
				openGroupDialog();
				selectionModel.clear();
			}
		});

		this.contextMenu.getItems().add(menuIdx, group);
		this.dynamicMenuListItems.add(group);
	}


	public void openGroupDialog() {
		groupManager.openGroupDialog(this.getScene().getWindow());
	}
	
	public void openFilterDialog() {

        LinkedList<VertexFilter> selectedVertexFilter = new LinkedList<>(this.graphFactory.getGraph().getActiveVertexFilter());
		ObservableList<VertexFilter> obsvervedVertexFilter = FXCollections.observableList(selectedVertexFilter);
		List<VertexFilter> vertexBackup = new LinkedList<>(selectedVertexFilter);

		LinkedList<EdgeFilter> selectedEdgeFilter = new LinkedList<>(this.graphFactory.getGraph().getActiveEdgeFilter());
		ObservableList<EdgeFilter> observedEdgeFilter = FXCollections.observableList(selectedEdgeFilter);
		List<EdgeFilter> edgeBackup = new LinkedList<>(selectedEdgeFilter);

    	FilterDialog fd = new FilterDialog(obsvervedVertexFilter, observedEdgeFilter);
		fd.initOwner(this.getScene().getWindow());

    	// On Apply and Layout apply the filters and relayout the graph.
		final Button btnApplyAndLayout = (Button) fd.getDialogPane().lookupButton(FilterDialog.applyAndLayout);
        btnApplyAndLayout.addEventFilter(ActionEvent.ACTION, event -> {
            if (!(listEqualsNoOrder(vertexBackup, obsvervedVertexFilter) &&
                    listEqualsNoOrder(edgeBackup, observedEdgeFilter))) {
				applyAndLayout(obsvervedVertexFilter, observedEdgeFilter);
			}
			event.consume();
        });

		// On Apply and Layout only apply the filters.
		final Button btnApply = (Button) fd.getDialogPane().lookupButton(ButtonType.APPLY);
		btnApply.addEventFilter(ActionEvent.ACTION, event -> {
			this.graphFactory.getGraph().setVertexFilter(obsvervedVertexFilter);
			this.graphFactory.getGraph().setEdgeFilter(observedEdgeFilter);
			reloadGraph();
			event.consume();
		});

		// Disable apply button when earlier disabled filter are enabled.
		observedEdgeFilter.addListener((ListChangeListener<EdgeFilter>) c ->
            btnApply.setDisable(!(observedEdgeFilter.containsAll(lastEdgeFilter)
					           && obsvervedVertexFilter.containsAll(lastVertexFilter))));

		obsvervedVertexFilter.addListener((ListChangeListener<VertexFilter>) c ->
			btnApply.setDisable(!(observedEdgeFilter.containsAll(lastEdgeFilter)
					&& obsvervedVertexFilter.containsAll(lastVertexFilter))));

		fd.showAndWait();
	}

    private static <T> boolean listEqualsNoOrder(List<T> l1, List<T> l2) {
        final Set<T> s1 = new HashSet<>(l1);
        final Set<T> s2 = new HashSet<>(l2);

        return s1.equals(s2);
    }

    private void applyAndLayout(List<VertexFilter> vertexFilters, List<EdgeFilter> edgeFilters) {
		// Only redraw if OK was pressed and if there was a change in the selection.
		this.graphFactory.getGraph().setVertexFilter(vertexFilters);
		this.graphFactory.getGraph().setEdgeFilter(edgeFilters);

		lastVertexFilter.clear();
		lastEdgeFilter.clear();
		lastVertexFilter.addAll(vertexFilters);
		lastEdgeFilter.addAll(edgeFilters);

		layoutGraph();
	}

	private void layoutGraph() {
		this.layout.applyLayout();
		reloadGraph();
	}
}
