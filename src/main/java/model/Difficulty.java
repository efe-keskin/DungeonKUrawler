package model;

/**
 * Difficulty band for a tower floor. Used by the level factory to pick an
 * enemy spawn policy (GoF Strategy) without scattering per-level branching
 * through the engine.
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD,
    VERY_HARD,
    BOSS
}
