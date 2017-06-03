package edu.kit.student.objectproperty;

import javafx.beans.value.ChangeListener;

/**
 * A {@link ReadOnlyGAnsProperty} is a single read-only property, which can be used to describe
 * a property of an object in the GAns-Framework.
 *
 * @param <T> The type of element that is to be stored in the GAnsProperty.
 * @author Lucas Steinmann
 */
public interface ReadOnlyGAnsProperty<T> {

    /**
     * Returns the name/identifier of this property.
     * If this property has no name, this method returns an empty {@link String}.
     *
     * @return the name or an empty {@link String}.
     */
    String getName();

    /**
     * Returns the value of this property.
     *
     * @return the value
     */
    T getValue();

    /**
     * Returns the string representation of the value of this GAnsProperty.
     *
     * @return the string representation of the value of this GAnsProperty.
     */
    String getValueAsString();

    /**
     * Adds a {@link ChangeListener} to the value of the property.
     *
     * @param listener the listener that will be added.
     */
    void addListener(ChangeListener<T> listener);

    /**
     * Removes a {@link ChangeListener} from the value of the property.
     *
     * @param listener the listener that will be removed.
     */
    void removeListener(ChangeListener<T> listener);
}
