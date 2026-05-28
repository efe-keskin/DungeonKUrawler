package save;

public final class SaveLimitExceededException extends SaveGameException {

    public SaveLimitExceededException(int maxSaves) {
        super("You can keep at most " + maxSaves + " saved games.");
    }
}
