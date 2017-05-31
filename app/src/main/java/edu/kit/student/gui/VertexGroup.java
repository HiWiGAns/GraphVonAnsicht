package edu.kit.student.gui;

import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.util.IdGenerator;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a group of vertices.
 * These vertices are visually grouped by applying a border to their shape.
 *
 * @author Nicolas Boltz, Lucas Steinmann
 */
public class VertexGroup {
	
	private GraphViewGraphFactory factory;
	
	private Integer id;
	private String name;
	private Color currentColor = Color.WHITE;
	private Set<ViewableVertex> vertices = new HashSet<>();
	private HashMap<VertexShape, VertexShape.Border> shapeToBorder = new HashMap<>();
	private Label label;
	private ColorPicker picker;
	
	public VertexGroup(GraphViewGraphFactory factory, String name, Set<ViewableVertex> vertices) {
		this.factory = factory;
		this.id = IdGenerator.getInstance().createId();
		this.name = name;
		this.label = new Label(name);
		this.picker = new ColorPicker();
//		picker.setOnAction(action -> setColor(picker.getValue()));
		
		addVertices(vertices);
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Label getLabel() {
		return this.label;
	}
	
	public Color getColor() {
		return this.currentColor;
	}
	
	public void setColor(Color color) {
		this.currentColor = color;
        for(VertexShape.Border border : shapeToBorder.values()) {
            border.setColor(color);
        }
		updatePickerColor();
	}
	
	public ColorPicker getPicker() {
		return this.picker;
	}
	
	public void addVertices(Set<ViewableVertex> vertices) {
		this.vertices.addAll(vertices);
		// Only color new vertices
		colorVertices(vertices);
	}

	/**
	 * Removes the color form the vertices
	 */
	private void uncolorVertices() {
		for(Map.Entry<VertexShape, VertexShape.Border> entry : shapeToBorder.entrySet()) {
            entry.getKey().removeBorder(entry.getValue());
		}
		shapeToBorder.clear();
		this.currentColor = Color.WHITE;
		updatePickerColor();
	}

	/**
	 * Should be called to add the border to the specified vertices.
     * @param vertices the vertices to color
	 */
	private void colorVertices(Set<ViewableVertex> vertices) {
		this.currentColor = picker.getValue();
		for (ViewableVertex vertex : vertices) {
			VertexShape shape = factory.getShapeFromVertex(vertex);
			if(shape != null) {
				shapeToBorder.put(shape, shape.addBorder(this.currentColor));
			}
		}
	}

	/**
	 * Removes the border of the vertices in this list.
	 * This must if one want to remove the group.
	 */
	public void dissolve() {
		uncolorVertices();
		this.vertices.clear();
	}
	
	private void updatePickerColor() {
		this.picker.setValue(this.currentColor);
	}
}
