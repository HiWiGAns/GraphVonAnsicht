package edu.kit.student.gui;

import edu.kit.student.graphmodel.GraphModel;
import edu.kit.student.graphmodel.VertexReference;
import edu.kit.student.graphmodel.ViewableGraph;
import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.parameter.Settings;
import edu.kit.student.plugin.Exporter;
import edu.kit.student.plugin.Importer;
import edu.kit.student.plugin.LayoutOption;
import edu.kit.student.plugin.PluginManager;
import edu.kit.student.plugin.Workspace;
import edu.kit.student.plugin.WorkspaceOption;
import edu.kit.student.util.LanguageManager;
import javafx.application.Application.Parameters;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main application of GAns.
 * 
 * @author Nicolas
 */
public class GAnsApplication {

	private StructureView structureView;
	private InformationView informationView;
	private Stage primaryStage;
	private MenuBar menuBar;
	private GraphTabPaneController graphPaneController;

	private Workspace workspace;
	private GraphModel model;

	private File currentImportPath;
	private File currentExportPath;
	private GAnsMediator mediator;

	private final Logger logger = LoggerFactory.getLogger(GAnsApplication.class);

	public GAnsApplication(GAnsMediator mediator) {
		this.mediator = mediator;
	}

	public void start(Stage primaryStage, Parameters parameters) {
		LanguageManager.getInstance().setLanguage(ApplicationSettings.getInstance().getProperty("language"));
		this.primaryStage = primaryStage;
		primaryStage.setTitle(LanguageManager.getInstance().get("wind_title"));

		VBox rootLayout = new VBox();
		Scene scene = new Scene(rootLayout, 800, 600);

		menuBar = new MenuBar();
		menuBar.setId("Menubar");
		setupMenuBar();


		SplitPane treeInfoLayout = new SplitPane();
		treeInfoLayout.setOrientation(Orientation.VERTICAL);
		treeInfoLayout.setDividerPosition(0, 0.6);
		structureView = new StructureView(this);
		structureView.setId("StructureView");
		informationView = new InformationView();
		informationView.setId("InformationView");
		treeInfoLayout.getItems().addAll(structureView, informationView);
		this.graphPaneController = new GraphTabPaneController(this);

		SplitPane mainViewLayout = new SplitPane();
		mainViewLayout.setDividerPosition(0, 0.75);
		mainViewLayout.getItems().addAll(graphPaneController.getTabPane(), treeInfoLayout);
		rootLayout.getChildren().addAll(menuBar, mainViewLayout);
		VBox.setVgrow(mainViewLayout, Priority.ALWAYS);
		
		primaryStage.setOnCloseRequest(event -> saveSettings());

		primaryStage.getIcons().add(new Image("gans_icon.png"));
		primaryStage.setScene(scene);
		loadSettings();
		primaryStage.show();
		
		if (parameters != null) parseCommandLineArguments(parameters);

		this.primaryStage.getScene().focusOwnerProperty().addListener((observable, oldValue, newValue) -> {
			if (structureView.equals(newValue))
				informationView.setFocus(structureView.getInformation());
			else if (graphPaneController.getTabPane().equals(newValue))
				informationView.setFocus(graphPaneController.getTabPane().getInformation());
		});
	}

