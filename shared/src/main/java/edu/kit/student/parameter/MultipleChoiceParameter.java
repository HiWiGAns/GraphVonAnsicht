package edu.kit.student.parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * MultipleChoiceParameter are parameters with an predefined String value space.
 */
public class MultipleChoiceParameter extends Parameter<String> {
	private int selectedIndex;
	private List<String> values;

	/**
	 * Constructs a new MultipleChoiceParameter, sets its name, its possible
	 * choices and initialized index.
	 * @param name The name of the parameter.
	 * @param choices The choices of the parameter.
	 * @param initIndex The initialized index of the parameter.
	 */
	public MultipleChoiceParameter(String name, List<String> choices, int initIndex) {
		super(name, choices.get(initIndex));
		this.values = choices;
		this.selectedIndex = Integer.min(Integer.max(0, initIndex), choices.size() - 1);
	}

	/**
	 * Constructs a new MultipleChoiceParameter and sets its name. The possible
	 * choices and index are set as null.
	 * @param name The name of the parameter.
	 */
	public MultipleChoiceParameter(String name) {
		super(name, "");
		this.values = new ArrayList<String>();
		this.selectedIndex = 0;
	}

	/**
	 * Overloads the setValue of GAnsProperty. Sets the String at position
	 * selected in values as the value of the Parameter.
	 * @param selected The position in values that has been selected and will be set as value.
	 */
	public void setValue(int selected) {
		this.selectedIndex = selected;
		super.setValue(values.get(selected));
	}

	/**
	 * Returns a list of all set possible choices in the
	 * MultipleChoiceParameter.
	 * @return A list of all set possible choices in the MultipleChoiceParameter.
	 */
	public List<String> getChoices() {
		return values;
	}

	/**
	 * Adds a choice to the MultipleChoiceParameter.
	 * @param choice The choice that will be added.
	 * @param index The index in the list the new choice will be added.
	 */
	public void addChoice(String choice, int index) {
		values.add(index, choice);
	}

	/**
	 * Removes a choice from the MultipleChoiceParameter.
	 * @param index The index in the list of the choice to be removed.
	 */
	public void remove(int index) {
		values.remove(index);
	}

	/**
	 * Returns the index of the currently selected choice.
	 * @return The index of the currently selected choice.
	 */
	public int getSelectedIndex() {
		return selectedIndex;
	}

	@Override
	public void accept(ParameterVisitor visitor) {
		visitor.visit(this);
	}
}
