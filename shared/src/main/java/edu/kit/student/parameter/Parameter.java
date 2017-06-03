package edu.kit.student.parameter;

import edu.kit.student.objectproperty.GAnsProperty;
import javafx.beans.value.ChangeListener;

/**
 * An abstract parameter class.
 * A parameter contains a value and a name.
 * Clients can set {@link ChangeListener} to track changes of the value.
 *
 * <p>
 * Classes inheriting from this class can be visited by a ParameterVisitor.
 * This enables an iteration over a set of variable parameters, while being able
 * to handle each parameter type adequately.
 * </p>
 *
 * @param <V> the type of the value of the parameter
 * @author Lucas Steinmann, Nicolas Boltz
 */
public abstract class Parameter<V> extends GAnsProperty<V> implements ReadOnlyParameter<V> {
	
	private V tempValue;
	
	/**
	 * Constructor, setting the name and value of the property.
	 * 
	 * @param name
	 *            The string will be set as the name of the GAnsProperty.
	 * @param value
	 *            The value that will be set in the GAnsProperty.
	 */
	public Parameter(String name, V value) {
		super(name, value);
		tempValue = value;
	}

	/**
	 * Let the visitor visit this parameter.
	 * @param visitor The visitor to visit
	 */
	public abstract void accept(ParameterVisitor visitor);

    /**
     * Caches the current value in a variable.
     * This has to be called before reset() is called, 
     * otherwise the cached value is the initial value of the parameter.
     */
    public void cacheCurrentValue() {
    	tempValue = this.getValue();
    }
    
    /**
     * Sets the value of the parameter to the value cached via cacheCurrentValue().
     * If no value is cached, the initial value of the parameter is set.
     */
    public void reset() {
    	this.setValue(tempValue);
    }
}