	public void importFile(File file) {
		if(file == null) return;
		if(!openWorkspaceDialog()) return;
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			String fileName = file.getName();
			String fileExtension = "*" + fileName.substring(fileName.lastIndexOf('.'));
			List<Importer> importerList = PluginManager.getPluginManager().getImporter();
			List<String> supportedFileExtensions = new ArrayList<>();
			importerList.forEach((importer) -> supportedFileExtensions.add(importer.getSupportedFileEndings()));
			Importer importer = importerList.get(supportedFileExtensions.indexOf(fileExtension));
			importer.importGraph(workspace.getGraphModelBuilder(), inputStream);
			this.graphPaneController.getTabPane().getTabs().clear();
			this.informationView.setFocus(FXCollections.observableList(new LinkedList<>()));
			this.model = workspace.getGraphModel();
			ViewableGraph currentGraph = this.model.getRootGraphs().get(0);
			this.graphPaneController.openGraph(currentGraph);
			this.structureView.showGraphModel(this.model);

		} catch (ParseException e) {
			this.graphPaneController.getTabPane().getTabs().clear();
			showErrorDialog(e.getMessage() + "\nat\n" + Arrays.toString(e.getStackTrace()));
		}  catch (IOException e) {
			this.graphPaneController.getTabPane().getTabs().clear();
			showErrorDialog(e.getMessage());
		}

	}

	/**
	 * Opens the specified graph in the main view.
	 * Creates a new tab if the graph is not already opened in an existing tab.
	 * Otherwise selects the tab containing the specified graph.
     *
	 * @param graph the graph to open
	 */
	public void openGraph(ViewableGraph graph) {
		this.graphPaneController.openGraph(graph);
	}

	public void navigateTo(VertexReference vertexReference) {
	    openGraph(vertexReference.getGraph());
        ViewableVertex vertex = vertexReference.getTarget();
        if (vertex != null) {
            VertexShape shape = this.graphPaneController.getGraphView().getFactory().getShapeFromVertex(vertex);
            this.graphPaneController.graphViewPanesProperty.getValue().runAfterLayout(() -> {
				this.graphPaneController.graphViewPanesProperty.getValue().center(
						new Point2D(vertex.getX() + shape.getWidth() / 2, vertex.getY() + shape.getHeight() / 2)
				);
			});
        }
	}


	private void loadSettings() {
		this.primaryStage.setWidth(ApplicationSettings.getInstance().getPropertyAsDouble("primary_width"));
		this.primaryStage.setHeight(ApplicationSettings.getInstance().getPropertyAsDouble("primary_height"));
		this.primaryStage.setX(ApplicationSettings.getInstance().getPropertyAsDouble("primary_x"));
		this.primaryStage.setY(ApplicationSettings.getInstance().getPropertyAsDouble("primary_y"));
		this.primaryStage.setFullScreen(Boolean.parseBoolean(ApplicationSettings.getInstance().getProperty("primary_full")));
		
		this.currentImportPath = new File(ApplicationSettings.getInstance().getProperty("import_path"));
		this.currentExportPath = new File(ApplicationSettings.getInstance().getProperty("export_path"));
	}
	
	private void saveSettings() {
		if(this.primaryStage.isFullScreen()) {
			ApplicationSettings.getInstance().setProperty("primary_full", "true");
		} else {
			ApplicationSettings.getInstance().setProperty("primary_full", "false");
			ApplicationSettings.getInstance().setProperty("primary_width", this.primaryStage.getWidth());
			ApplicationSettings.getInstance().setProperty("primary_height", this.primaryStage.getHeight());
			ApplicationSettings.getInstance().setProperty("primary_x", this.primaryStage.getX());
			ApplicationSettings.getInstance().setProperty("primary_y", this.primaryStage.getY());
		}
		
		ApplicationSettings.getInstance().setProperty("import_path", currentImportPath.getAbsolutePath());
		ApplicationSettings.getInstance().setProperty("export_path", currentExportPath.getAbsolutePath());
		
		ApplicationSettings.getInstance().saveSettings();
	}
	
	private void parseCommandLineArguments(Parameters params) {
	    Map<String, String> namedParams = params.getNamed();
	    if (namedParams.isEmpty()) return;
	    String filename = "";
	    String layout = "";
	    String ws = "";
	    Importer importer = null;
	    Workspace tempWorkspace = null;
	    
	    for (String key : namedParams.keySet()) {
	        switch (key) {
	          case "in":
	              filename = namedParams.get(key);
	              if (!filename.contains(".")) {
	            	  showErrorDialog(String.format(LanguageManager.getInstance().get("err_comm_filename_invalid"), filename));
	                  return;
	              }
	              String extension = "*" + filename.substring(filename.lastIndexOf('.'));
	              List<Importer> importerList = PluginManager.getPluginManager().getImporter();
	              for (Importer temp : importerList) {
                      if (extension.equals(temp.getSupportedFileEndings())) {
                          importer = temp;
                          break;
                      }     
	              }
	              break;
	          case "layout":
	              layout = namedParams.get(key);
	              break;
	          case "ws":
	              ws = namedParams.get(key);
	              List<WorkspaceOption> options = PluginManager.getPluginManager().getWorkspaceOptions();
	              for (WorkspaceOption option : options) {
	                  if (ws.equals(option.getId())){
	                      tempWorkspace = option.getInstance();
	                      break;
	                  }
	              }
	              break;
	          default:
	              // Information not specified
	        	  showErrorDialog(String.format(LanguageManager.getInstance().get("err_comm_argument_unkn"), key, namedParams.get(key)));
	        }
	    }
	    
        if (filename.equals("")) {
            showErrorDialog(LanguageManager.getInstance().get("err_comm_filename_missing"));
            return;
        } else if (importer == null) {
            String extension = "*" + filename.substring(filename.lastIndexOf('.'));
            showErrorDialog(String.format(LanguageManager.getInstance().get("err_comm_extension_unkn"), extension));
            return;
        }
	    
	    //import graph
        File importFile = new File(filename);
        currentImportPath = importFile.getParentFile();
        //check if workspace is in arguments
        if (tempWorkspace != null) {
            this.workspace = tempWorkspace;
        } else {
            if (!ws.equals("")) {
            	showErrorDialog(String.format(LanguageManager.getInstance().get("err_comm_workspace_unkn"), ws));
            }
            if(!openWorkspaceDialog()) return;
        }
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(importFile);
            importer.importGraph(workspace.getGraphModelBuilder(), inputStream);
            this.model = workspace.getGraphModel();
            ViewableGraph currentGraph = this.model.getRootGraphs().get(0);

			LayoutOption selectedLayout = null;

            List<LayoutOption> options = currentGraph.getRegisteredLayouts();
            for (LayoutOption option : options) {
                if (layout.equals(option.getId())) {
                    //found valid layout and apply layout
                    selectedLayout = option;
                }
            }  

            if (selectedLayout == null) {
                if (!Objects.equals(layout, "")) {
                	showErrorDialog(String.format(LanguageManager.getInstance().get("err_comm_layout_unkn"), layout));
                }
                selectedLayout = currentGraph.getDefaultLayout();
            }

            //open graph
			selectedLayout.chooseLayout();
            selectedLayout.applyLayout();
            graphPaneController.openGraph(currentGraph);
            graphPaneController.getGraphView().setCurrentLayoutOption(selectedLayout);
            
            this.structureView.showGraphModel(this.model);
        } catch (ParseException e) {
			showErrorDialog(e.getMessage() + "\nat\n" 
					+ Arrays.stream(e.getStackTrace())
							.map(st -> st.getClassName() + ": " + st.getMethodName() + "(" + st.getLineNumber() + ")")
							.reduce("", (s,n) -> s + n + "\n"));
        } catch (IOException e) {
            showErrorDialog(e.getMessage());
        }
		
	}

	private void importClicked() {
		List<Importer> importerList = PluginManager.getPluginManager().getImporter();
		List<String> supportedFileExtensions = new ArrayList<>();
		importerList.forEach((importer) -> supportedFileExtensions.add(importer.getSupportedFileEndings()));
		ExtensionFilter filter = new ExtensionFilter(LanguageManager.getInstance().get("wind_imp_ext"), supportedFileExtensions);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(LanguageManager.getInstance().get("wind_imp_title"));
		fileChooser.getExtensionFilters().add(filter);
		fileChooser.setSelectedExtensionFilter(filter);
		if(currentImportPath.exists()) {
			fileChooser.setInitialDirectory(currentImportPath);
		}
		File tmp = fileChooser.showOpenDialog(primaryStage);
		if(tmp != null) {
			currentImportPath = tmp.getParentFile();
			importFile(tmp);
		}
	}


	private void exportClicked() {
		Map<ExtensionFilter, Exporter> supportedFileExtensions = new HashMap<>();
		for (Exporter exporter : PluginManager.getPluginManager().getExporter()) {
		        StringBuilder description = new StringBuilder(exporter.getFileEndingDescription() + " (");
                exporter.getSupportedFileEndings().forEach((fe) -> description.append("*.").append(fe).append(", "));
                description.delete(description.length() - 2, description.length());
                description.append(")");
		        supportedFileExtensions.put(new FileChooser.ExtensionFilter( 
		                description.toString(), 
		                exporter.getSupportedFileEndings().stream()
		                                                  .map((fe) -> "*." + fe)
		                                                  .collect(Collectors.toList())),
		                exporter);
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(LanguageManager.getInstance().get("wind_exp_title"));
		if(currentExportPath.exists()) {
			fileChooser.setInitialDirectory(currentExportPath);
		}
		supportedFileExtensions.keySet().forEach((filter) -> fileChooser.getExtensionFilters().add(filter));
		File tmp = fileChooser.showSaveDialog(primaryStage);

		// Wait for user to select file
		if (tmp != null) {
			currentExportPath = tmp.getParentFile();
            String fileExtension = tmp.getName().substring(tmp.getName().lastIndexOf('.') + 1);
            try {
                FileOutputStream outputStream = new FileOutputStream(tmp);
                Exporter exporter = supportedFileExtensions.get(fileChooser.getSelectedExtensionFilter());
                try {
                    exporter.exportGraph(this.graphPaneController.getGraphView().getFactory().serializeGraph(),
							outputStream,
							fileExtension);
                } catch (IllegalArgumentException e) {
                    showErrorDialog(e.getMessage());
                } catch (Exception e) {
                    showErrorDialog(LanguageManager.getInstance().get("err_exp"));
                }
            } catch (FileNotFoundException e) {
                showErrorDialog(e.getMessage());
            }
		}
	}

	private boolean openWorkspaceDialog() {
		List<String> workspaceNames = new ArrayList<>();
		List<WorkspaceOption> options = PluginManager.getPluginManager().getWorkspaceOptions();
		options.forEach((option) -> workspaceNames.add(option.getName()));
		ChoiceDialog<String> dialog = new ChoiceDialog<>(workspaceNames.get(0), workspaceNames);
		dialog.setTitle(LanguageManager.getInstance().get("wind_workspace_title"));
		dialog.setHeaderText(null);
		dialog.setGraphic(null);
		dialog.setContentText(LanguageManager.getInstance().get("wind_workspace_text"));
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
    	stage.getIcons().add(new Image("gans_icon.png"));
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
		    WorkspaceOption chosenOption = options.get(workspaceNames.indexOf(result.get()));
		    if(ParameterDialogGenerator.showDialog(primaryStage.getScene().getWindow(), chosenOption.getSettings())) {
		    	workspace = chosenOption.getInstance();
			    return true;
		    }
		}
		return false;
	}

