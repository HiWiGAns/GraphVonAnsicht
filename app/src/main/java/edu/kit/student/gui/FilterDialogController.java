package edu.kit.student.gui;

import edu.kit.student.plugin.EdgeFilter;
import edu.kit.student.plugin.Filter;
import edu.kit.student.plugin.PluginManager;
import edu.kit.student.plugin.VertexFilter;
import edu.kit.student.util.LanguageManager;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
public class FilterDialogController extends Dialog<ButtonType> {


    private final Logger logger = LoggerFactory.getLogger(GraphViewSelectionModel.class);

    private static final int minWindowWidth = 500;
    private static final int minWindowHeight = 400;

    private enum Props { filterdialog_x, filterdialog_y, filterdialog_width, filterdialog_height }

    public static final ButtonType APPLYANDLAYOUT = new ButtonType("Apply and Layout", ButtonBar.ButtonData.APPLY);

    private FilterModel model;

    @FXML
    private Tab edgeTab;

    @FXML
    private Tab vertexTab;

    @FXML
    private DialogPane dialogPane;

    @FXML
    private CheckBox cbOptimize;

    /**
     *
     * @param model the model holding the state of activated and possible filter options
     */
    public void initModel(FilterModel model) {
        if (this.model != null)
            throw new IllegalStateException("Cannot init model twice.");
        this.model = model;
        fillDialog();
        this.setDialogPane(dialogPane);

        // Disable apply button when earlier disabled filter are enabled.
        final Button btnApply = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        model.needLayout.addListener((observable, oldValue, newValue) -> btnApply.setDisable(newValue));
        btnApply.setDisable(model.needLayout.getValue());

        model.canOptimize.addListener((observable, oldValue, newValue)
                -> cbOptimize.setDisable(!model.canOptimize.get() || model.needLayout.get()));
        model.needLayout.addListener((observable, oldValue, newValue)
                -> cbOptimize.setDisable(!model.canOptimize.get() || model.needLayout.get()));
        cbOptimize.setDisable(!model.canOptimize.getValue());
        cbOptimize.selectedProperty().addListener((observable, oldValue, newValue) -> model.setOptimize(newValue));
        cbOptimize.setSelected(model.optimize());
    }

    public static FilterDialogController showDialog(FilterModel model) {
        FilterDialogController dialogController = new FilterDialogController();
        ResourceBundle bundle = ResourceBundle.getBundle("language.Lang");
        FXMLLoader loader = new FXMLLoader( dialogController.getClass().getResource("/fxml/filterdialog.fxml"), bundle);
        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final FilterDialogController fdc = loader.getController();
        fdc.initModel(model);

        Stage stage = (Stage) fdc.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("gans_icon.png"));
        stage.setMinWidth(minWindowWidth);
        stage.setMinHeight(minWindowHeight);

        fdc.setOnCloseRequest(e -> fdc.saveSettings());
        fdc.loadConfig();
        fdc.setTitle(LanguageManager.getInstance().get("wind_filter_title"));
        fdc.setHeaderText(null);
        fdc.setGraphic(null);
        fdc.setResizable(true);


