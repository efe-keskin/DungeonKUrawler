package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Overview:
 *   Container represents a game item that can store other items up to a fixed
 *   capacity. A container may be locked, may require a key to unlock, may be
 *   breakable, and may or may not be portable.
 *
 * Abstract Function:
 *   AF(c) = a container named c.name whose stored items are c.contents, whose
 *   maximum number of stored items is c.capacity, whose lock state is
 *   c.isLocked, whose key requirement is represented by c.requiresKey and
 *   c.requiredKeyId, whose break behavior is represented by c.breakable and
 *   c.breakStrengthRequired, and whose portability is represented by c.portable.
 *
 * Representation Invariant:
 *   - name is not null or blank
 *   - capacity is non-negative
 *   - contents is not null
 *   - contents.size() is less than or equal to capacity
 *   - contents does not contain null
 *   - if requiresKey is true, requiredKeyId is not null or blank
 *   - breakStrengthRequired is non-negative
 */
public class Container extends Item implements Lockable {

    private boolean isLocked;
    private boolean requiresKey;
    private String requiredKeyId;
    private boolean breakable;
    private int breakStrengthRequired;
    /** Portable containers (pouches, small bags) can be picked up; chests/crates cannot. */
    private final boolean portable;
    private final int capacity;
    private final String spriteResource;
    private final List<Item> contents = new ArrayList<>();

    public Container(String name, boolean isLocked, boolean requiresKey, int capacity) {
        this(name, isLocked, requiresKey, capacity, false, null);
    }

    public Container(String name, boolean isLocked, boolean requiresKey, int capacity, boolean portable) {
        this(name, isLocked, requiresKey, capacity, portable, null);
    }

    public Container(String name, boolean isLocked, boolean requiresKey, int capacity,
            boolean portable, String spriteResource) {
        super(name);
        this.isLocked = isLocked;
        this.requiresKey = requiresKey;
        this.capacity = capacity;
        this.portable = portable;
        this.spriteResource = spriteResource;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isRequiresKey() {
        return requiresKey;
    }

    public void setRequiresKey(boolean requiresKey) {
        this.requiresKey = requiresKey;
    }

    public String getRequiredKeyId() {
        return requiredKeyId;
    }

    public void setRequiredKeyId(String requiredKeyId) {
        this.requiredKeyId = requiredKeyId;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

    public int getBreakStrengthRequired() {
        return breakStrengthRequired;
    }

    public void setBreakStrengthRequired(int breakStrengthRequired) {
        this.breakStrengthRequired = breakStrengthRequired;
    }

    public boolean isPortable() {
        return portable;
    }

    public int getCapacity() {
        return capacity;
    }

    /** Read-only view; UI iterates this but does not mutate. */
    public List<Item> getContents() {
        return Collections.unmodifiableList(contents);
    }

    public int size() {
        return contents.size();
    }

    public boolean isFull() {
        return contents.size() >= capacity;
    }

    /**
     * Adds {@code item} when there is room.
     * @return true if the item was stored; false if full or null.
     */
    public boolean addItem(Item item) {
        if (item == null || isFull()) {
            return false;
        }
        contents.add(item);
        return true;
    }

    public boolean removeItem(Item item) {
        return contents.remove(item);
    }

    /**
     * Checks whether this container satisfies its representation invariant.
     */
    public boolean repOk() {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (capacity < 0) {
            return false;
        }
        if (contents == null) {
            return false;
        }
        if (contents.size() > capacity) {
            return false;
        }
        if (contents.contains(null)) {
            return false;
        }
        if (requiresKey && (requiredKeyId == null || requiredKeyId.isBlank())) {
            return false;
        }
        return breakStrengthRequired >= 0;
    }

    /**
     * Whether the hero can open this container right now (no key/strength gating yet).
     * Future: delegate to a ContainerAccessPolicy (Strategy).
     */
    public boolean canOpen(Hero hero) {
        return !isLocked;
    }

    /** Chests/crates are fixtures; only portable containers can be picked up. */
    @Override
    public boolean isTakable() {
        return portable;
    }

    /** Non-portable containers (chests, crates) block movement onto their tile. */
    @Override
    public boolean isBlocking() {
        return !portable;
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
