package edu.kit.student.plugin;

import edu.kit.student.graphmodel.Graph;
import edu.kit.student.parameter.Settings;

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
    public abstract Settings getSettings();

    /**
     * Called when this layout option is chosen.
     * This allows the layout option to prepare the actual LayoutAlgorithm.
     */
    public abstract void chooseLayout();

    public boolean canOptimizeEdges() {
        return getLayout().canOptimizeEdges();
    }

    public void setFixVertices(boolean fixVertices) {
        getLayout().setFixesVertices(fixVertices);
    }

    public boolean getFixVertices() {
        return getLayout().fixesVertices().getValue();
    }

    public abstract LayoutAlgorithm<? extends Graph> getLayout();
}
