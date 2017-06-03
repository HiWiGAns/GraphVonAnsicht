package edu.kit.student.sugiyama;

import edu.kit.student.graphmodel.directed.DirectedGraph;
import edu.kit.student.graphmodel.directed.DirectedGraphLayoutOption;
import edu.kit.student.parameter.Settings;

public class SugiyamaLayoutOption extends DirectedGraphLayoutOption {
	
	private SugiyamaLayoutAlgorithm<? extends DirectedGraph> algo;

	public SugiyamaLayoutOption() {
		this.setName("Sugiyama-Layout");
		this.setId("SUG");
	}

	@Override
	public void applyLayout() {
		this.algo.layout(graph);
	}

	@Override
	public Settings getSettings() {
		if (algo == null) {
			chooseLayout();
		}
		return algo.getSettings();
	}

	@Override
	public void chooseLayout() {
		this.algo = new SugiyamaLayoutAlgorithm<>();
	}

}
