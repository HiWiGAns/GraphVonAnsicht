package edu.kit.student.gui;

import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.util.DoublePoint;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.LinkedList;

/**
 * A visual representation of a vertex with a text inside of it.
 * 
 * @author Nicolas Boltz, Lucas Steinmann
 */
public class VertexShape extends GAnsGraphElement {

	private final ObservableList<Border> borderList = FXCollections.observableList(new LinkedList<>());
	private static final int BORDER_WIDTH = 2;

	private Rectangle rectangle;
	private Text text;
	private Color color;
	private final static double mindWidth = 20;
	private final static double mindHeight = 5;

	/**
	 * Constructor
	 */
	public VertexShape() {
		rectangle = new Rectangle(mindWidth, mindHeight);
		this.setColor(Color.GREEN);
		text = new Text();
		getChildren().addAll(rectangle, text);
	}

	/**
	 * Constructor. All settings are being automatically set through the supplied vertex.
	 * @param vertex The vertex that will be represented.
	 */
	public VertexShape(Vertex vertex) {
		DoublePoint size = vertex.getSize();
		this.rectangle = new Rectangle(vertex.getX(), vertex.getY(), size.x, size.y);

		// TODO: Clip text when text is larger than content.
		this.text = new Text(vertex.getLabel());
		getChildren().add(rectangle);
		if (!getText().isEmpty())
			getChildren().add(text);

		setColor(vertex.getColor());

		updateBorder();
		borderList.addListener((ListChangeListener<Border>) c -> updateBorder());
		relocate(vertex.getX(), vertex.getY());
	}

	@Override
	public void setText(String text) {
	    if (text.isEmpty())
	    	getChildren().remove(this.text);
	    else if (!getChildren().contains(this.text))
	    	getChildren().add(this.text);
		this.text.setText(text);
	}

	@Override
	public String getText() {
		return this.text.getText();
	}

	@Override
	public void setColor(Color color) {
		this.color = color;
        rectangle.setFill(Color.TRANSPARENT);
	}
	
	@Override
	public Color getColor() {
		return this.color;
	}

	@Override
	public String toString() {
		return "Vertex \"" + text.getText() + "\"";
	}

	@Override
	public Rectangle getElementShape() {
		return rectangle;
	}
	
	/**
	 * Adds the specified border to the current borders of this vertex shape at the outermost position.
	 * @param border the border to add
	 */
	public void addBorder(Border border) {
		this.borderList.add(0, border);
	}

	/**
	 * Adds a border with the specified color to the current borders of this vertex shape at the outermost position.
	 * Returns a reference to the border for later removal.
	 * @param color the color of the border to add
	 * @return the border which has been added
	 */
	public Border addBorder(Color color) {
		Border border = new Border(color);
		this.borderList.add(border);
		return border;
	}

	/**
     * Removes the specified border from the list of border.
	 * Returns true if the border was set as a border of this shape. Otherwise false.
	 * @return true, if the border was removed, false otherwise.
	 */
	public boolean removeBorder(Border border) {
		return this.borderList.remove(border);
	}

	private void updateBorder() {
		StringBuilder cssStringB = new StringBuilder();

		cssStringB.append("-fx-background-color: ");
		// Add borders from outermost to innermost
		for (Border border : borderList) {
			cssStringB.append(GraphViewGraphFactory.toRGBCode(border.getColor()))
					  .append(", ");
		}
		// Set inner background (the color of the vertex)
		cssStringB.append(GraphViewGraphFactory.toRGBCode(color))
				  .append(";\n");

		cssStringB.append("-fx-background-insets: ");
		for (int i = 0; i <= borderList.size(); i++) {
			cssStringB.append(i * BORDER_WIDTH);
			if (i != borderList.size())
                  cssStringB.append(", ");
		}
		cssStringB.append(";\n");
		this.setStyle(cssStringB.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		VertexShape that = (VertexShape) o;

		if (borderList != null ? !borderList.equals(that.borderList) : that.borderList != null) return false;
		if (rectangle != null ? !rectangle.equals(that.rectangle) : that.rectangle != null) return false;
		if (text != null ? !text.equals(that.text) : that.text != null) return false;
		return color != null ? color.equals(that.color) : that.color == null;
	}

	/*@Override
	public int hashCode() {
		int result = borderList != null ? borderList.hashCode() : 0;
		result = 31 * result + (rectangle != null ? rectangle.hashCode() : 0);
		result = 31 * result + (text != null ? text.hashCode() : 0);
		result = 31 * result + (color != null ? color.hashCode() : 0);
		return result;
	}*/

	class Border {

		private Color color;

		Border(Color color) {
			this.color = color;
		}

		Color getColor() {
			return color;
		}

		void setColor(Color color) {
			this.color = color;
			updateBorder();
		}
	}
}
