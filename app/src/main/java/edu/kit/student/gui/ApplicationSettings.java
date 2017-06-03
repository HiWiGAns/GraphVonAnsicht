package edu.kit.student.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ApplicationSettings {
	
	private static ApplicationSettings instance;
	
	private Properties properties;
	
	private File settingsFile = new File("settings.xml");
	
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
	
	public static ApplicationSettings getInstance() {
		if(instance == null) {
			instance = new ApplicationSettings();
		}
		
		return instance;
	}
	
	public void saveSettings() {
		try {
			OutputStream out = new FileOutputStream(settingsFile);
			properties.storeToXML(out, "");
		} catch (IOException e) {
			//settings not saved, no error
		}
	}
	
	public String getProperty(String key) {
		return properties.getProperty(key, "");
	}
	
	public double getPropertyAsDouble(String key) {
	    return getPropertyAsDouble(key, 0.0);
	}

	public double getPropertyAsDouble(String key, double defaultValue) {
		try {
			return Double.parseDouble(properties.getProperty(key));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
	
	public void setProperty(String key, double value) {
		properties.setProperty(key, Double.toString(value));
	}
	
	private void createDefaultSettings() throws IOException {
		setDefaultSettings();
		OutputStream out = new FileOutputStream(settingsFile);
		properties.storeToXML(out, "");
	}
	
	private void setDefaultSettings() {
		properties = new Properties();
		properties.setProperty("primary_full", "false");
		properties.setProperty("primary_width", "800");
		properties.setProperty("primary_height", "600");
		properties.setProperty("primary_x", "0");
		properties.setProperty("primary_y", "0");
		properties.setProperty("import_path", "C:\\\\");
		properties.setProperty("export_path", "C:\\\\");
		properties.setProperty("language_options", "en_EN;de_DE");
		properties.setProperty("language", "en_EN");
	}
}
