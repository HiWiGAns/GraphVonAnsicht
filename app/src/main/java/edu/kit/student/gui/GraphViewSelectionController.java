package edu.kit.student.gui;

import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;

import java.util.Set;
import java.util.stream.Collectors;

public class GraphViewSelectionController {

    private final GraphViewSelectionModel model;
    private final int MIN_CLICKBOUND_SIZE = 10;
    private final SelectionGestures rubberBand;

    public GraphViewSelectionController(GraphViewSelectionModel model, GraphViewPaneStack panes) {
        this.model = model;
        this.rubberBand = new SelectionGestures(panes);

}
    public SelectionGestures getRubberBand() {
        return rubberBand;
    }



    private class SelectionGestures {
        // Stores the state of the current dragging action (start point etc.)
        private final DragContext dragContext = new DragContext();
        // The rectangle which is drawn to visualize the area to select
        private final Rectangle rect;
        private final GraphViewPaneStack viewPanes;
        private final GraphView view;
        private ContextMenu menu;

        private SelectionGestures(GraphViewPaneStack viewPanes) {
            this.viewPanes = viewPanes;
            this.view = viewPanes.getGraphView();
            this.menu = view.getContextMenu();

            rect = new Rectangle(0, 0, 0, 0);
            rect.setStroke(Color.BLUE);
            rect.setStrokeWidth(1);
            rect.setStrokeLineCap(StrokeLineCap.ROUND);
            rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));

            // Hide menu when zooming (looks weird when it is drawn while graph is scaled)
            viewPanes.getScaleProperty().addListener((ob, n, o) -> menu.hide());

            viewPanes.getUIPane().addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
            viewPanes.getUIPane().addEventFilter(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
            viewPanes.getUIPane().addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);
        }

        void setContextMenu(ContextMenu menu) {
            this.menu = menu;
        }


        private void showRect() {
            viewPanes.getUIPane().getChildren().add(rect);
        }

        private void hideRect() {
            viewPanes.getUIPane().getChildren().remove(rect);
        }

        EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                menu.hide();
                dragContext.mouseAnchorX = event.getX();
                dragContext.mouseAnchorY = event.getY();

                if (event.getButton() == MouseButton.PRIMARY) {
                    // If primary is pressed a new selection is started
                    // => show empty rectangle at mouse cursor position
                    rect.setX(dragContext.mouseAnchorX);
                    rect.setY(dragContext.mouseAnchorY);
                    rect.setWidth(0);
                    rect.setHeight(0);
                    showRect();
                } else if (event.getButton() == MouseButton.SECONDARY) {
                    // Open menu on right mouse button click.
                    if (!event.isControlDown()) {
                        // Instead of rectangle use a small hit box at cursor position to intersect elements.
                        // Area must be greater than zero as the area of the intersection is used to determine if
                        // a collision is taking place.
                        final BoundingBox clickBound = new BoundingBox(event.getSceneX() - MIN_CLICKBOUND_SIZE/2,
                                event.getSceneY() - MIN_CLICKBOUND_SIZE,
                                MIN_CLICKBOUND_SIZE, MIN_CLICKBOUND_SIZE);

                        // Get all intersected elements in the graph
                        // should mostly contain one item (if there are nodes on top of each other there
                        // can be more items contained)
                        final Set<VertexShape> vertexShapes = intersectedVertexShapes(clickBound);
                        final Set<EdgeShape> edgeShapes = intersectedEdgeShapes(clickBound);

                        if (vertexShapes.isEmpty() && edgeShapes.isEmpty()) {
                            // If user draws empty rectangle => clear selection (even if control is pressed)
                            model.clear();
                        } else {
                            // If the selected element(s) is not in the set of previously selected elements
                            // clear the set
                            if (!(model.getSelectedVertexShapes().containsAll(vertexShapes)
                                    && model.getSelectedEdgeShapes().containsAll(edgeShapes))) {
                                model.clear();
                            }

                            // Add all intersected elements to the selection
                            model.addAllVertices(vertexShapes);
                            model.addAllEdges(edgeShapes);
                            menu.show(viewPanes.getUIPane(), event.getScreenX(),event.getScreenY());
                        }
                    }
                }
            }
        };

        EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    // Offset from initial anchor point where the mouse has been clicked
                    // NOT the delta between last and this event.
                    final double offsetX = event.getX() - dragContext.mouseAnchorX;
                    final double offsetY = event.getY() - dragContext.mouseAnchorY;

                    if (offsetX >= 0) {
                        rect.setWidth(offsetX);
                    } else {
                        rect.setX(event.getX());
                        rect.setWidth(dragContext.mouseAnchorX - rect.getX());
                    }

                    if (offsetY >= 0) {
                        rect.setHeight(offsetY);
                    } else {
                        rect.setY(event.getY());
                        rect.setHeight(dragContext.mouseAnchorY - rect.getY());
                    }
                    event.consume();
                }
            }
        };

        EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                // selection like in explorer
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (!event.isControlDown() && !model.isEmpty()) {
                        if (event.getClickCount() == 2){
                            // When double clicked on element
                            // => launch action of first menu item.
                            if (menu.getItems().size() > 0) {
                                menu.getItems().iterator().next().fire();
                            }
                        }

                        // selection needs to be cleared before adding new shapes
                        model.clear();
                    }

                    final Bounds selectionBounds;
                    // If rectangle is very small (e.g. because the user just clicking and did not draw a rectangle)
                    // artificially enlarge the bound of the selection
                    if (rect.getBoundsInLocal().getWidth() < MIN_CLICKBOUND_SIZE &&
                            rect.getBoundsInLocal().getHeight() < MIN_CLICKBOUND_SIZE) {
                        selectionBounds = new BoundingBox(event.getSceneX() - MIN_CLICKBOUND_SIZE/2,
                                event.getSceneY() - MIN_CLICKBOUND_SIZE,
                                MIN_CLICKBOUND_SIZE, MIN_CLICKBOUND_SIZE);
                    } else {
                        selectionBounds = rect.localToScene(rect.getBoundsInLocal());
                    }

                    // Get all intersected elements in the graph
                    final Set<VertexShape> vertexShapes = intersectedVertexShapes(selectionBounds);
                    final Set<EdgeShape> edgeShapes = intersectedEdgeShapes(selectionBounds);

                    if (vertexShapes.isEmpty() && edgeShapes.isEmpty()) {
                        // If user draws empty rectangle => clear selection (even if control is pressed)
                        model.clear();
                    } else {
                        // Add all intersected elements to the selection
                        // or remove them when control is pressed and they are already selected.
                        for (VertexShape shape : vertexShapes) {
                            if (event.isControlDown() && model.contains(shape)) {
                                model.removeVertex(shape);
                            } else {
                                model.addVertex(shape);
                            }
                        }
                        for (EdgeShape shape : edgeShapes) {
                            if (event.isControlDown() && model.contains(shape)) {
                                model.removeEdge(shape);
                            } else {
                                model.addEdge(shape);
                            }
                        }
                    }

