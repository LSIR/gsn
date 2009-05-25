package gsn.utils;

import gsn.beans.model.Parameter;

import java.util.HashMap;
import java.util.List;

public class ParameterValidityCheck {
    public static HashMap<String, String> isValidParameter(Parameter parameter) {
        HashMap toReturn = new HashMap();
        if (!parameter.getModel().isOptional() && parameter.getValue() == null)
            toReturn.put(parameter.getModel().getName(), "This parameter is mandatory.");
        return toReturn;
    }

    public static boolean isInstanceOfType(String value, String dataType) {
        return false;
    }

    public static HashMap<String, String> isValidParameters(List<Parameter> parameters) {
        HashMap toReturn = new HashMap();
        return toReturn;
    }
}
