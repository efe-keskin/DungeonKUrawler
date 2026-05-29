package engine;

/**
 * GoF Observer: lets the tower controller learn that the active floor's win
 * condition has been met, instead of the view polling the engine for victory.
 * {@link GameEngine} is the subject; {@code TowerProgressController} is the
 * typical subscriber.
 */
public interface LevelCompletionListener {

    /** Fired once when the active tower floor is completed. */
    void onLevelCompleted(LevelCompletionResult result);
}
