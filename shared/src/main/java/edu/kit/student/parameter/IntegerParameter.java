package edu.kit.student.parameter;

/**
 * IntegerParameters are parameters with an integer value space.
 */
public class IntegerParameter extends Parameter<Integer> {
    private int min;
    private int max;

    /**
     * Constructs a new IntegerParameter, sets its name, its default value and boundaries.
     * @param name The name of the parameter.
     * @param value The value of the parameter.
     * @param min The minimum boundary of the parameter.
     * @param max The maximum boundary of the parameter.
     */
    public IntegerParameter(String name, int value, int min, int max) {
    	super(name, value);
        this.min = min;
        this.max = max;
    }

	@Override
	public void accept(ParameterVisitor visitor) {
		visitor.visit(this);		
	}

	/**
	 * Returns the minimum boundary.
	 * @return The minimum boundary.
	 */
	public int getMin() {
		return min;
	}

	/**
	 * Sets the minimum boundary.
	 * @param min The minimum boundary.
	 */
	public void setMin(int min) {
		this.min = min;
	}

	/**
	 * Returns the maximum boundary.
	 * @return The maximum boundary.
	 */
	public int getMax() {
		return max;
	}

	/**
	 * Sets the maximum boundary.
	 * @param max The maximum boundary.
	 */
	public void setMax(int max) {
		this.max = max;
	}
}
