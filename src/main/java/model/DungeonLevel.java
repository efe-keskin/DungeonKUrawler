package model;

/**
 * Immutable static configuration for a single tower floor (GRASP Information
 * Expert: a level knows its own fixed metadata). Mutable progression — locked /
 * unlocked / completed — is owned by {@link TowerProgress}, not here.
 *
 * <p>Created as a record so the factory can read configuration via the
 * generated accessors ({@code difficulty()}, {@code levelType()}, ...).
 */
public record DungeonLevel(
        int levelNumber,
        String levelName,
        LevelType levelType,
        Difficulty difficulty,
        /**
         * Controls whether this floor uses the Fear-of-the-Dark visibility fog.
         * Kept as {@code fogHidden} because this record component is already
         * shared across tower setup code.
         */
        boolean fogHidden,
        boolean hasTrader,
        int rewardGold) {
}
