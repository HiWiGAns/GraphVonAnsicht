package edu.kit.student.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Controls persisting application settings with JavaFx Properties.
 *
 * @author Nicolas Boltz, Lucas Steinmann
 */
public class ApplicationSettings {
	
	private static ApplicationSettings instance;
	
	private final Properties properties;
	
	private final File settingsFile = new File("settings.xml");
	
	private ApplicationSettings() {
		properties = new Properties();
		try {
			InputStream in = new FileInputStream(settingsFile);
			properties.loadFromXML(in);
		} catch (FileNotFoundException e) {
			try {
				createDefaultSettings();
			} catch (IOException e1) {
				//settings could not be created default settings used.
				setDefaultSettings();
			}
		} catch (IOException e) {
			//settings could not be read default settings used.
			setDefaultSettings();
		}
	}

	/**
	 * Returns the singleton instance of the ApplicationSettings.
	 * @return the singleton instance
	 */
	public static ApplicationSettings getInstance() {
		if(instance == null) {
			instance = new ApplicationSettings();
		}
		
		return instance;
	}

	/**
	 * Saves the settings to the disk.
	 */
	public void saveSettings() {
		try {
			OutputStream out = new FileOutputStream(settingsFile);
			properties.storeToXML(out, "");
		} catch (IOException e) {
		    // TODO: Throw exception
		}
	}

	/**
	 * Returns the property with the specified key as a String.
	 * If no property was found an empty String is returned.
	 *
	 * @param key the key the property to get is associated with
	 * @return the property if found, otherwise a String of length 0
	 */
	public String getProperty(String key) {
		return properties.getProperty(key, "");
	}

	/**
	 * Returns the property with the specified key parsed to a Double.
	 * If no property was found 0.0d is returned.
	 *
	 * @param key the key the property to get is associated with
	 * @return the property if found, otherwise 0.0d
	 */
	public double getPropertyAsDouble(String key) {
	    return getPropertyAsDouble(key, 0.0);
	}

	/**
	 * Returns the property with the specified key parsed to a Double.
	 * If no property was found defaultValue is returned.
	 *
	 * @param key the key the property to get is associated with
     * @param defaultValue the value to return, when no property was found
	 * @return the property if found, otherwise defaultValue
	 */
	public double getPropertyAsDouble(String key, double defaultValue) {
		try {
			return Double.parseDouble(properties.getProperty(key));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Returns true if a property with the specified key exists in this settings.
	 * Otherwise false.
	 *
	 * @param key the key of the property
	 * @return true if the property was found, false otherwise
	 */
	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}

	/**
	 * Sets the property with the specified key to the specified string value.
	 * If no property exists with the specified key. A property is added.
	 * @param key the key of the property to set.
	 * @param value the value to set.
	 */
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	/**
	 * Sets the property with the specified key to the specified double value.
	 * If no property exists with the specified key. A property is added.
	 * @param key the key of the property to set.
	 * @param value the value to set.
	 */
	public void setProperty(String key, double value) {
		properties.setProperty(key, Double.toString(value));
	}

	/**
	 * Sets all properties in this settings to the default value
	 * and stores the properties on the disk.
	 * @throws IOException if the settings could not be written to disk.
	 */
	private void createDefaultSettings() throws IOException {
		setDefaultSettings();
		OutputStream out = new FileOutputStream(settingsFile);
		properties.storeToXML(out, "");
	}
	
	private void setDefaultSettings() {
	    properties.clear();
		setProperty("primary_full", "false");
		setProperty("primary_width", 800);
		setProperty("primary_height", 600);
		setProperty("primary_x", 0);
		setProperty("primary_y", 0);
		setProperty("import_path", "C:\\\\");
		setProperty("export_path", "C:\\\\");
		setProperty("language_options", "en_EN;de_DE");
		setProperty("language", "en_EN");
		setProperty("filterdialog_width", 500);
		setProperty("filterdialog_height", 500);
	}
}
