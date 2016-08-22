package edu.kit.student.parameter;

/**
 * DoubleParameters are parameters with an double value space.
 */
public class DoubleParameter extends Parameter<DoubleParameter, Double> {
	private Double min;
	private Double max;
	private Double amoutPerStep;
	
    /**
     * Constructs a new DoubleParameter, sets its name, its default value and boundaries.
     * @param name The name of the parameter.
     * @param value The value of the parameter.
     * @param min The minimum boundary of the parameter.
     * @param max The maximum boundary of the parameter.
     * @param amoutPerStep 
     */
	public DoubleParameter(String name, Double value, Double min, Double max, Double amoutPerStep) {
		super(name, value);
		this.min = min;
		this.max = max;
		this.amoutPerStep = amoutPerStep;
	}

	@Override
	public void accept(ParameterVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Returns the minimum boundary.
	 * @return The minimum boundary.
	 */
	public double getMin() {
		return min;
	}

	/**
	 * Sets the minimum boundary.
	 * @param min The minimum boundary.
	 */
	public void setMin(double min) {
		this.min = min;
	}

	/**
	 * Returns the maximum boundary.
	 * @return The maximum boundary.
	 */
	public double getMax() {
		return max;
	}

	/**
	 * Sets the maximum boundary.
	 * @param max The maximum boundary.
	 */
	public void setMax(double max) {
		this.max = max;
	}
	
	public double getAmoutPerStep() {
		return amoutPerStep;
	}
	
	public void setAmoutPerStep(double amoutPerStep) {
		this.amoutPerStep = amoutPerStep;
	}
}
