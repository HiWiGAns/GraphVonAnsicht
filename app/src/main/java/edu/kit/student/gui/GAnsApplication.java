package edu.kit.student.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import edu.kit.student.graphmodel.Graph;
import edu.kit.student.graphmodel.GraphModel;
import edu.kit.student.graphmodel.builder.IGraphModelBuilder;
import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.Exporter;
import edu.kit.student.plugin.Importer;
import edu.kit.student.plugin.LayoutOption;
import edu.kit.student.plugin.PluginManager;
import edu.kit.student.plugin.Workspace;
import edu.kit.student.plugin.WorkspaceOption;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * Main application of GAns.
 * 
 * @author Nicolas
 */
public class GAnsApplication extends Application {

	private StructureView structureView;
	private InformationView informationView;
	private TabPane graphViewTabPane;
	private Stage primaryStage;
	private MenuBar menuBar;
	
	private Workspace workspace;
	private GraphModel model;
	private LayoutOption layoutOption;

	private GraphView currentGraphView;
	// Mapped TabId zum Controller.
	private LinkedList<GraphViewEventHandler> graphViewEventHandler;
	private GraphViewSelectionModel graphViewSelectionModel;

	private File currentFile;
//	private MethodGraphLayout methodlayout = new MethodGraphLayout();

