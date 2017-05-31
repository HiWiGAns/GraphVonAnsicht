package edu.kit.student.gui;

import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.util.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages group of one graph.
 * It's lifetime matches the time a graph is hold by a GraphView.
 * Can launch an interface to add a group and one to manage existing ones.
 *
 * @author Nicolas Boltz, Lucas Steinmann
 */
class GroupManager {

	private final GraphViewGraphFactory factory;

    private final ObservableList<VertexGroup> groups;

    // Stores the ordering of the groups, at the time the dialog was started.
	// If the dialog is cancelled, the items in groups are set to the items in this list,
	// resulting in the earlier set and ordering.
    // If the changes are applied, this list is cleared.
	private final List<VertexGroup> groupBackup;

    // Stores the colors of the groups, at the time the dialog was started.
	// If the dialog is cancelled, the colors of the VertexGroups are reverted.
	// If the changes are applied, this map is cleared.
	private final Map<VertexGroup, Color> groupColorBackup;

	GroupManager(GraphViewGraphFactory factory) {
		this.factory = factory;
		groups = FXCollections.observableArrayList();
		groupColorBackup = new HashMap<>();
		groupBackup = new ArrayList<>();
	}
	
	boolean openAddGroupDialog(Set<ViewableVertex> vertices) {
		TextInputDialog dialog = new TextInputDialog(LanguageManager.getInstance().get("wind_group_new_default"));
    	dialog.setTitle(LanguageManager.getInstance().get("wind_group_new_title"));
    	dialog.setHeaderText(null);
    	dialog.setGraphic(null);
    	dialog.setContentText(LanguageManager.getInstance().get("wind_group_new_text"));
    	Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
    	stage.getIcons().add(new Image("gans_icon.png"));
    	Optional<String> result = dialog.showAndWait();
    	if (result.isPresent()){
    		VertexGroup group = new VertexGroup(factory, result.get(), vertices);
    		groups.add(group);
    		return true;
    	}
    	return false;
    }

	void openGroupDialog() {
		backup();

		Dialog<ButtonType> dialog = new Dialog<>();
		ListView<VertexGroup> groupList = new ListView<>();
		groupList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		groupList.setItems(groups);
		groupList.setCellFactory(list -> new GroupListCell());

		Button upButton = new Button(LanguageManager.getInstance().get("wind_group_up"));
		upButton.setDisable(true);
		upButton.setOnAction(event -> {
            VertexGroup group = groupList.getSelectionModel().getSelectedItem();
            int currentPos = groups.indexOf(group);
            groups.remove(group);
            groups.add(currentPos - 1, group);
            groupList.getSelectionModel().select(group);
        });
		
		Button downButton = new Button(LanguageManager.getInstance().get("wind_group_down"));
		downButton.setDisable(true);
		downButton.setOnAction(event -> {
            VertexGroup group = groupList.getSelectionModel().getSelectedItem();
            int currentPos = groups.indexOf(group);
            groups.remove(group);
            groups.add(currentPos + 1, group);
            groupList.getSelectionModel().select(group);
        });
		
		Button removeButton = new Button(LanguageManager.getInstance().get("wind_group_remove"));
		removeButton.setDisable(true);
		removeButton.setOnAction(event -> {
            VertexGroup group = groupList.getSelectionModel().getSelectedItem();
            groupList.getSelectionModel().clearSelection();
            groups.remove(group);
            groupList.refresh();
        });
		
		groupList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null) {
                upButton.setDisable(true);
                downButton.setDisable(true);
                removeButton.setDisable(true);
            } else {
                int index = groups.indexOf(newValue);
                upButton.setDisable(index == 0);
                downButton.setDisable(index == groups.size() - 1);
                removeButton.setDisable(false);
            }
        });
		
		HBox buttonBox = new HBox(upButton, downButton, removeButton);
		buttonBox.setSpacing(3);
		VBox root = new VBox(groupList, buttonBox);
		
		dialog.getDialogPane().setContent(root);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE, ButtonType.APPLY);
		dialog.setTitle(LanguageManager.getInstance().get("wind_group_title"));
		dialog.setHeaderText(null);
		dialog.setGraphic(null);
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
    	stage.getIcons().add(new Image("gans_icon.png"));


		final Button btnApply = (Button) dialog.getDialogPane().lookupButton(ButtonType.APPLY);
        btnApply.addEventFilter(ActionEvent.ACTION, event -> {
			applyChanges();
			backup();
			event.consume();
         });
		Optional<ButtonType> result = dialog.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			applyChanges();
		} else {
		    restore();
		}
	}

	private void backup() {
		groupColorBackup.clear();
		for(VertexGroup group : groups) {
			groupColorBackup.put(group, group.getColor());
		}
		groupBackup.clear();
		groupBackup.addAll(groups);
	}

	private void restore() {
		groups.clear();
		groups.addAll(groupBackup);
		for(Map.Entry<VertexGroup, Color> entry : groupColorBackup.entrySet()) {
			entry.getKey().setColor(entry.getValue());
		}
	}

	private void applyChanges() {
		for (VertexGroup group : groupBackup) {
			if (groups.contains(group))
                group.setColor(group.getPicker().getValue());
			else
				group.dissolve();
		}
	}

	private class GroupListCell extends ListCell<VertexGroup> {

		@Override
		public void updateItem(VertexGroup group, boolean empty) {
			super.updateItem(group, empty);

			if(!empty && group != null) {
				Region spacer = new Region();
				HBox box = new HBox(group.getLabel(), spacer, group.getPicker());
				HBox.setHgrow(spacer, Priority.ALWAYS);
				box.setSpacing(10);
				setGraphic(box);
			} else {
				setGraphic(null);
			}
		}
	}
}
