package gsn.operators;

public class MandatoryParameterMissingException extends RuntimeException{
    public MandatoryParameterMissingException(String paramName) {
        super("The mandatory parameter: "+paramName+" is missing.");
    }
}
