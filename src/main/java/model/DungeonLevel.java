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
        boolean fogHidden,
        boolean hasTrader,
        int rewardGold) {
}
