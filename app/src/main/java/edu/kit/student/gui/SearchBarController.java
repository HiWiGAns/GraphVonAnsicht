package edu.kit.student.gui;

import edu.kit.student.util.LanguageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;

import java.net.URL;
import java.util.ResourceBundle;

/**
 *
 */
public class SearchBarController implements Initializable {


    @FXML
    private TextField txtSearch;

    @FXML
    private ToolBar searchBar;

    private SearchBarModel model;

    private boolean active;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        model = new SearchBarModel();
        txtSearch.setPromptText(LanguageManager.getInstance().get("scb_search"));
        deactivate();
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    public void switchActive() {
        setActive(!isActive());
    }

    public void setActive(boolean state) {
        active = state;
        searchBar.setManaged(state);
        searchBar.setVisible(state);
    }
}
