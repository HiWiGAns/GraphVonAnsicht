package edu.kit.student.plugin;

import edu.kit.student.parameter.BooleanParameter;
import edu.kit.student.parameter.Parameter;
import edu.kit.student.parameter.Settings;
import edu.kit.student.util.LanguageManager;

import java.util.LinkedList;
import java.util.List;

/**
 * An option for a layout of a specific graph.
 * Workspaces can return these for a specific graph.
 * The client can then decide between one ore more LayoutOptions
 * When selected the layout will be applied to the graph.
 *
 */
public abstract class LayoutOption extends EntryPointOption implements Cloneable {

    /**
     * This should execute the layout on the graph, which should be
     * specified on construction, or in beforehand.
     * The settings, which are accessible over {@code getSettings()} will be used
     * to instantiate the LayoutAlgorithm.
     */
    public abstract void applyLayout();
    
    /**
     * Get the set of parameters for an algorithm of this option.
     * {@code choose()} has to be called up front.
     * 
     * @return the set of parameters
     */
    public Settings getSettings() {
        List<Parameter<?>> parameters = new LinkedList<>();
        parameters.add(new BooleanParameter(LanguageManager.getInstance().get("fix_vpos"), false));
        return new Settings(LanguageManager.getInstance().get("general"), parameters);
    }

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
     * Called when this layout option is chosen.
     * This allows the layout option to prepare the actual LayoutAlgorithm.
     */
    public abstract void chooseLayout();
}
