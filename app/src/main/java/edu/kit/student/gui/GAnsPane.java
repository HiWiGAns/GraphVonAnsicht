package edu.kit.student.gui;

import edu.kit.student.objectproperty.GAnsProperty;
import javafx.collections.ObservableList;

/**
 * Common interface for all main panes in the GAnsApplication.
 */
public interface GAnsPane {

    /**
     * Returns true, if this pane wants information about its
     * content to be displayed in the {@link InformationView},
     * when it is focused.
     *
     * @return true if this pane has information to be displayed, false otherwise
     */
    boolean hasInformation();

    /**
     * Returns an observable list of {@link GAnsProperty}, if {@link GAnsPane#hasInformation()} returns true.
     * Otherwise undefined.
     *
     * @return the information as a list of properties.
     */
    ObservableList<GAnsProperty<?>> getInformation();
}
