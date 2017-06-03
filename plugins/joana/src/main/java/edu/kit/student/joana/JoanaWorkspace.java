package edu.kit.student.joana;

import edu.kit.student.graphmodel.builder.IGraphModelBuilder;
import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.Workspace;
import edu.kit.student.util.LanguageManager;

import java.util.LinkedList;

/**
 * The {@link JoanaWorkspace} is the workspace for Joana graphs. It is used to
 * define parameters, provides an {@link IGraphModelBuilder} and contains a
 * {@link JoanaGraphModel}.
 */
public class JoanaWorkspace implements Workspace {
	
	private Settings settings;
	private JoanaGraphModelBuilder builder;
	private JoanaGraphModel model;

	public JoanaWorkspace() {
		settings = new Settings(LanguageManager.getInstance().get("joana_workspace"), new LinkedList<>());
		
		builder = new JoanaGraphModelBuilder(this);
	}
	
	@Override
	public void initialize() { }

	@Override
	public JoanaGraphModelBuilder getGraphModelBuilder() {
		return builder;
	}
	
	/**
	 * Sets the specialized GraphModel in the workspace.
	 * Should only be called from inside of the builder!
	 * 
	 * @param model The model that will be set.
	 */
	public void setGraphModel(JoanaGraphModel model) {
		this.model = model;
	}

	@Override
	public JoanaGraphModel getGraphModel() {
		return model;
	}

	@Override
	public Settings getSettings() {
		return settings;
	}
}
