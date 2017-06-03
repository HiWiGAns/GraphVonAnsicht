package edu.kit.student.objectproperty;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

import java.lang.ref.WeakReference;


/**
 * The GAnsProperty is a single property of a vertex or an edge in GAns. The
 * name and value of the property are being stored in JavaFX-Properties. A graph
 * consists out of vertices and edges which contain GAnsProperties, which are
 * linked to the GUI-elements.
 * 
 * @param <T>
 *            The type of element that is to be stored in the GAnsProperty.
 * 
 * @author Nicolas
 */
public class GAnsProperty<T> implements ReadOnlyGAnsProperty<T> {

	/**
	 * A string with whom, factories or other elements from the GUI, can
	 * reference to the name/identifier of the value.
	 */
	public final static String nameId = "name";

	/**
	 * A string with whom, factories or other elements from the GUI, can
	 * reference to the value.
	 */
	public final static String valueId = "value";

	/**
	 * A string with whom, factories or other elements from the GUI, can
	 * reference to the string-representation of the value.
	 */
	public final static String valueAsStringId = "valueAsStringId";

	private WeakReference<StringProperty> namePropertyRef;
	private WeakReference<GAnsObjectProperty<T>> valuePropertyRef;
	private WeakReference<StringProperty> valueAsStringPropertyRef;

	private WeakChangeListener<String> namePropertyListener;
    private WeakChangeListener<T> valuePropertyListener;
    private WeakChangeListener<String> valueAsStringPropertyListener;

	private String name;
	private T value;

	/**
	 * Constructor, setting the name and value of the property.
	 * 
	 * @param name
	 *            The string will be set as the name of the GAnsProperty.
	 * @param value
	 *            The value that will be set in the GAnsProperty.
	 */
	public GAnsProperty(String name, T value) {
		setName(name);
		setValue(value);
	}

	/**
	 * Ensures that the property which contains the name/identifier of the
	 * GAnsProperty is not null and always set with the right name.
	 * 
	 * @return The StringProperty which contains the name/identifier of the
	 *         GAnsProperty.
	 */
	public StringProperty propertyNameProperty() {
		if (namePropertyRef == null || namePropertyRef.get() == null) {
			StringProperty nameProp = new SimpleStringProperty(this, nameId);
			nameProp.set(this.name);
            namePropertyRef = new WeakReference<>(nameProp);
            nameProp.addListener(new WeakChangeListener<>((observable, oldValue, newValue) ->  this.name = newValue ));
        }
		return namePropertyRef.get();
	}

	/**
	 * Ensures that the property which contains the value in the GAnsProperty is
	 * not null and always set with the right name.
	 * 
	 * @return The property which contains the value in the GAnsProperty.
	 */
	public GAnsObjectProperty<T> propertyValue() {
		if (valuePropertyRef == null || valuePropertyRef.get() == null) {
            GAnsObjectProperty<T> valueProp = new GAnsObjectProperty<>(this, valueId);
		    valueProp.addListener(new WeakChangeListener<>((observable, oldValue, newValue) -> { this.value = newValue; }));
            valuePropertyRef = new WeakReference<>(valueProp);
			propertyValueAsString().bind(valueProp.asString());
		}
		return valuePropertyRef.get();
	}

	/**
	 * Ensures that the property which contains the string-representation of the
	 * value in the GAnsProperty is not null and always set with the right name.
	 * 
	 * @return The StringProperty which contains the string-representation of
	 *         the value of the GAnsProperty.
	 */
	public StringProperty propertyValueAsString() {
		if (valueAsStringPropertyRef == null || valueAsStringPropertyRef.get() == null) {
            StringProperty valueAsStringProp = new SimpleStringProperty(this, valueAsStringId);
            valueAsStringProp.set(this.value.toString());
            valueAsStringPropertyRef = new WeakReference<>(valueAsStringProp);

            // TODO: Decide what to do if value as string was changed
            // Optimally would be a superclass of GAnsProperty, which is read only
            // and WritableGAnsProperties would need a handler for changing the property value as string.
            valueAsStringProp.addListener(new WeakChangeListener<>((observable, oldValue, newValue) -> {
                return;
            }));
        }
		return valueAsStringPropertyRef.get();
	}

	/**
	 * Sets the name/identifier of the GAnsProperty.
	 * 
	 * @param name The string will be set as the name of the GAnsProperty.
	 */
	public void setName(String name) {
	    if (namePropertyRef == null || this.namePropertyRef.get() == null) {
	        this.name = name;
        } else {
            propertyNameProperty().set(name);
        }
	}

	@Override
	public String getName() {
	    return this.name;
	}

	/**
	 * Sets the value and its string-representation of the GAnsProperty.
	 * 
	 * @param value The value that will be set in the GAnsProperty.
	 */
	public void setValue(T value) {
        if (valuePropertyRef == null || this.valuePropertyRef.get() == null) {
            this.value = value;
        } else {
            propertyValue().setValue(value);
        }
	}

	@Override
	public T getValue() {
	    return this.value;
	}

	@Override
	public String getValueAsString() {
		return propertyValueAsString().get();
	}

	@Override
	public void addListener(ChangeListener<T> listener) {
		propertyValue().addListener(listener);
	}

	@Override
	public void removeListener(ChangeListener<T> listener) {
		propertyValue().removeListener(listener);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GAnsProperty)) return false;

		GAnsProperty<?> that = (GAnsProperty<?>) o;


		if (!getName().equals(that.getName())) return false;

		if(getValue() ==null){
			if(that.getValue() != null)
				return false;
			return true;
		}
		return getValue().equals(that.getValue());
	}

	@Override
	public int hashCode() {
		int result = (name != null ? name.hashCode() : 0);
		result = 31 * result + (value != null ? value.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Property: " + this.name + ", " + this.value.toString();
	}
}