package engine;

/**
 * Shared timing constants for hero and enemy combat pacing.
 */
public final class GameConstants {

    /** Global action rhythm shared by hero attacks and enemy attack ticks (ms). */
    public static final int GLOBAL_ACTION_TICK_MS = 800;
    /** Slower cadence prevents multiple Sorcerers from flooding the map. */
    public static final int SORCERER_ATTACK_TICK_MS = 9000;

    private GameConstants() {
    }
}
