package edu.kit.student.parameter;

import edu.kit.student.objectproperty.ReadOnlyGAnsProperty;

/**
 * A {@link ReadOnlyParameter}
 * @param <V>
 */
public interface ReadOnlyParameter<V> extends ReadOnlyGAnsProperty<V> {
    // TODO: Implement read only parameter visiting

    /**
     * Returns true if this parameter is disabled.
     * An enabled parameter should be editable in any UI.
     * @return true if disabled, false otherwise
     */
    boolean isDisabled();
}
