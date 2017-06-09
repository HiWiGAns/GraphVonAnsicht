package edu.kit.student.joana.callgraph;

import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.LayoutAlgorithm;
import edu.kit.student.plugin.LayoutOption;

/**
 * A {@link LayoutOption} which is specific for {@link CallGraph}.
 */
public abstract class CallGraphLayoutOption extends LayoutOption {

	private CallGraph graph;
	private LayoutAlgorithm<CallGraph> layout;

	/**
	 * Sets the {@link CallGraph} that will be the target of the
	 * CallGraphLayoutOption.
	 * 
	 * @param graph
	 *            The {@link CallGraph} that will be the target of this
	 *            CallGraphLayoutOption.
	 */
	public void setGraph(CallGraph graph) {
	    this.graph = graph;
	}

	/**
	 * Sets the LayoutAlgorithm that will be used to layout the set graph.
	 * 
	 * @param layout
	 *            The LayoutAlgorithm that will be used to layout the set graph.
	 */
	public void setLayout(LayoutAlgorithm<CallGraph> layout) {
		this.layout = layout;
	}

	@Override
	public void applyLayout() {
	    if (layout == null) {
	        return;
	    }
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
	public LayoutAlgorithm<? extends CallGraph> getLayout() {
		if (this.layout == null) {
			chooseLayout();
		}
		return layout;
	}
}
