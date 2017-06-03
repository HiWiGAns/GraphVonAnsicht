package edu.kit.student.plugin;

import edu.kit.student.graphmodel.Graph;
import edu.kit.student.parameter.BooleanParameter;
import edu.kit.student.parameter.Parameter;
import edu.kit.student.parameter.ReadOnlyParameter;
import edu.kit.student.parameter.Settings;
import edu.kit.student.util.LanguageManager;

import java.util.LinkedList;
import java.util.List;

/**
 * An implementations of LayoutAlgorithm takes a graph.
 * It assigns all vertices absolute coordinates and assigns all edges coordinates, 
 * they have to pass through.
 * LayoutAlgorithms can be registered with a {@link LayoutOption} at a {@link LayoutRegister}.
 * 
 * @param <G> the type of directed graph which should be processed
 */
public abstract class LayoutAlgorithm<G extends Graph> {

    /**
     * The parameter holding the boolean option if the vertex positions should be fixed
     * during the layout.
     */
    private final BooleanParameter fixedVertexPosition
            = new BooleanParameter(LanguageManager.getInstance().get("fix_vpos"), false);

    public LayoutAlgorithm() {
        fixedVertexPosition.setDisabled(!canOptimizeEdges());
    }

    /**
     * Get the set of parameters for this instance of the algorithm.
     * <p>
     * Classes inheriting from this class should call this method on its super class
     * and include the settings in its settings.
     * </p>
     *
     * @return the set of parameters
     */
    public Settings getSettings() {
        List<Parameter<?>> parameters = new LinkedList<>();
        parameters.add(fixedVertexPosition);
        return new Settings(LanguageManager.getInstance().get("layout"), parameters);
    }

    /**
     * Layout the specified Graph.
     * 
     * @param graph the graph to layout
     */
    public abstract void layout(G graph);

    /**
     * Returns true if this {@link LayoutAlgorithm} can take a graph with fixed vertex position and only
     * layout edges.
     * <p>
     * Returns true by default.
     * If a {@link LayoutAlgorithm} can not layout a graph without changing it's vertices positions
     * it should override this method and return false.
     * </p>
     * @return true if this algorithm can just optimize edges
     */
    public boolean canOptimizeEdges() {
        return true;
    }

    /**
     * Returns a read-only version of the parameter, specifying if the vertices should be fixed.
     * @return the fixed vertices parameter
     */
    public ReadOnlyParameter<Boolean> fixedVertices() {
        return fixedVertexPosition;
    }

}
