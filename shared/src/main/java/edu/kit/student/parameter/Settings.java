package edu.kit.student.parameter;

import java.util.LinkedList;
import java.util.List;

/**
 * A compound object to store parameters.
 *
 * @author Lucas Steinmann, Nicolas Boltz
 */
public class Settings {

    private final List<Parameter<?,?>> parameters;

    private final List<Settings> subSettings;

    private String name;

    /**
     * Constructs a new Settings object and sets its parameters.
     * @param parameters the parameters the Settings object will have
     */
    public Settings(String name, List<Parameter<?,?>> parameters) {
        this.name = name;
        this.parameters = new LinkedList<>(parameters);
        this.subSettings = new LinkedList<>();
    }

    /**
     * Returns the amount of parameters in the Settings.
     * @return the amount of parameters in the Settings
     */
    public int size() {
        return parameters.size() + subSettings.size();
    }

    public boolean isEmpty() {
        return parameters.isEmpty() && subSettings.stream().allMatch(Settings::isEmpty);
    }

    /**
     * Returns the Parameter associated with the given key.
     * @param key the key which is associated with the Parameter
     * @return the Parameter associated with the given key
     */
    public Parameter<?,?> get(String key) {
        return parameters.stream().filter(p -> p.getName().equals(key)).findFirst().orElse(null);
    }

    public static double unpackDouble(Parameter<?, Double> parameter) {
        return parameter.getValue();
    }

    public static int unpackInteger(Parameter<?, Integer> parameter) {
        return parameter.getValue();
    }

    public static boolean unpackBoolean(Parameter<?, Boolean> parameter) {
        return parameter.getValue();
    }

    /**
     * Returns all Parameters in the Settings object.
     * @return all Parameters in the Settings object.
     */
    public List<Parameter<?,?>> getParameters() {
        return new LinkedList<>(parameters);
    }

    public boolean containsParameter(String name) {
        return parameters.stream().anyMatch(p -> p.getName().equals(name));
    }

    public boolean containsParameterRecursively(String name) {
        return containsParameter(name)
                || subSettings.stream().anyMatch(s -> s.containsParameterRecursively(name));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addSubSetting(Settings settings) {
        subSettings.add(settings);
    }

    public List<Settings> getSubSettings() {
        return subSettings;
    }
}
