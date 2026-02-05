package errors;

public class IdAlreadyExistsException extends RuntimeException {
    public IdAlreadyExistsException(String message) {
        super(message);
    }
}
