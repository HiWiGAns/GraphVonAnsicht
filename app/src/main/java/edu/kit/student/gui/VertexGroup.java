package edu.kit.student.gui;

import edu.kit.student.util.IdGenerator;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a group of vertices.
 * These vertices are visually grouped by applying a border to their shape.
 *
 * @author Nicolas Boltz, Lucas Steinmann
 */
public class VertexGroup {
	
	private Integer id;
	private String name;
	private Color currentColor = Color.WHITE;
	private HashMap<VertexShape, VertexShape.Border> shapeToBorder = new HashMap<>();

	private Label label;
	private ColorPicker picker;
	
	public VertexGroup(String name, Set<VertexShape> vertexShapes) {
		this.id = IdGenerator.getInstance().createId();
		this.name = name;
		this.label = new Label(name);
		this.picker = new ColorPicker();
//		picker.setOnAction(action -> setColor(picker.getValue()));
		
		addVertices(vertexShapes);
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
	
	public void addVertices(Set<VertexShape> vertices) {
		// Only color new vertices
		showBorder(vertices);
	}

	/**
	 * Removes the border associated with this group from all vertices in this group.
	 */
	public void hideBorder() {
		for(Map.Entry<VertexShape, VertexShape.Border> entry : shapeToBorder.entrySet()) {
            entry.getKey().removeBorder(entry.getValue());
		}
	}

	/**
	 * Adds a border with the color of this group to the specified vertices.
     * @param vertices the vertices to color
	 * 				   should only contain vertices, which are in this group.
	 */
	private void showBorder(Set<VertexShape> vertices) {
		this.currentColor = picker.getValue();
		for (VertexShape shape : vertices) {
			VertexShape.Border border = shapeToBorder.get(shape);
			if (border == null) {
				border = shape.addBorder(this.currentColor);
				shapeToBorder.put(shape, border);
			}
			// Don't add the border twice.
			else if (!shape.borderList.contains(border)) {
                shape.addBorder(shapeToBorder.get(shape));
            }
		}
	}

	/**
	 * Adds a border with the color of this group to all vertices in this group.
	 */
	public void showBorder() {
		showBorder(shapeToBorder.keySet());
	}

	/**
	 * Removes the border of the vertices in this list.
	 * This must if one want to remove the group.
	 */
	public void dissolve() {
		hideBorder();
		shapeToBorder.clear();
	}

	public Set<VertexShape> getVertices() {
		return this.shapeToBorder.keySet();
	}
	
	private void updatePickerColor() {
		this.picker.setValue(this.currentColor);
	}
}
