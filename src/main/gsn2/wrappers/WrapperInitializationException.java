package gsn2.wrappers;

public class WrapperInitializationException extends RuntimeException{
    public WrapperInitializationException(String message) {
        super(message);
    }

    public WrapperInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrapperInitializationException(Throwable cause) {
        super(cause);
    }
}
