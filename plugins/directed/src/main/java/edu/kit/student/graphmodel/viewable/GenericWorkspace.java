package edu.kit.student.graphmodel.viewable;

import edu.kit.student.graphmodel.GraphModel;
import edu.kit.student.graphmodel.builder.IGraphModelBuilder;
import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.Workspace;
import edu.kit.student.util.LanguageManager;

import java.util.LinkedList;

public class GenericWorkspace implements Workspace {

	private Settings settings;
	private GenericGraphModelBuilder builder;
	private GenericGraphModel model;
	
	GenericWorkspace() {
		settings = new Settings(LanguageManager.getInstance().get("general"), new LinkedList<>());
		builder = new GenericGraphModelBuilder(this);
	}

	@Override
	public void initialize() { }

	@Override
	public IGraphModelBuilder getGraphModelBuilder() {
		return builder;
	}

	@Override
	public GraphModel getGraphModel() {
		return model;
	}
	
	/**
	 * Sets the specialized GraphModel in the workspace.
	 * 
	 * @param model the model that will be set.
	 */
	public void setGraphModel(GenericGraphModel model) {
		this.model = model;
	}

	@Override
	public Settings getSettings() {
		return settings;
	}
}
