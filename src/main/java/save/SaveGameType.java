package save;

/**
 * High-level save category used by the main menu to route saves to the right
 * game flow.
 */
public enum SaveGameType {
    SCENARIO_CHECKPOINT,
    SCENARIO_PROGRESS,
    CUSTOM_GAME;

    public static SaveGameType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SaveGameType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isScenario() {
        return this == SCENARIO_CHECKPOINT || this == SCENARIO_PROGRESS;
    }
}
