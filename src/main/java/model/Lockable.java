package model;

/**
 * Anything that can be in a locked state and opened by a matching {@link Key}.
 * Shared by {@link Container} and (future) {@code Gate} so the keyed-action
 * flow is not duplicated per type.
 */
public interface Lockable {

    boolean isLocked();

    void setLocked(boolean locked);

    /** Lock id the {@link Key#matches(String)} call is compared against. */
    String getRequiredKeyId();

    /**
     * Attempts to unlock this object with {@code key}.
     * @return true if the lock opened (or was already open).
     */
    default boolean unlockWith(Key key) {
        if (!isLocked()) {
            return true;
        }
        if (key == null) {
            return false;
        }
        if (!key.matches(getRequiredKeyId())) {
            return false;
        }
        setLocked(false);
        return true;
    }
}
