package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * World object that may hold loot.
 *
 * <p>Information Expert: the container owns its contents, capacity, and
 * access rules (lock / key / break / portability). Access policies are
 * currently exposed as flags; they can be lifted to a Strategy later
 * without changing callers.
 */
public class Container extends Item {

    private boolean isLocked;
    private boolean requiresKey;
    private String requiredKeyId;
    private boolean breakable;
    private int breakStrengthRequired;
    /** Portable containers (pouches, small bags) can be picked up; chests/crates cannot. */
    private final boolean portable;
    private final int capacity;
    private final List<Item> contents = new ArrayList<>();

    public Container(String name, boolean isLocked, boolean requiresKey, int capacity) {
        this(name, isLocked, requiresKey, capacity, false);
    }

    public Container(String name, boolean isLocked, boolean requiresKey, int capacity, boolean portable) {
        super(name);
        this.isLocked = isLocked;
        this.requiresKey = requiresKey;
        this.capacity = capacity;
        this.portable = portable;
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
}