        return fdc;
    }

    private void loadConfig() {
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        ApplicationSettings settings = ApplicationSettings.getInstance();
        if (settings.hasProperty(Props.filterdialog_x.name())
                && settings.hasProperty(Props.filterdialog_y.name())) {
            setX(settings.getPropertyAsDouble(Props.filterdialog_x.name()));
            setY(settings.getPropertyAsDouble(Props.filterdialog_y.name()));
        }
        if (settings.hasProperty(Props.filterdialog_width.name())
                && settings.hasProperty(Props.filterdialog_height.name())) {
            getDialogPane().setPrefSize(settings.getPropertyAsDouble(Props.filterdialog_width.name()),
                                        settings.getPropertyAsDouble(Props.filterdialog_height.name()));
        }
    }

    private void saveSettings() {
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        ApplicationSettings settings = ApplicationSettings.getInstance();
        settings.setProperty(Props.filterdialog_x.name(), this.getX());
        settings.setProperty(Props.filterdialog_y.name(), this.getY());
        settings.setProperty(Props.filterdialog_width.name(), stage.getWidth());
        settings.setProperty(Props.filterdialog_height.name(), stage.getHeight());
        settings.saveSettings();
    }

    private void fillDialog() {

        // The box controlling the state of all vertices
        CheckBox checkAllVertex = new CheckBox(LanguageManager.getInstance().get("filter_select_all"));
        checkAllVertex.setPadding(new Insets(0, 0, 0, 12));
        CheckBox checkAllEdge = new CheckBox(LanguageManager.getInstance().get("filter_select_all"));
        checkAllEdge.setPadding(new Insets(0, 0, 0, 12));


        List<FilterCheckBox> vertexFilterCheckboxes = new LinkedList<>();
        for (VertexFilter filter : PluginManager.getPluginManager().getVertexFilter())
            vertexFilterCheckboxes.add(new FilterCheckBox<>(filter, model.getVertexFilters()));
        List<FilterCheckBox> edgeFilterCheckboxes = new LinkedList<>();
        for (EdgeFilter filter : PluginManager.getPluginManager().getEdgeFilter())
            edgeFilterCheckboxes.add(new FilterCheckBox<>(filter, model.getEdgeFilters()));


        // Fill with groups of checkboxes
        Pane vertexCheckboxes = createCheckboxSetPane(vertexFilterCheckboxes, Filter::getGroup);
        Pane edgeCheckboxes = createCheckboxSetPane(edgeFilterCheckboxes, Filter::getGroup);


        new FilterGroup(checkAllVertex, vertexFilterCheckboxes);
        new FilterGroup(checkAllEdge, edgeFilterCheckboxes);


        VBox vertexGroups = new VBox(10, checkAllVertex, vertexCheckboxes);
        vertexGroups.setPadding(new Insets(10, 0, 0, 0));
        ScrollPane vertexScroll = new ScrollPane(vertexGroups);
        vertexScroll.setFitToWidth(true);
        vertexTab.setContent(vertexScroll);

        VBox edgeGroups = new VBox(10, checkAllEdge, edgeCheckboxes);
        edgeGroups.setPadding(new Insets(20, 0, 0, 0));
        ScrollPane edgeScroll = new ScrollPane(edgeGroups);
        edgeScroll.setFitToWidth(true);
        edgeTab.setContent(edgeScroll);
    }

    private Pane createCheckboxSetPane(List<FilterCheckBox> checkBoxes,
                                       Function<Filter, String> groupMap) {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(20,0,0,0));
        // Group items after the specified group mapping
        Map<String, List<FilterCheckBox>> groupAll =
                checkBoxes.stream().collect(
                    Collectors.groupingBy(groupMap.compose(fcb -> fcb.filter)));

        // For every group create one checkbox group
        for (String group : groupAll.keySet()) {
            pane.getChildren().add(createCheckboxGroup(groupAll.get(group), group));
        }
        return pane;
    }

    private Node createCheckboxGroup(List<FilterCheckBox> filters,
                                     String groupName) {

        TilePane tilePane = new TilePane();

        // Create group box which toggles all boxes inside the group
        final CheckBox groupCheckBox = new CheckBox(groupName);
        groupCheckBox.setAllowIndeterminate(false);

        FilterGroup filterGroup = new FilterGroup(groupCheckBox, new HashSet<>());

        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setPadding(new Insets(10, 10, 10, 10));
        tilePane.setTileAlignment(Pos.BASELINE_LEFT);

        for(FilterCheckBox box : filters) {
            filterGroup.add(box);
            tilePane.getChildren().add(box);
        }

        return new BorderedPane(tilePane, groupCheckBox);
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
