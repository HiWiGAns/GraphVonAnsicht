package edu.kit.student.parameter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A compound object to store parameters.
 *
 * @author Lucas Steinmann, Nicolas Boltz
 */
public class Settings {

    private final Map<String, Parameter<?,?>> parameters;
    private final Map<String, Settings> subSettings;

    private String name;

    /**
     * Constructs a new Settings object and sets its parameters.
     * @param parameters the parameters the Settings object will have
     */
    public Settings(String name, List<Parameter<?,?>> parameters) {
        this.name = name;
        this.parameters = new HashMap<>();
        this.subSettings = new HashMap<>();
        for (Parameter<?, ?> parameter : parameters)
            this.parameters.put(parameter.getName(), parameter);
    }

    /**
     * Returns the amount of parameters in the Settings.
     * @return the amount of parameters in the Settings
     */
    public int size() {
        return parameters.size();
    }

    public boolean isEmpty() {
        return parameters.isEmpty() && subSettings.values().stream().allMatch(Settings::isEmpty);
    }

    /**
     * Returns the Parameter associated with the given key.
     * @param key the key which is associated with the Parameter
     * @return the Parameter associated with the given key
     */
    public Parameter<?,?> get(String key) {
        return parameters.get(key);
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
     * Returns all the Parameters in the Settings object.
     * @return all the Parameters in the Settings object.
     */
    public Collection<Parameter<?,?>> getParameters() {
        Set<Parameter<?, ?>> parameters = new HashSet<>();
        parameters.addAll(this.parameters.values());
        for (Settings set : subSettings.values())
            parameters.addAll(set.getParameters());
        return parameters;
    }

    public boolean containsParameter(String name) {
        return parameters.containsKey(name) || subSettings.values().stream().anyMatch(s -> s.containsParameter(name));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addSubSetting(Settings settings) {
        this.subSettings.put(settings.getName(), settings);
    }

}
