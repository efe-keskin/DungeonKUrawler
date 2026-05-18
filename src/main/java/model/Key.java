package model;

/**
 * Inventory item used to unlock {@link Lockable}s (chests, gates).
 *
 * <p>The {@code keyId} is the real matcher — it is compared verbatim to a
 * lock's {@code requiredKeyId}. {@link KeyColor} is presentation only.
 *
 * <p>{@code singleUse} keys are consumed on the first successful unlock;
 * reusable keys stay in the inventory.
 */
public class Key extends Item {

    private final String keyId;
    private final KeyColor color;
    private final boolean singleUse;

    public Key(String keyId, KeyColor color, boolean singleUse) {
        super(color.displayName());
        this.keyId = keyId;
        this.color = color;
        this.singleUse = singleUse;
    }

    /** Reusable key convenience constructor. */
    public Key(String keyId, KeyColor color) {
        this(keyId, color, false);
    }

    public String getKeyId() {
        return keyId;
    }

    public KeyColor getColor() {
        return color;
    }

    public boolean isSingleUse() {
        return singleUse;
    }

    /** True when this key's id matches the lock's required id (case-insensitive). */
    public boolean matches(String requiredKeyId) {
        return requiredKeyId != null && requiredKeyId.equalsIgnoreCase(keyId);
    }
}
