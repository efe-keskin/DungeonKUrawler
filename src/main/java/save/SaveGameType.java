package save;

/**
 * High-level save category used by the main menu to route saves to the right
 * game flow.
 */
public enum SaveGameType {
    /** A named scenario/tower playthrough: long-term progression plus embedded per-level saves. */
    SCENARIO,
    /** A standalone custom-map game, saved independently of scenario progression. */
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
}
