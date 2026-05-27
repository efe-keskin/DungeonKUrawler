package model;

/**
 * Strategy role for "somewhere the mission system can stash a target item."
 * Today only chests/containers implement this; once searchable scenery
 * (vases, pedestals, missing bricks, ...) gains the ability to conceal loot,
 * those classes adopt the same interface and become drop targets automatically.
 *
 * <p>Information Expert: each implementation knows whether it has room and
 * how to absorb the item — the mission code never touches container internals.
 */
public interface HidingPlace {

    /** True when this place still has room to receive {@code item}. */
    boolean canHide(Item item);

    /**
     * Tries to stash {@code item}. Returns true on success; false if the
     * place was unexpectedly unavailable (race with another mutation, etc.).
     */
    boolean hide(Item item);

    /** Short label used for debug logging. */
    String describe();
}
