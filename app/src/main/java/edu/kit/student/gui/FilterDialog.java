package edu.kit.student.gui;

import edu.kit.student.plugin.EdgeFilter;
import edu.kit.student.plugin.Filter;
import edu.kit.student.plugin.PluginManager;
import edu.kit.student.plugin.VertexFilter;
import edu.kit.student.util.LanguageManager;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A dialog for selecting the vertex and edge filter.
 * Accepts a preselected set of filter.
 * Up on selection and removal of a filter by the user
 * through a check box, the filter will be removed
 * from the initial set of filters.
 *
 * @author Lucas Steinmann, Nicolas Boltz
 */
public class FilterDialog extends Dialog<ButtonType> {


    private final Logger logger = LoggerFactory.getLogger(GraphViewSelectionModel.class);

    /**
     * Constructs a new dialog for selecting filter.
     * <p>
     * The filters passed as arguments should be the filters
     * which are currently active.
     * These filter will be preselected when the dialog is opened.
     * </p>
     *
     * @param activeVertexFilter the currently active vertex filter
     * @param activeEdgeFilter the currently active edge filter
     */
    public FilterDialog(List<VertexFilter> activeVertexFilter, List<EdgeFilter> activeEdgeFilter) {
        super();

        // Create the vertex filter tab
        TabPane tabPane = new TabPane();
        Tab vertexFilterTab = new Tab(LanguageManager.getInstance().get("wind_filter_vertices"));
        vertexFilterTab.setClosable(false);

        //Create the edge filter tab
        Tab edgeFilterTab = new Tab(LanguageManager.getInstance().get("wind_filter_edges"));
        edgeFilterTab.setClosable(false);

        // The box controlling the state of all vertices
        CheckBox checkAllVertex = new CheckBox(LanguageManager.getInstance().get("filter_select_all"));
        checkAllVertex.setPadding(new Insets(0, 0, 0, 12));
        CheckBox checkAllEdge = new CheckBox(LanguageManager.getInstance().get("filter_select_all"));
        checkAllEdge.setPadding(new Insets(0, 0, 0, 12));


        List<FilterCheckBox> vertexFilterCheckboxes = new LinkedList<>();
        for (VertexFilter filter : PluginManager.getPluginManager().getVertexFilter())
            vertexFilterCheckboxes.add(new FilterCheckBox<>(filter, activeVertexFilter));
        List<FilterCheckBox> edgeFilterCheckboxes = new LinkedList<>();
        for (EdgeFilter filter : PluginManager.getPluginManager().getEdgeFilter())
            edgeFilterCheckboxes.add(new FilterCheckBox<>(filter, activeEdgeFilter));


        // Fill with groups of checkboxes
        Pane vertexCheckboxes = createCheckboxSetPane(vertexFilterCheckboxes, Filter::getGroup, 6);
        Pane edgeCheckboxes = createCheckboxSetPane(edgeFilterCheckboxes, Filter::getGroup, 6);


        new FilterGroup(checkAllVertex, vertexFilterCheckboxes);
        new FilterGroup(checkAllEdge, edgeFilterCheckboxes);


        VBox vertexGroups = new VBox(10, checkAllVertex, vertexCheckboxes);
        vertexGroups.setPadding(new Insets(10, 0, 0, 0));
        vertexFilterTab.setContent(vertexGroups);

        VBox edgeGroups = new VBox(10, checkAllEdge, edgeCheckboxes);
        edgeGroups.setPadding(new Insets(20, 0, 0, 0));
        edgeFilterTab.setContent(edgeGroups);

        tabPane.getTabs().addAll(vertexFilterTab, edgeFilterTab);


        this.getDialogPane().setContent(tabPane);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        this.setTitle(LanguageManager.getInstance().get("wind_filter_title"));
        this.setHeaderText(null);
        this.setGraphic(null);
        this.setWidth(500);
        this.setHeight(500);
        Stage stage = (Stage) this.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("gans_icon.png"));
    }

    private Pane createCheckboxSetPane(List<FilterCheckBox> checkBoxes,
                                       Function<Filter, String> groupMap,
                                       int columnCount) {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(20,0,0,0));
        // Group items after the specified group mapping
        Map<String, List<FilterCheckBox>> groupAll =
                checkBoxes.stream().collect(
                    Collectors.groupingBy(groupMap.compose(fcb -> fcb.filter)));

        // For every group create one checkbox group
        for (String group : groupAll.keySet()) {
            pane.getChildren().add(createCheckboxGroup(groupAll.get(group), group, columnCount));
        }
        return pane;
    }

    private Node createCheckboxGroup(List<FilterCheckBox> filters,
                                     String groupName, int columnCount) {

        GridPane gridPane = new GridPane();

        // Create group box which toggles all boxes inside the group
        final CheckBox groupCheckBox = new CheckBox(groupName);
        groupCheckBox.setAllowIndeterminate(false);

        FilterGroup filterGroup = new FilterGroup(groupCheckBox, new HashSet<>());


        int column = 0;
        int row = 0;
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        // Create equally spaced columns
        for (int i = 0; i < columnCount; i++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0/columnCount);
            gridPane.getColumnConstraints().add(constraint);
        }

        for(FilterCheckBox box : filters) {
            if(column == columnCount) {
                column = 0;
                row++;
            }
            filterGroup.add(box);
            gridPane.add(box, column, row);
            column++;
        }

        return createBorderedGroup(gridPane, groupCheckBox);
    }

    private Pane createBorderedGroup(Node content, Region inTitle) {

        StackPane pane = new StackPane() {

            private final static int CheckBoxPadding = 4;

            {
                getChildren().add(content);
                inTitle.setPadding(new Insets(0, 0, 0, CheckBoxPadding));
                inTitle.setStyle("-fx-background-color: -fx-background");
                getChildren().add(inTitle);
            }

            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                final double groupCbHeight = inTitle.prefHeight(-1);
                final double groupCbWidth = inTitle.prefWidth(groupCbHeight) + CheckBoxPadding;
                inTitle.resize(groupCbWidth, groupCbHeight);

                // Move checkbox a bit right from the top left corner
                inTitle.relocate(CheckBoxPadding * 2, -groupCbHeight / 2.0 - 1);
            }
        };
        pane.setPadding(new Insets(10, 0, 10, 0));
        pane.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID,
                new CornerRadii(4), new BorderWidths(1))));

        return pane;
    }



    private class FilterGroup {

        public final CheckBox representingCheckBox;
        private final List<FilterCheckBox> checkBoxGroup;

        public FilterGroup(CheckBox representingCheckBox, Collection<FilterCheckBox> checkBoxes) {
            this.representingCheckBox = representingCheckBox;
            representingCheckBox.setOnAction(event -> updateGroup());
            this.checkBoxGroup = new LinkedList<>();
            addAll(checkBoxes);
        }

        public void add(FilterCheckBox checkBox) {
            this.checkBoxGroup.add(checkBox);
            updateGroupBox();
            checkBox.addListener((obs, o, n) -> updateGroupBox());
        }

        public void addAll(Collection<FilterCheckBox> checkBoxes) {
            for (FilterCheckBox cb : checkBoxes)
                add(cb);
        }


        /**
         * Synchronizes the state of the representing group checkbox with the current state of
         * the checkboxes in the group
         */
        private <T> void updateGroup() {
            checkBoxGroup.forEach(checkBox -> checkBox.setSelected(representingCheckBox.isSelected()));
        }

        /**
         * Synchronizes the state of the representing group checkbox with the current state of
         * the checkboxes in the group.
         * This means the state of the groupCheckBox is adapted to match the state of all checkBoxes.
         */
        private void updateGroupBox() {
            boolean allSelected = checkBoxGroup.stream().allMatch(CheckBox::isSelected);
            boolean allNotSelected = checkBoxGroup.stream().noneMatch(CheckBox::isSelected);

            if (allSelected) {
                representingCheckBox.setSelected(true);
                representingCheckBox.setIndeterminate(false);
            } else if (allNotSelected) {
                representingCheckBox.setSelected(false);
                representingCheckBox.setIndeterminate(false);
            } else {
                representingCheckBox.setIndeterminate(true);
            }
        }
    }

    private class FilterCheckBox<T extends Filter> extends CheckBox {
        public final T filter;
        private final List<ChangeListener<Boolean>> listeners = new LinkedList<>();

        public FilterCheckBox(T filter, List<T> selectedSet) {
            super(filter.getName());
            this.filter = filter;
            selectedProperty().addListener(activateListeners);
            boolean selected = !selectedSet.contains(filter);
            setSelected(selected);
            addListener((obs, o, n) -> {
                if (n)
                    selectedSet.remove(filter);
                else
                    selectedSet.add(filter);
            });
        }

        public void addListener(ChangeListener<Boolean> eventHandler) {
            this.listeners.add(eventHandler);
        }

        public void removeListener(ChangeListener<Boolean> eventHandler) {
            this.listeners.remove(eventHandler);
        }

        private ChangeListener<Boolean> activateListeners = (obs, o, n) -> {
            for (ChangeListener<Boolean> listener : listeners) {
                listener.changed(obs, o, n);
            }
        };
    }
}
