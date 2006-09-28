package gsn.utils;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class GSNRuntimeException extends RuntimeException {
    private int type = 0;

    public static final int UNEXPECTED_VIRTUAL_SENSOR_REMOVAL = 1;

    public static final int UNSPECIFIED_EXCEPTION = 0;

    public GSNRuntimeException(String message) {
	super(message);
    }

    public GSNRuntimeException(String message, int type) {
	super(message);
	this.type = type;
    }

    /**
         * @return the type of the exception. If the value is 0 it means there
         *         is no type specified for the exception.
         */
    public int getType() {
	return type;
    }

}
