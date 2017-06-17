package edu.kit.student.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;

public class GraphViewTab extends Tab {

	private GraphViewPaneStack panes;

	private final VBox content;

	private ToolBar searchBar;

	public GraphViewTab(GraphViewPaneStack panes) {
		this.panes = panes;
		this.content = new VBox();
		VBox.setVgrow(panes.getRoot(), Priority.ALWAYS);

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/searchbar.fxml"));
			searchBar = loader.load();
		} catch (IOException e) {
			Label errLabel = new Label("Could not load search bar. Cause: " + e.getCause());
			errLabel.setTextFill(Color.web("#FF0000"));
			searchBar = new ToolBar(errLabel);
		}
		this.content.getChildren().addAll(searchBar, panes.getRoot());
		searchBar.visibleProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));
		this.tabPaneProperty().addListener(addSearchBarToggle);
		assert getTabPane() == null;

		this.tabPaneProperty().addListener(new ChangeListener<TabPane>() {
			@Override
			public void changed(ObservableValue<? extends TabPane> observable, TabPane oldValue, TabPane newValue) {
			    System.out.println("TEEEEEEEEST");
//				newValue.focusedProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean old, Boolean newv) -> {
//					panes.getRoot().requestFocus();
//				});
			}
		});
		this.setContent(content);
	}

	private ChangeListener<TabPane> addSearchBarToggle = new ChangeListener<TabPane>() {
		@Override
		public void changed(ObservableValue<? extends TabPane> observable, TabPane oldValue, TabPane newValue) {
		    if (oldValue != null)
                oldValue.setOnKeyPressed(null);
            newValue.setOnKeyPressed(event -> {
				if (event.isControlDown() && event.getCode().equals(KeyCode.F)) {
					searchBar.setVisible(!searchBar.isVisible());
					searchBar.setManaged(!searchBar.isManaged());
				}
			});
		}
	};

	public GraphViewPaneStack getGraphViewPanes() {
		return panes;
	}
}