//	private boolean openLayoutSelectionDialog(ViewableGraph graph) {
//		List<String> layoutNames = new ArrayList<String>();
//		List<LayoutOption> options = graph.getRegisteredLayouts();
//		options.forEach((option) -> layoutNames.add(option.getName()));
//		ChoiceDialog<String> dialog = new ChoiceDialog<String>(layoutNames.get(0), layoutNames);
//		dialog.setTitle(LanguageManager.getInstance().get("wind_layoutsel_title"));
//		dialog.setHeaderText(null);
//		dialog.setGraphic(null);
//		dialog.setContentText(LanguageManager.getInstance().get("wind_layoutsel_text"));
//		Optional<String> result = dialog.showAndWait();
//		if (result.isPresent()) {
//		    LayoutOption chosenOption = options.get(layoutNames.indexOf(result.get()));
//		    chosenOption.chooseLayout();
//		    Settings settings = chosenOption.getSettings();
//		    if(ParameterDialogGenerator.showDialog(settings)) {
//		    	currentGraphView.setCurrentLayoutOption(chosenOption);
//		    	chosenOption.applyLayout();
//		    }
//		    return true;
//		}
//		return false;
//	}

	private void openLayoutSettingsDialog(LayoutOption option) {
		Settings settings = option.getSettings();
	    if(ParameterDialogGenerator.showDialog(primaryStage.getScene().getWindow(), settings)) {
	    	graphPaneController.getGraphView().setCurrentLayoutOption(option);
	    	option.applyLayout();
	    }
	}

	private void setupMenuBar() {
		Menu menuFile = new Menu(LanguageManager.getInstance().get("mnu_file"));
		MenuItem importItem = new MenuItem(LanguageManager.getInstance().get("mnu_file_import"));
		importItem.setOnAction(e -> importClicked());
		if(PluginManager.getPluginManager().getImporter().isEmpty()) {
			importItem.setDisable(true);
		}
		MenuItem exportItem = new MenuItem(LanguageManager.getInstance().get("mnu_file_export"));
		exportItem.setOnAction(e -> exportClicked());
		if(PluginManager.getPluginManager().getExporter().isEmpty()) {
			exportItem.setDisable(true);
		}
		MenuItem exitItem = new MenuItem(LanguageManager.getInstance().get("mnu_file_exit"));
		exitItem.setOnAction(e -> {
            saveSettings();
            Platform.exit();
        });
		
		
		menuFile.getItems().addAll(importItem, exportItem, exitItem);
		
		// disabling the export button if there is no graphView
		menuFile.setOnShowing(e -> exportItem.setDisable(graphPaneController.getGraphView() == null));

		Menu menuLayout = new Menu(LanguageManager.getInstance().get("mnu_layout"));
		Menu changeLayoutItem = new Menu(LanguageManager.getInstance().get("mnu_layout_change"));
		MenuItem layoutPropertiesItem = new MenuItem(LanguageManager.getInstance().get("mnu_layout_prop"));
		layoutPropertiesItem.setOnAction(e -> {
            openLayoutSettingsDialog(graphPaneController.getGraphView().getCurrentLayoutOption());
            graphPaneController.getGraphView().reloadGraph();
        });
		menuLayout.getItems().addAll(changeLayoutItem, layoutPropertiesItem);
		
		menuLayout.setOnShowing(e -> {
            // disabling the change and layout properties button if there is no graphview
            if(graphPaneController.getGraphView() == null) {
                changeLayoutItem.setDisable(true);
                layoutPropertiesItem.setDisable(true);
            } else {
                changeLayoutItem.setDisable(false);
                if(graphPaneController.getGraphView().getCurrentLayoutOption().getSettings().size() == 0) {
                    layoutPropertiesItem.setDisable(true);
                } else {
                    layoutPropertiesItem.setDisable(false);
                }
                changeLayoutItem.getItems().clear();
                for(LayoutOption option: graphPaneController.getGraphView().getFactory().getGraph().getRegisteredLayouts()) {
                    MenuItem item = new MenuItem(option.getName());
                    item.setOnAction(e1 -> {
                        option.chooseLayout();
                        openLayoutSettingsDialog(option);
                        graphPaneController.getGraphView().reloadGraph();
                    });
                    changeLayoutItem.getItems().add(item);
                }
            }
        });
		
		Menu menuOther = new Menu(LanguageManager.getInstance().get("mnu_other"));
		MenuItem groupItem = new MenuItem(LanguageManager.getInstance().get("mnu_other_groups"));
		groupItem.setOnAction(e -> graphPaneController.getGraphView().openGroupDialog());
		
		MenuItem filterItem = new MenuItem(LanguageManager.getInstance().get("mnu_other_filter"));
		filterItem.setOnAction(event -> graphPaneController.getGraphView().openFilterDialog());
		
		Menu menuLanguage = new Menu(LanguageManager.getInstance().get("mnu_other_lang"));
		
		String languageOptions = ApplicationSettings.getInstance().getProperty("language_options");
		List<String> languages = Arrays.asList(languageOptions.split(";"));
		for(String language : languages) {
			String[] lang = language.split("_");
			Locale loc = new Locale(lang[0], lang[1]);
			MenuItem languageItem = new MenuItem(loc.getCountry());
			
			menuLanguage.getItems().add(languageItem);
			
			languageItem.setOnAction(event -> showChangeLanguageDialog(language));
		}
		
		
		menuOther.getItems().addAll(groupItem, filterItem, menuLanguage);
		
		// disabling the groups button if there is no graphView
		menuOther.setOnShowing(e -> {
            if(graphPaneController.getGraphView() == null) {
                groupItem.setDisable(true);
                filterItem.setDisable(true);
            } else {
                groupItem.setDisable(false);
                filterItem.setDisable(false);
            }

        });

		menuBar.getMenus().addAll(menuFile, menuLayout, menuOther);
	}

	private void showChangeLanguageDialog(String newLanguage) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setHeaderText(null);
		alert.setContentText(LanguageManager.getInstance().get("err_changelng"));
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    	stage.getIcons().add(new Image("gans_icon.png"));
		Optional<ButtonType> result = alert.showAndWait();
		if(result.isPresent()) {
			if(result.get() == ButtonType.OK) {
				ApplicationSettings.getInstance().setProperty("language", newLanguage);
				saveSettings();
				mediator.restart();
			}
		}
	}
	
	private void showErrorDialog(String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setHeaderText(null);
		alert.setContentText(message);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    	stage.getIcons().add(new Image("gans_icon.png"));
		alert.showAndWait();
	}

}
