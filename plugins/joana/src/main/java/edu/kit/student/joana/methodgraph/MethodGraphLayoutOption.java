package edu.kit.student.joana.methodgraph;

import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.LayoutAlgorithm;
import edu.kit.student.plugin.LayoutOption;

/**
 * A {@link LayoutOption} which is specific for {@link MethodGraph}.
 */
public abstract class MethodGraphLayoutOption extends LayoutOption {
	
	private MethodGraph graph;
	private LayoutAlgorithm<MethodGraph> layout;
	
	/**
	 * Sets the {@link MethodGraph} that will be the target of the CallGraphLayoutOption.
	 * 
	 * @param graph
	 *            The {@link MethodGraph} that will be the target of the
	 *            MethodGraphLayoutOption.
	 */
	public void setGraph(MethodGraph graph) {
	    this.graph = graph;
	}

	/**
	 * Sets the LayoutAlgorithm that will be used to layout the set graph.
	 * 
	 * @param layout
	 *            The LayoutAlgorithm that will be used to layout the set graph.
	 */
	public void setLayout(LayoutAlgorithm<MethodGraph> layout) {
		this.layout = layout;
	}
	
	@Override
	public void applyLayout() {
		layout.layout(graph);
	}
	
    @Override
    public Settings getSettings() {
        if (layout == null) {
            this.chooseLayout();
        }
        return layout.getSettings();
    }

	@Override
	public LayoutAlgorithm<? extends MethodGraph> getGraph() {
		if (this.layout == null) {
			chooseLayout();
		}
		return layout;
	}
}
