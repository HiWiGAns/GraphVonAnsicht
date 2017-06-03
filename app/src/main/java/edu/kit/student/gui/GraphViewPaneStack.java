package edu.kit.student.gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sun.javafx.util.Utils.clamp;

/**
 * The {@link GraphViewPaneStack} class encapsulates all graphical elements needed to make the {@link GraphView} behave as specified.
 * The {@link GraphView} is encapsulated in three other Nodes.
 * They control zooming, panning
 * and drawing control elements (such as a selection rectangle) over the graph.
 *
 * |-----------------------------------------------|
 * |                  AnchorPane                   |
 * |   |---------------------------------------|   |
 * |   |              ScrollPane               |   |
 * |   |   |-------------------------------|   |   |
 * |   |   |            Group          |   |   |   |
 * |   |   |   |-----------------------|   |   |   |
 * |   |   |   |      GraphView        |   |   |   |
 * |   |   |   |                       |   |   |   |
 * |   |   |   |                       |   |   |   |
 * |   |   |   |                       |   |   |   |
 * |   |   |   |                       |   |   |   |
 * |   |   |   |                       |   |   |   |
 * |   |   |   |-----------------------|   |   |   |
 * |   |   |-------------------------------|   |   |
 * |   |---------------------------------------|   |
 * |-----------------------------------------------|
 *
 * @author Nicolas Boltz, Lucas Steinmann
 *
 */
public class GraphViewPaneStack {


    private final AnchorPane wrapper;
    private final GraphView graphView;

    private ToolBar searchBar;

    private final VBox vBox;

    private final DoubleProperty scaleProperty = new SimpleDoubleProperty(1.0d);
    // Captures the last scroll direction and length
	private final DoubleProperty deltaY = new SimpleDoubleProperty(0.0d);

    private final Logger logger = LoggerFactory.getLogger(GraphViewPaneStack.class);


