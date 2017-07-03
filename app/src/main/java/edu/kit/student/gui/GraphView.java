package edu.kit.student.gui;

import edu.kit.student.graphmodel.Edge;
import edu.kit.student.graphmodel.VertexReference;
import edu.kit.student.graphmodel.ViewableGraph;
import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.graphmodel.action.Action;
import edu.kit.student.plugin.EdgeFilter;
import edu.kit.student.plugin.LayoutOption;
import edu.kit.student.plugin.VertexFilter;
import edu.kit.student.util.LanguageManager;
import javafx.collections.ListChangeListener;
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
 * A fxml used for showing and creating a graph in GAns. It supports zooming and
 * other general navigation features.
 * 
 * @author Nicolas
 */
public class GraphView extends Pane {
	// TODO: Deselect items on focus lost

	private final GraphViewSelectionModel selectionModel;
	private GraphViewGraphFactory graphFactory;
	private LayoutOption layout;
	private FilterModel model = new FilterModel();

	private final ContextMenu contextMenu = new ContextMenu();
    private final List<MenuItem> dynamicMenuListItems = new LinkedList<>();
    
	private GroupManager groupManager;
	
	private final GAnsApplication application;

	/**
	 * Hold the filters, which have been active in during the last layout.
	 */
	private final Set<VertexFilter> lastLayoutVFilter = new HashSet<>();
	private final Set<EdgeFilter> lastLayoutEFilter = new HashSet<>();

	/**
	 * Constructor.
	 */
	public GraphView(GAnsApplication application) {
		this.application = application;
		this.selectionModel = new GraphViewSelectionModel(this);
		selectionModel.getSelectedShapes().addListener(onSelectionChanged);
	}
	
	/**
	 * Sets a graph. Every element in the graph will be generated and then
	 * shown.
	 * 
	 * @param graph
	 *            The graph to be visualized in the fxml.
	 */
	public void setGraph(ViewableGraph graph) {
	    // TODO: When analyzing usages of setGraph, it shows that a graph is only set after
		// 		 creating a new GraphView. It should be discussed if a GraphView exists only
		//		 to hold one graph or can be reused. If it should not be reused, the graph
		//		 could be set in the constructor.
		graphFactory = new GraphViewGraphFactory(graph);
		groupManager = new GroupManager();

		lastLayoutVFilter.clear();
		lastLayoutVFilter.addAll(graph.getActiveVertexFilter());
		lastLayoutEFilter.clear();
		lastLayoutEFilter.addAll(graph.getActiveEdgeFilter());

		getChildren().addAll(graphFactory.getGraphicalElements());
	}
	
	public void reloadGraph() {
		graphFactory.refreshGraph();

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
	 * Returns the current {@link GraphViewGraphFactory} from the fxml.
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
				VertexReference link = vertex.getLink();
				if (link != null) {
					MenuItem item = new MenuItem(LanguageManager.getInstance().get("ctx_open_graph"));
					dynamicMenuListItems.add(item);
					// set action to open new graph
					item.setOnAction(event -> GraphView.this.application.navigateTo(link));
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

        // Reload vertex filters from graph (maybe they have changed through some other control)
        model.getVertexFilters().clear();
		model.getVertexFilters().addAll(this.graphFactory.getGraph().getActiveVertexFilter());


		model.getEdgeFilters().clear();
		model.getEdgeFilters().addAll(this.graphFactory.getGraph().getActiveEdgeFilter());

		model.backup();
		assert !needLayout();
		model.needLayoutProperty.setValue(needLayout());
		model.layoutCanOptimizeProperty.setValue(layout.canOptimizeEdges());
		FilterDialogController fdc = FilterDialogController.showDialog(model);
		fdc.initOwner(this.getScene().getWindow());

		// On Apply and Layout only apply the filters.
		final Button btnApply = (Button) fdc.getDialogPane().lookupButton(ButtonType.APPLY);
		btnApply.addEventFilter(ActionEvent.ACTION, event -> {
			this.graphFactory.getGraph().setVertexFilter(model.getVertexFilters());
			this.graphFactory.getGraph().setEdgeFilter(model.getEdgeFilters());
			reloadGraph();
			event.consume();
		});

		model.observableVertexFilters().addListener((ListChangeListener<VertexFilter>) c ->
				model.needLayoutProperty.setValue(needLayout()));
		model.observableEdgeFilters().addListener((ListChangeListener<EdgeFilter>) c ->
				model.needLayoutProperty.setValue(needLayout()));


		// On Apply and Layout apply the filters and relayout the graph.
		fdc.showAndWait().filter(FilterDialogController.APPLYANDLAYOUT::equals).ifPresent(b -> {
			if (model.changedSinceBackup()) {
			    if (model.optimize())
			        layout.setFixVertices(true);
				applyAndLayout(model.getVertexFilters(), model.getEdgeFilters());
				layout.setFixVertices(false);
			}
        });
	}

	private boolean needLayout() {
        return !model.getEdgeFilters().containsAll(lastLayoutEFilter)
            || !model.getVertexFilters().containsAll(lastLayoutVFilter);
	}

    private void applyAndLayout(List<VertexFilter> vertexFilters, List<EdgeFilter> edgeFilters) {
		// Only redraw if OK was pressed and if there was a change in the selection.
		this.graphFactory.getGraph().setVertexFilter(new LinkedList<>(vertexFilters));
		this.graphFactory.getGraph().setEdgeFilter(new LinkedList<>(edgeFilters));
		layoutGraph();
	}

	private void layoutGraph() {
		lastLayoutEFilter.clear();
		lastLayoutVFilter.clear();
		lastLayoutVFilter.addAll(graphFactory.getGraph().getActiveVertexFilter());
		lastLayoutEFilter.addAll(graphFactory.getGraph().getActiveEdgeFilter());
		assert !needLayout();
		this.layout.applyLayout();
		reloadGraph();
	}

}
