package edu.kit.student.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class GAnsMediator extends Application {
	private GAnsApplication app;
	
	private Stage primaryStage;
	private Parameters params;
	
	public GAnsMediator() {
		this.app = new GAnsApplication(this);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		this.app.start(this.primaryStage, params == null ? getParameters() : params);
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	public GAnsApplication getGansApp() {
	    return app;
	}
	
	public void restart() {
		primaryStage.close();
		primaryStage = new Stage();
		app = new GAnsApplication(this);
		Platform.runLater( () -> app.start(primaryStage, null));
	}
}
