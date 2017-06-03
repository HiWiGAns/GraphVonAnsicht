package edu.kit.student.joana.callgraph;

import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.LayoutAlgorithm;
import edu.kit.student.sugiyama.SugiyamaLayoutAlgorithm;

/**
 * Offers a layout for {@link CallGraph}.
 * Groups vertices representing the same Java-Method together.
 */
public class CallGraphLayout extends LayoutAlgorithm<CallGraph> {
	private SugiyamaLayoutAlgorithm<CallGraph> sugiyamaLayoutAlgorithm;

	public CallGraphLayout() {
		this.sugiyamaLayoutAlgorithm = new SugiyamaLayoutAlgorithm<>();
	}

	@Override
	public Settings getSettings() {
		return sugiyamaLayoutAlgorithm.getSettings();
	}

	@Override
	public void layout(CallGraph graph) {
		//TODO: call SugiyamaLayoutAlgorithm layout(graph) after refactoring
		sugiyamaLayoutAlgorithm.layout(graph);
		
	}
}