//                    model.log();

                    rect.setX(0);
                    rect.setY(0);
                    rect.setWidth(0);
                    rect.setHeight(0);
                    hideRect();

                }
                event.consume();
            }
        };

        /**
         * Returns the {@link VertexShape}s, that are intersected by the specified bound.
         *
         * @param bounds the bounds to intersect the {@link VertexShape}s with.
         * @return the set of {@link VertexShape}s, which intersect with the bounds
         */
        private Set<VertexShape> intersectedVertexShapes(Bounds bounds) {
            return view.getFactory().getVertexShapes().stream()
                    .filter(v -> intersects(bounds, v))
                    .collect(Collectors.toSet());
        }

        /**
         * Returns the {@link EdgeShape}s, that are intersected by the specified bound.
         *
         * @param bounds the bounds to intersect the {@link EdgeShape}s with.
         * @return the set of {@link EdgeShape}s, which intersect with the bounds
         */
        private Set<EdgeShape> intersectedEdgeShapes(Bounds bounds) {
            return view.getFactory().getEdgeShapes().stream()
                    .filter(v -> intersects(bounds, v))
                    .collect(Collectors.toSet());
        }

        /**
         * Checks if there is a non empty intersection between the
         * specified {@link Bounds} in scene space and a given {@link GAnsGraphElement}.
         * This uses the actual shape of element and not its bounding box.
         * @param bounds the {@link Bounds} to intersect
         * @param element the {@link GAnsGraphElement} to intersect
         * @return true if the element intersects with the bounds.
         */
        private boolean intersects(Bounds bounds, GAnsGraphElement element) {
            Shape intersect = Shape.intersect(element.getElementShape(),
                    new Rectangle(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight()));
            return (intersect.getBoundsInLocal().getWidth() != -1 || intersect.getBoundsInLocal().getHeight() != -1);
        }

        private final class DragContext {
            //Anchor for the mouse position
            double mouseAnchorX;
            double mouseAnchorY;

            //Anchor for the translate position of the fxml
            double translateAnchorX;
            double translateAnchorY;

            @Override
            public String toString() {
                return "mouseX: " + mouseAnchorX + " mouseY: " +
                        mouseAnchorY + " translateX: " + translateAnchorX +
                        " translateY: " + translateAnchorY;
            }
        }
    }
}
