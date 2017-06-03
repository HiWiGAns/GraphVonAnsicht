package edu.kit.student.graphmodel;

import java.util.List;

import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.util.DoublePoint;
import edu.kit.student.util.IntegerPoint;
import javafx.scene.paint.Color;

/**
 * This vertex interface specifies a vertex. Every vertex contains an ID, a name
 * and a label. The ID of a vertex is unique.
 */
public interface Vertex {

	/**
	 * Returns the nameId of the vertex. A descriptive nameId of the vertex.
	 * Multiple vertices with equal name in one graph are allowed. Therefore
	 * don't use this as identifier, instead use {@code getID()}.
	 * 
	 * @return The name of the vertex.
	 */
	public String getName();

	/**
	 * Returns the ID of the vertex. Every vertex in one graph has a unique ID.
	 * 
	 * @return The ID of the vertex.
	 */
	public Integer getID();

	/**
	 * Returns the label of the vertex, that will be shown in the GUI. The label
	 * can be an empty string.
	 * 
	 * @return The label of the vertex
	 */
	public String getLabel();

	/**
	 * Returns the X-coordinate of the vertex.
	 * 
	 * @return The X-coordinate of this vertex.
	 */
	public double getX();

	/**
	 * Returns the Y-coordinate of the vertex.
	 * 
	 * @return The Y-coordinate of the vertex.
	 */
	public double getY();
	
	/**
     * Set the X-coordinate of the vertex.
     * 
     * @param x X-coordinate of this vertex.
     */
    public void setX(double x);

    /**
     * Set the Y-coordinate of the vertex.
     * 
     * @param y The Y-coordinate of the vertex.
     */
    public void setY(double y);

	/**
	 * Adds the vertex to a {@link FastGraphAccessor}.
	 * 
	 * @param fga
	 *            The {@link FastGraphAccessor} to whom this vertex will be
	 *            added.
	 */
	public void addToFastGraphAccessor(FastGraphAccessor fga);
	
	/**
	 * Returns a list of properties of the Vertex that should be shown 
	 * in the InformationView when selected
	 * @return the list of properties
	 */
	public List<GAnsProperty<?>> getProperties();
	
	public DoublePoint getSize();
	
	//TODO: think about a better way for setting and getting the leftRightMargin from a vertex, with possibility to set a default very easy.
	public void setLeftRightMargin(IntegerPoint newMargin);
	
	public IntegerPoint getLeftRightMargin();

	public IntegerPoint getDefaultLeftRightMargin();
	
	public Color getColor();
}
