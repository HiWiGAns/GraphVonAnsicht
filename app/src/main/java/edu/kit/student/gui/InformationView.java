package edu.kit.student.gui;

import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.util.LanguageManager;
import edu.kit.student.util.ListSynchronization;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * The InformationView shows a given set of properties from the selected
 * vertices in the {@link GraphView}.
 * 
 * @author Nicolas
 */
public class InformationView extends TableView<GAnsProperty<?>> implements GAnsPane {

	private final ObservableList<GAnsProperty<?>> internalItems = FXCollections.observableArrayList();
	private final ListSynchronization<GAnsProperty<?>> synchronization = new ListSynchronization<>(internalItems);
	private ObservableList<GAnsProperty<?>> focusedList;

	InformationView() {
	    setItems(internalItems);

		// Implementation as described in:
		// https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/TableView.html)
		TableColumn<GAnsProperty<?>, String> propertyNameCol = new TableColumn<>(LanguageManager.getInstance().get("inf_prop"));
		propertyNameCol.setCellValueFactory(p -> p.getValue().propertyNameProperty());
		TableColumn<GAnsProperty<?>, String> propertyValueCol = new TableColumn<>(LanguageManager.getInstance().get("inf_value"));
		propertyValueCol.setCellValueFactory(p -> p.getValue().propertyValueAsString());

		propertyNameCol.prefWidthProperty().bind(this.widthProperty().divide(2));
		propertyValueCol.prefWidthProperty().bind(this.widthProperty().divide(2));

		getColumns().add(propertyNameCol);
		getColumns().add(propertyValueCol);

		refresh();
	}

	/**
	 * Sets focus to the specified observable list of properties.
	 * These properties will be shown in this InformationView.
	 * Updates in the list synchronized with the table.
	 *
	 * @param information
	 *            List with {@link GAnsProperty} elements which define the
	 *            content of the InformationView
	 */
	void setFocus(ObservableList<GAnsProperty<?>> information) {
		if (information == focusedList)
			return;

		internalItems.clear();
		internalItems.addAll(information);
		if (focusedList != null) {
			focusedList.removeListener(synchronization);
		}
		focusedList = information;
		information.addListener(synchronization);
	}

	@Override
	public boolean hasInformation() {
		return false;
	}

	@Override
	public ObservableList<GAnsProperty<?>> getInformation() {
	    throw new IllegalStateException();
	}
}
