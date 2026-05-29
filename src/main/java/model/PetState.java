package model;

/**
 * Lifecycle state of a {@link Pet}.
 *
 * <ul>
 *   <li>{@code UNEQUIPPED} — owned but not the active companion.</li>
 *   <li>{@code ACTIVE} — equipped and alive; spawns and uses abilities.</li>
 *   <li>{@code FAINTED} — equipped but {@code hp == 0}; stays owned but inert.</li>
 * </ul>
 */
public enum PetState {
    UNEQUIPPED,
    ACTIVE,
    FAINTED
}
