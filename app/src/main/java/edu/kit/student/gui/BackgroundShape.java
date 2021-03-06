package edu.kit.student.gui;

import edu.kit.student.graphmodel.InlineSubGraph;
import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.util.DoublePoint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class BackgroundShape extends GAnsGraphElement {
	
	private Rectangle rectangle;
	private Text text;
	private Color color;
	
	public BackgroundShape(ViewableVertex vertex) {
		DoublePoint size = vertex.getSize();
		this.rectangle = new Rectangle(size.x, size.y, vertex.getColor());
		this.text = new Text(vertex.getLabel());
		this.color = vertex.getColor();
		
		getChildren().addAll(rectangle, text);
		
		relocate(vertex.getX(), vertex.getY());
	}

	public BackgroundShape(InlineSubGraph subgraph) {
		DoublePoint size = subgraph.getSize();
		this.rectangle = new Rectangle(size.x, size.y, subgraph.getBackgroundColor());
		this.text = new Text(subgraph.getName());
		this.color = subgraph.getBackgroundColor();
		
		getChildren().addAll(rectangle, text);
		
		relocate(subgraph.getX(), subgraph.getY());
    }

    @Override
	public void setText(String text) {
		this.text.setText(text);
	}

	@Override
	public String getText() {
		return this.text.getText();
	}

	@Override
	public void setColor(Color color) {
		this.color = color;
		this.rectangle.setFill(color);
	}

	@Override
	public Color getColor() {
		return this.color;
	}

	@Override
	public Rectangle getElementShape() {
		return this.rectangle;
	}

}
