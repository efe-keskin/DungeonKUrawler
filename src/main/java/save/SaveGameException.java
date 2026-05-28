package save;

public class SaveGameException extends Exception {

    public SaveGameException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaveGameException(String message) {
        super(message);
    }
}
