package gsn.utils;

import gsn.beans.model.Parameter;

import java.util.List;

public class EasyParamWrapper {
    private List<Parameter> parameters;

    public EasyParamWrapper(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public String getPredicateValue(String key) {
        key = key.trim();
        for (Parameter predicate : this.parameters)
            if (predicate.getModel().getName().trim().equalsIgnoreCase(key)) return predicate.getValue();
        return null;
    }

    /**
     * Gets a parameter name. If the parameter value exists and is not an empty string, returns the value otherwise returns the
     * default value
     *
     * @param key          The key to look for in the map.
     * @param defaultValue Will be return if the key is not present or its an empty string.
     * @return
     */
    public String getPredicateValueWithDefault(String key, String defaultValue) {
        String value = getPredicateValue(key);
        if (value == null || value.trim().length() == 0)
            return defaultValue;
        else
            return value;
    }

    /**
     * Gets a parameter name. If the parameter value exists and is a valid integer, returns the value otherwise returns the
     * default value
     *
     * @param key          The key to look for in the map.
     * @param defaultValue Will be return if the key is not present or its value is not a valid integer.
     * @return
     */
    public int getPredicateValueAsInt(String key, int defaultValue) {
        String value = getPredicateValue(key);
        if (value == null || value.trim().length() == 0)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getPredicateValueAsIntWithException(String key) {
        String value = getPredicateValue(key);
        if (value == null || value.trim().length() == 0)
            throw new RuntimeException("The required parameter: >" + key + "<+ is missing.from the virtual sensor configuration file.");
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new RuntimeException("The required parameter: >" + key + "<+ is bad formatted.from the virtual sensor configuration file.", e);
        }
    }


    public boolean getPredicateValueAsBoolean(String key, boolean defaultValue) {
        String value = getPredicateValue(key);
        if (value == null || value.trim().length() == 0)
            return defaultValue;
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
