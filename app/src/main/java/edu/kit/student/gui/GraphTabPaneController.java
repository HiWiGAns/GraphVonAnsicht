package edu.kit.student.gui;

import edu.kit.student.graphmodel.ViewableGraph;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.plugin.LayoutOption;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GraphTabPaneController {

    final Property<GraphViewPaneStack> graphViewPanesProperty = new SimpleObjectProperty<>();

    private GraphTabPane tabPane;
    private GAnsApplication application;

    private final Logger logger = LoggerFactory.getLogger(GraphTabPaneController.class);

    GraphTabPaneController(GAnsApplication application) {
        this.application = application;
        tabPane = new GraphTabPane();
        tabPane.setId("GraphViewTabPane");
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, val) ->
                graphViewPanesProperty.setValue(val == null
                        ? null
                        : ((GraphViewTab) val).getGraphViewPanes()
        ));
    }

    GraphTabPane getTabPane() {
        return tabPane;
    }

    GraphView getGraphView() {
        return graphViewPanesProperty.getValue().getGraphView();
    }

    void openGraph(ViewableGraph graph) {
        LayoutOption option = graph.getDefaultLayout();
        option.chooseLayout();
        openGraph(graph, option);
    }

    void openGraph(ViewableGraph graph, LayoutOption layoutOption) {
        if (!openTab(graph)) {
            layoutOption.applyLayout();
            openNewGraph(graph, layoutOption);
            getGraphView().setCurrentLayoutOption(layoutOption);
        }
    }

    private void loadGraphIntoCurrentTab(ViewableGraph graph) {
        Tab tab = getTabPane().getSelectionModel().getSelectedItem();
        tab.setText(graph.getName());
        tab.setId(graph.getID().toString());
        getGraphView().setGraph(graph);
    }

    private void openNewGraph(ViewableGraph graph, LayoutOption layoutOption) {
        newEmptyTab();
        loadGraphIntoCurrentTab(graph);
    }

    private boolean openTab(ViewableGraph graph) {
        boolean found = false;
        for (Tab tab : getTabPane().getTabs()) {
            if (graph.getID().toString().equals(tab.getId())) {
                found = true;
                getTabPane().getSelectionModel().select(tab);
                break;
            }
        }
        return found;
    }

    private void newEmptyTab() {
        GraphView graphView = new GraphView(application);
        GraphViewPaneStack graphViewPaneStack = new GraphViewPaneStack(graphView);
        GraphViewSelectionController selectionController
                = new GraphViewSelectionController(graphView.getSelectionModel(), graphViewPaneStack);

        GraphViewTab tab = new GraphViewTab(graphViewPaneStack);
        getTabPane().getTabs().add(tab);
        // TODO: Needed?
        graphViewPaneStack.getRoot().requestFocus();
        getTabPane().getSelectionModel().select(tab);
    }

    class GraphTabPane extends TabPane implements GAnsPane {

        @Override
        public boolean hasInformation() {
            return getGraphView() != null;
        }

        @Override
        public ObservableList<GAnsProperty<?>> getInformation() {
            return getGraphView().getSelectionModel().getSelectionInformation();
        }
    }
}