	/**
	 * Main method.
	 * 
	 * @param args
	 *            Arguments.
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		graphViewEventHandler = new LinkedList<GraphViewEventHandler>();

		this.primaryStage = primaryStage;
		primaryStage.setTitle("Graph von Ansicht - Graphviewer");

		VBox rootLayout = new VBox();
		Scene scene = new Scene(rootLayout, 800, 600);

		menuBar = new MenuBar();
		setupMenuBar();

		SplitPane treeInfoLayout = new SplitPane();
		treeInfoLayout.setOrientation(Orientation.VERTICAL);
		treeInfoLayout.setDividerPosition(0, 0.6);
		structureView = new StructureView();
		informationView = new InformationView();
		treeInfoLayout.getItems().addAll(structureView, informationView);
		
		structureView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		structureView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2) {
					structureView.getIdOfSelectedItem();
					//Model hinzufuegen von dem Model und heraussuchen des graphs. showGraph -> und graph an neue View uebergeben.
					//TODO: hier den passenden Sub/Methodengraphen finden und mit addTab oeffnen.
				}
			}
		});

		graphViewSelectionModel = new GraphViewSelectionModel();
		graphViewSelectionModel.getSelectedItems().addListener(new ListChangeListener<GAnsGraphElement>() {
			public void onChanged(Change<? extends GAnsGraphElement> changedItem) {
				ObservableList<GAnsGraphElement> selectedItems = graphViewSelectionModel.getSelectedItems();
				for (int i = 0; i < selectedItems.size(); i++) {
					GAnsGraphElement element = selectedItems.get(i);
					// TODO: Vertex oder Edge herausfinden und deren Properties
					// passend in eine observable list und an informationView.
				}
				// TODO: holen der selectierten Items, ueber currentGraphView
				// auf factory zugreifen
				// Factory bietet zugriff auf map (GAnsGraphElement,Vertex/Edge)
				// Properties der Vertex/Edge-Elemente speichern und an
				// informationView uebergeben.

				// informationView.setInformations(graphViewSelectionModel.getSelectedItems());
			}
		});

		SplitPane mainViewLayout = new SplitPane();
		mainViewLayout.setDividerPosition(0, 0.75);
		graphViewTabPane = new TabPane();
		graphViewTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
				//TODO: Boese :D
				Pane pane = ((Pane) newValue.getContent());
				Group group = ((Group) pane.getChildren().get(pane.getChildren().size() - 1));
				currentGraphView = ((GraphView) group.getChildren().get(group.getChildren().size() - 1));
			}
		});
		
		

		mainViewLayout.getItems().addAll(graphViewTabPane, treeInfoLayout);
		rootLayout.getChildren().addAll(menuBar, mainViewLayout);

		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	private void parseCommandLineArguments(String[] args) {
		
	}

	private void importClicked() {
		List<Importer> importerList = PluginManager.getPluginManager().getImporter();
		List<String> supportedFileExtensions = new ArrayList<String>();
		importerList.forEach((importer) -> supportedFileExtensions.add(importer.getSupportedFileEndings()));
		ExtensionFilter filter = new ExtensionFilter("Supported file extensions", supportedFileExtensions);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select a graph file");
		fileChooser.getExtensionFilters().add(filter);
		fileChooser.setSelectedExtensionFilter(filter);
		currentFile = fileChooser.showOpenDialog(primaryStage);
		if(currentFile == null) return;
		if(!openWorkspaceDialog()) return;
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(currentFile);
			String fileName = currentFile.getName();
			String fileExtension = "*" + fileName.substring(fileName.lastIndexOf('.'));
			Importer importer = importerList.get(supportedFileExtensions.indexOf(fileExtension));
			importer.importGraph(workspace.getGraphModelBuilder(), inputStream);
			this.model = workspace.getGraphModel();
			Graph currentGraph = this.model.getRootGraphs().get(0);
			openLayoutSelectionDialog(currentGraph);
			showGraph(currentGraph);
			this.structureView.showGraphModel(this.model);
		} catch (ParseException e) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setHeaderText(null);
			alert.setContentText(e.getMessage());
			alert.show();
			return;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	private void exportClicked() {
		List<Exporter> exporterList = PluginManager.getPluginManager().getExporter();
		List<String> supportedFileExtensions = new ArrayList<String>();
		exporterList.forEach((exporter) -> supportedFileExtensions.add(exporter.getSupportedFileEnding()));
		ExtensionFilter filter = new ExtensionFilter("Supported file extensions", supportedFileExtensions);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select an export location");
		fileChooser.getExtensionFilters().add(filter);
		fileChooser.setSelectedExtensionFilter(filter);
		File saveFile = fileChooser.showSaveDialog(primaryStage);
		try {
			FileOutputStream outputStream = new FileOutputStream(saveFile);
			String fileName = saveFile.getName();
			String fileExtension = "*" + fileName.substring(fileName.lastIndexOf('.'));
			Exporter exporter = exporterList.get(supportedFileExtensions.indexOf(fileExtension));
			exporter.exportGraph(currentGraphView.getFactory().getGraph().serialize(), outputStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void showGraph(Graph graph) {
		Group group = new Group();
		GraphView graphView = new GraphView();

		group.getChildren().add(graphView);
		// Die Oberflaeche die gezogen und gezoomed werden kann.
		Pane pane = new Pane(group);

		GraphViewEventHandler eventHandler = new GraphViewEventHandler(graphView);
		pane.addEventFilter(MouseEvent.MOUSE_PRESSED, eventHandler.getOnMousePressedEventHandler());
		pane.addEventFilter(MouseEvent.MOUSE_DRAGGED, eventHandler.getOnMouseDraggedEventHandler());
		pane.addEventFilter(ScrollEvent.ANY, eventHandler.getOnScrollEventHandler());

		// TODO: kann vermutlich weg
		graphViewEventHandler.add(eventHandler);

		graphView.setSelectionModel(graphViewSelectionModel);

		Tab tab = new Tab(graph.getName());
		tab.setContent(pane);
		graphViewTabPane.getTabs().add(tab);
		graphViewTabPane.getSelectionModel().select(tab);

		graphView.addGrid();
		graphView.setGraph(graph);
	}

	private void setupMenuBar() {
		Menu menuFile = new Menu("File");
		MenuItem importItem = new MenuItem("Import");
		importItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				importClicked();
			}
		});
		if(PluginManager.getPluginManager().getImporter().isEmpty()) {
			importItem.setDisable(true);
		}
		MenuItem exportItem = new MenuItem("Export");
		exportItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				exportClicked();
			}
		});
		if(PluginManager.getPluginManager().getExporter().isEmpty()) {
			exportItem.setDisable(true);
		}
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				System.exit(0);
			}
		});
		menuFile.getItems().addAll(importItem, exportItem, exitItem);

		Menu menuLayout = new Menu("Layout");
		Menu changeLayoutItem = new Menu("Change algorithms");
		
		menuLayout.setOnShowing(new EventHandler<Event>() {
			@Override
			public void handle(Event e) {
				changeLayoutItem.getItems().clear();
				if(currentGraphView != null) {
					for(LayoutOption option: currentGraphView.getFactory().getGraph().getRegisteredLayouts()) {
						MenuItem item = new MenuItem(option.getName());
						item.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent e) {
								option.chooseLayout();
								if(openParameterDialog(option.getSettings())) option.applyLayout();
							}
						});
						changeLayoutItem.getItems().add(item);
					}
				}
			}
		});
		MenuItem layoutPropertiesItem = new MenuItem("Properties");
		menuLayout.getItems().addAll(changeLayoutItem, layoutPropertiesItem);

		Menu menuView = new Menu("View");
		MenuItem testItem = new MenuItem("test");
		testItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
//				openTestDialog();
			}
		});
		menuView.getItems().add(testItem);

		menuBar.getMenus().addAll(menuFile, menuLayout, menuView);
	}

	private boolean openWorkspaceDialog() {
		List<String> workspaceNames = new ArrayList<String>();
		List<WorkspaceOption> options = PluginManager.getPluginManager().getWorkspaceOptions();
		options.forEach((option) -> workspaceNames.add(option.getName()));
		ChoiceDialog<String> dialog = new ChoiceDialog<String>(workspaceNames.get(0), workspaceNames);
		dialog.setTitle("Workspaces");
		dialog.setHeaderText(null);
		dialog.setGraphic(null);
		dialog.setContentText("Choose a workspace:");
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
		    WorkspaceOption chosenOption = options.get(workspaceNames.indexOf(result.get()));
		    if(openParameterDialog(chosenOption.getSettings())) {
		    	workspace = chosenOption.getInstance();
			    return true;
		    }
		}
		return false;
	}
	
	private boolean openLayoutSelectionDialog(Graph graph) {
		List<String> layoutNames = new ArrayList<String>();
		List<LayoutOption> options = graph.getRegisteredLayouts();
		options.forEach((option) -> layoutNames.add(option.getName()));
		ChoiceDialog<String> dialog = new ChoiceDialog<String>(layoutNames.get(0), layoutNames);
		dialog.setTitle("Layout Algorithms");
		dialog.setHeaderText(null);
		dialog.setGraphic(null);
		dialog.setContentText("Choose an layout algorithm:");
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
		    LayoutOption chosenOption = options.get(layoutNames.indexOf(result.get()));
		    chosenOption.chooseLayout();
		    if(openParameterDialog(chosenOption.getSettings())) chosenOption.applyLayout();
		    return true;
		}
		return false;
	}
	
	private void openLayoutSettingsDialog() {
		//TODO: Implementieren und nutzen
	}

	private boolean openParameterDialog(Settings settings) {
		GridPane root = new GridPane();
		ColumnConstraints c1 = new ColumnConstraints();
		ColumnConstraints c2 = new ColumnConstraints();
		c1.setPercentWidth(50);
		c2.setPercentWidth(50);
		root.getColumnConstraints().add(c1);
		root.getColumnConstraints().add(c2);
		new ParameterDialogGenerator(root, settings);
		Alert dialog = new Alert(AlertType.CONFIRMATION);
		dialog.setTitle("Settings");
		dialog.setHeaderText(null);
		dialog.setGraphic(null);
		dialog.getDialogPane().getChildren().add(root);
		Optional<ButtonType> result = dialog.showAndWait();
		if(result.get() != ButtonType.OK) {
			return false;
		}
		return true;
	}
}