    /**
	 * Constructor.
     * Sets up all the panes and enables user controls.
	 * 
	 * @param graphView
	 *            The {@link GraphView} placed inside of the panes.
	 */
	GraphViewPaneStack(GraphView graphView) {
		this.graphView = graphView;

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPannable(false);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

		AnchorPane.setTopAnchor(scrollPane, 10.0d);
		AnchorPane.setRightAnchor(scrollPane, 10.0d);
		AnchorPane.setBottomAnchor(scrollPane, 10.0d);
		AnchorPane.setLeftAnchor(scrollPane, 10.0d);


        final Group group = new Group();
        group.getChildren().add(this.graphView);

        final PanAndZoomPane panAndZoomPane = new PanAndZoomPane();
        scaleProperty.bind(panAndZoomPane.myScale);
		deltaY.bind(panAndZoomPane.deltaY);
		panAndZoomPane.getChildren().add(group);


		final SceneGestures sceneGestures = new SceneGestures(panAndZoomPane);
		scrollPane.setContent(panAndZoomPane);
		panAndZoomPane.toBack();

		vBox = new VBox();
		vBox.setFocusTraversable(true);
		vBox.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> vBox.requestFocus());

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/searchbar.fxml"));
			searchBar = loader.load();
			SearchBarController controller = loader.getController();
			vBox.setOnKeyPressed(event -> {
				if (event.isControlDown() && event.getCode().equals(KeyCode.F))
                    controller.switchActive();
			});

		} catch (IOException e) {
			Label errLabel = new Label("Could not load search bar. Cause: " + e.getCause());
            errLabel.setTextFill(Color.web("#FF0000"));
            searchBar = new ToolBar(errLabel);
		}

		wrapper = new AnchorPane();
        wrapper.setFocusTraversable(false);
        wrapper.getChildren().addAll(scrollPane);

        vBox.getChildren().addAll(searchBar, wrapper);
        VBox.setVgrow(wrapper, Priority.ALWAYS);


		scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneGestures.getOnMouseClickedEventHandler());
		scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneGestures.getOnMousePressedEventHandler());
		scrollPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, sceneGestures.getOnMouseDraggedEventHandler());
		scrollPane.addEventFilter(ScrollEvent.ANY, sceneGestures.getOnScrollEventHandler());
	}


    /**
     * Returns a read only double property, which describes the value the graph is
     * scaled with.
     * @return the scale property
     */
    public ReadOnlyDoubleProperty getScaleProperty() {
        return this.scaleProperty;
    }

    /**
     * Returns the GraphView containing the actual graph elements.
     * @return the GraphView
     */
	GraphView getGraphView() {
		return graphView;
	}
	
	/**
	 * Returns the scale on which the InnerPane currently is.
	 *
	 * @return The scale of the InnerPane.
	 */
	double getScale() {
        return scaleProperty.get();
    }

	/**
	 * Sets the scale of the InnerPane.
	 *
	 * @param scale
	 *            The scale of the InnerPane.
	 */
    public void setScale( double scale) {
        scaleProperty.set(scale);
    }

    /**
     * Returns the outer most pane.
     * @return the root pane of this stack of panes.
     */
	Pane getRoot() {
        return vBox;
    }

    Pane getUIPane() {
	    return wrapper;
	}

	/**
	 * Moves the graph to center the specified point.
	 * The point is interpreted as point in the GraphView space
	 * @param p the point to center
	 */
	public void center(Point2D p) {
		// Get current center point
		Point2D center = new Point2D(wrapper.getTranslateX() + wrapper.getWidth() / 2, wrapper.getTranslateY() + wrapper.getHeight() / 2);
		center = graphView.sceneToLocal(wrapper.localToScene(center));
		Point2D delta = center.subtract(p);
		graphView.setTranslateX(graphView.getTranslateX() + delta.getX());
		graphView.setTranslateY(graphView.getTranslateY() + delta.getY());
	}


	private class PanAndZoomPane extends Pane {

		static final double DEFAULT_DELTA = 1.3d;
        static final double MAX_SCALE = 40d;
        static final double MIN_SCALE = 0.1d;
		private DoubleProperty myScale = new SimpleDoubleProperty(1.0);
		private DoubleProperty deltaY = new SimpleDoubleProperty(0.0);


		private PanAndZoomPane() {
			// add scale transform
			scaleXProperty().bind(myScale);
			scaleYProperty().bind(myScale);
		}


		private double getScale() {
			return myScale.get();
		}

		private void setScale(double scale) {
			myScale.set(scale);
		}

		private void setPivot(double x, double y, double scale) {
            translateXProperty().setValue(getTranslateX()-x);
            translateYProperty().setValue(getTranslateY()-y);
			setScale(scale);
		}

		private void fitWidth() {
			double scale = getParent().getLayoutBounds().getMaxX() / getLayoutBounds().getMaxX();
			double oldScale = getScale();

			double f = scale - oldScale;

			double dx = getTranslateX() - getBoundsInParent().getMinX() - getBoundsInParent().getWidth() / 2;
			double dy = getTranslateY() - getBoundsInParent().getMinY() - getBoundsInParent().getHeight() / 2;

			double newX = f * dx + getBoundsInParent().getMinX();
			double newY = f * dy + getBoundsInParent().getMinY();

			setPivot(newX, newY, scale);

		}

		private void resetZoom() {
			double scale = 1.0d;

			double x = getTranslateX();
			double y = getTranslateY();

			setPivot(x, y, scale);
		}

		private double getDeltaY() {
			return deltaY.get();
		}

		private void setDeltaY(double dY) {
			deltaY.set(dY);
		}
	}


	/**
	 * Mouse drag context used for scene and nodes.
	 */
	private class DragContext {

		double mouseAnchorX;
		double mouseAnchorY;

		double translateAnchorX;
		double translateAnchorY;

	}

	/**
	 * Listeners for making the scene's canvas draggable and zoomable
	 */
	private class SceneGestures {

		private DragContext sceneDragContext = new DragContext();

		PanAndZoomPane panAndZoomPane;

		private SceneGestures(PanAndZoomPane canvas) {
			this.panAndZoomPane = canvas;
		}

		private EventHandler<MouseEvent> getOnMouseClickedEventHandler() {
			return onMouseClickedEventHandler;
		}

		private EventHandler<MouseEvent> getOnMousePressedEventHandler() {
			return onMousePressedEventHandler;
		}

		private EventHandler<MouseEvent> getOnMouseDraggedEventHandler() {
			return onMouseDraggedEventHandler;
		}

		private EventHandler<ScrollEvent> getOnScrollEventHandler() {
			return onScrollEventHandler;
		}

		private EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {

			public void handle(MouseEvent event) {
                if (event.isControlDown() && event.getButton() == MouseButton.SECONDARY) {
                    sceneDragContext.mouseAnchorX = event.getX();
                    sceneDragContext.mouseAnchorY = event.getY();

                    sceneDragContext.translateAnchorX = panAndZoomPane.getTranslateX();
                    sceneDragContext.translateAnchorY = panAndZoomPane.getTranslateY();
                }
			}

		};

		private EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
			    if (event.isControlDown() && event.getButton() == MouseButton.SECONDARY) {
                    panAndZoomPane.setTranslateX(sceneDragContext.translateAnchorX + event.getX() - sceneDragContext.mouseAnchorX);
                    panAndZoomPane.setTranslateY(sceneDragContext.translateAnchorY + event.getY() - sceneDragContext.mouseAnchorY);
                    event.consume();
                }
			}
		};

		/**
         * Performs scaling with the cursor position as pivot point.
		 */
		private EventHandler<ScrollEvent> onScrollEventHandler = new EventHandler<ScrollEvent>() {

			@Override
			public void handle(ScrollEvent event) {

				double delta = PanAndZoomPane.DEFAULT_DELTA;

				double scale = panAndZoomPane.getScale();
				double oldScale = scale;

				panAndZoomPane.setDeltaY(event.getDeltaY());
				delta = (panAndZoomPane.deltaY.get() < 0) ? 1 / delta : delta;

                // Clamp scaling
                scale = clamp(PanAndZoomPane.MIN_SCALE, oldScale * delta, PanAndZoomPane.MAX_SCALE);

				double f = (scale / oldScale) - 1;

				// Calculate pivot point
				double dx = (event.getX() - (panAndZoomPane.getBoundsInParent().getWidth() / 2 + panAndZoomPane.getBoundsInParent().getMinX()));
				double dy = (event.getY() - (panAndZoomPane.getBoundsInParent().getHeight() / 2 + panAndZoomPane.getBoundsInParent().getMinY()));

				panAndZoomPane.setPivot(f * dx, f * dy, scale);

				event.consume();
			}
		};

		/**
         * Fits graph into viewport on double right click.
		 */
		private EventHandler<MouseEvent> onMouseClickedEventHandler = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.getButton().equals(MouseButton.SECONDARY)) {
					if (event.getClickCount() == 2) {
						panAndZoomPane.fitWidth();
					}
//				} else if (event.getButton().equals(MouseButton.PRIMARY)) {
//					if (event.getClickCount() == 2) {
//						center(new Point2D(0, 0));
//					}
				}
			}
		};
	}
}
