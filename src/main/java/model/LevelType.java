package model;

/**
 * Category of a tower floor. Drives spawn/difficulty strategy selection and
 * completion handling (boss floors unlock special rewards; the final boss
 * ends the tower). Static level metadata — see {@link DungeonLevel}.
 */
public enum LevelType {
    REGULAR,
    BOSS,
    FINAL_BOSS
}
