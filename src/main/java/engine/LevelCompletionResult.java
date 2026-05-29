package engine;

/**
 * Payload describing a completed tower floor, delivered to a
 * {@link LevelCompletionListener}. Carries just enough for the controller to
 * advance progress and for the view to react (e.g. show final victory); the
 * full level configuration is looked up from the scenario by level number.
 */
public record LevelCompletionResult(int levelNumber, boolean finalLevel) {
}
