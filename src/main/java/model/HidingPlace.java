package model;

/**
 * Strategy role for "somewhere the mission system can stash a target item."
 * Containers and searchable scenery participate through small adapters, so
 * each implementation owns the details of receiving a hidden item.
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
