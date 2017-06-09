package edu.kit.student.joana.callgraph;

import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.LayoutAlgorithm;
import edu.kit.student.sugiyama.SugiyamaLayoutAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Offers a layout for {@link CallGraph}.
 * Groups vertices representing the same Java-Method together.
 */
public class CallGraphLayout extends LayoutAlgorithm<CallGraph> {
	private SugiyamaLayoutAlgorithm<CallGraph> sugiyamaLayoutAlgorithm;
	private final Logger logger = LoggerFactory.getLogger(CallGraphLayout.class);

	public CallGraphLayout() {
		this.sugiyamaLayoutAlgorithm = new SugiyamaLayoutAlgorithm<>();
		fixesVertices().addListener(((observable, oldValue, newValue) -> {
			if (sugiyamaLayoutAlgorithm.fixesVertices().getValue() != newValue)
                sugiyamaLayoutAlgorithm.setFixesVertices(newValue);
		}));
		sugiyamaLayoutAlgorithm.fixesVertices().addListener(((observable, oldValue, newValue) -> {
			if (fixesVertices().getValue() != newValue)
				setFixesVertices(newValue);
		}));
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
