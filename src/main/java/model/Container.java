package model;

/**
 * World object that may hold loot; locking rules tie to {@code requiresKey}.
 */
public class Container extends Item {

    private boolean isLocked;
    private boolean requiresKey;
    private int capacity;

    public Container(String name, boolean isLocked, boolean requiresKey, int capacity) {
        super(name);
        this.isLocked = isLocked;
        this.requiresKey = requiresKey;
        this.capacity = capacity;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
