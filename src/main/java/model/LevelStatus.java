package model;

/**
 * Per-player progression state of a tower floor, owned by {@link TowerProgress}.
 *
 * <ul>
 *   <li>{@code LOCKED} — known to exist but not yet enterable.</li>
 *   <li>{@code UNLOCKED} — enterable now.</li>
 *   <li>{@code COMPLETED} — cleared.</li>
 *   <li>{@code HIDDEN} — concealed by fog; existence/details not revealed yet.</li>
 * </ul>
 */
public enum LevelStatus {
    LOCKED,
    UNLOCKED,
    COMPLETED,
    HIDDEN
}
