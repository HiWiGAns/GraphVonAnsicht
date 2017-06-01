package edu.kit.student.gui;

import edu.kit.student.parameter.BooleanParameter;
import edu.kit.student.parameter.DoubleParameter;
import edu.kit.student.parameter.IntegerParameter;
import edu.kit.student.parameter.MultipleChoiceParameter;
import edu.kit.student.parameter.Parameter;
import edu.kit.student.parameter.ParameterVisitor;
import edu.kit.student.parameter.Settings;
import edu.kit.student.parameter.StringParameter;
import edu.kit.student.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Generates a parameter dialog given a parent node and a set of parameters.
 * 
 * @author Lucas
 */
public class ParameterDialogGenerator extends ParameterVisitor {
	private GridPane parent;
	private int parameterCount = 0;

	private StringConverter<Integer> intConverter;
	private StringConverter<Double> doubleConverter;

	private List<Parameter<?, ?>> parameters;

	/**
	 * Constructs a new ParameterDialogGenerator and sets the parent, where all
	 * parameter GUI-Elements are placed in afterwards.
	 * 
	 * @param parent 
	 * @param settings
	 */
	public ParameterDialogGenerator(GridPane parent, Settings settings) {
		this.parent = parent;
		parent.setHgap(10);
		parent.setVgap(10);
		parent.setPadding(new Insets(10, 20, 10, 10));

		this.initIntConverter();
		this.initDoubleConverter();

		parameters = new LinkedList<Parameter<?, ?>>(settings.values());

		for (Parameter<?, ?> p : parameters) {
			p.accept(this);
		}
	}

	@Override
	public void visit(BooleanParameter parameter) {
		CheckBox box = new CheckBox(parameter.getName());
		box.setSelected(parameter.getValue());
		parameter.propertyValue().bind(box.selectedProperty());
		parameter.cacheCurrentValue();

		parent.add(box, 0, parameterCount);
		parameterCount++;
	}

	@Override
	public void visit(IntegerParameter parameter) {
		SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
				parameter.getMin(), parameter.getMax(), parameter.getValue());
		
		factory.setConverter(this.intConverter);

		Spinner<Integer> spinner = new Spinner<Integer>(factory);
		spinner.setEditable(true);
		parameter.propertyValue().bind(spinner.valueProperty());
		parameter.cacheCurrentValue();

		parent.add(new Text(parameter.getName()), 0, parameterCount);
		parent.add(spinner, 1, parameterCount);
		parameterCount++;
	}

	@Override
	public void visit(DoubleParameter parameter) {
		// The factories purpose is so that 3 decimals can be shown in the spinner, also it prevents illegal input.
		SpinnerValueFactory.DoubleSpinnerValueFactory factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(
				parameter.getMin(), parameter.getMax(), parameter.getValue(), parameter.getAmountPerStep());

		factory.setConverter(this.doubleConverter);

		Spinner<Double> spinner = new Spinner<Double>(factory);
		spinner.setEditable(true);
		parameter.propertyValue().bind(spinner.valueProperty());
		parameter.cacheCurrentValue();

		parent.add(new Text(parameter.getName()), 0, parameterCount);
		parent.add(spinner, 1, parameterCount);
		parameterCount++;
	}

	@Override
	public void visit(StringParameter parameter) {
		TextField field = new TextField(parameter.getValue());
		parameter.propertyValue().bind(field.textProperty());
		parameter.cacheCurrentValue();

		parent.add(new Text(parameter.getName()), 0, parameterCount);
		parent.add(field, 1, parameterCount);
		parameterCount++;
	}

	@Override
	public void visit(MultipleChoiceParameter parameter) {
		parent.add(new Text(parameter.getName()), 0, parameterCount);
		ComboBox<String> cmb = new ComboBox<String>();
		cmb.getItems().addAll(parameter.getChoices());

		cmb.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> p) {
				return new ListCell<String>() {
					{
					}

					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						this.setText(item);
					}
				};
			}
		});
		cmb.getSelectionModel().select(parameter.getSelectedIndex());
		parameter.propertyValue().bind(cmb.valueProperty());
		parameter.cacheCurrentValue();

		parent.add(cmb, 1, parameterCount);
		parameterCount++;
	}

	/**
	 * Creates a ParameterDialog for the supplied Settings.
	 * 
	 * @param settings
	 *            The settings for which the dialog will be created.
	 * @return true: Dialog was accepted, false: Dialog was aborted.
	 */
	public static boolean showDialog(Window owner, Settings settings) {
		if (settings.size() == 0) {
			// if there are no settings to be shown the dialog will
			// automatically be accepted
			return true;
		} else {
			GridPane root = new GridPane();
			ColumnConstraints c1 = new ColumnConstraints();
			ColumnConstraints c2 = new ColumnConstraints();
			c1.setPercentWidth(50);
			c2.setPercentWidth(50);
			root.getColumnConstraints().add(c1);
			root.getColumnConstraints().add(c2);
			ParameterDialogGenerator gen = new ParameterDialogGenerator(root, settings);
			Alert dialog = new Alert(AlertType.CONFIRMATION);
			dialog.initOwner(owner);
			dialog.setTitle(LanguageManager.getInstance().get("wind_prop_title"));
			dialog.setHeaderText(null);
			dialog.setGraphic(null);
			Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
	    	stage.getIcons().add(new Image("gans_icon.png"));
			dialog.getDialogPane().setContent(root);
			Optional<ButtonType> result = dialog.showAndWait();
			if (result.get() != ButtonType.OK) {
				gen.resetParameters();
				return false;
			}
			return true;
		}
	}

	private void resetParameters() {
		for (Parameter<?, ?> p : parameters) {
			p.propertyValue().unbind();
			p.reset();
		}
	}

	private void initIntConverter() {
		this.intConverter = new StringConverter<Integer>() {

			@Override
			public String toString(Integer value) {
				if (value == null)
					return "";
				return value.toString();
			}

			@Override
			public Integer fromString(String value) {
				try {
					if (value == null)
						return null;
					
					value = value.trim();
					
					if (value.length() < 1)
						return null;
					
					return Integer.parseInt(value);
				} catch (NumberFormatException ex) {
					return 0;
				}

			}
		};
	}

	private void initDoubleConverter() {
		this.doubleConverter = new StringConverter<Double>() {
			private final DecimalFormat df = new DecimalFormat("#.###");

			@Override
			public String toString(Double value) {
				// If the specified value is null, return a zero-length String
				if (value == null)
					return "";
				return df.format(value);
			}

			@Override
			public Double fromString(String value) {
				try {
					// If the specified value is null or zero-length, return
					// null
					if (value == null)
						return null;

					value = value.trim();

					if (value.length() < 1)
						return null;

					// Perform the requested parsing
					return df.parse(value).doubleValue();
				} catch (ParseException ex) {
					return 0.0d;
				}
			}
		};
	}
}
