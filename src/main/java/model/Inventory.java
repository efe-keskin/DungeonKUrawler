package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fixed-capacity bag for the {@link Hero}. Add operations reject new items when full.
 */
public class Inventory {

    private final int capacity;
    private final List<Item> items;

    public Inventory(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be non-negative");
        }
        this.capacity = capacity;
        this.items = new ArrayList<>();
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Unmodifiable view — callers cannot bypass capacity rules by mutating the list directly.
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    /** Readable inverse of {@link #isFull()} — used by InteractionController. */
    public boolean hasFreeSlot() {
        return !isFull();
    }

    /**
     * requires: No special precondition. item may be null.
     * modifies: this.items
     * effects:
     *   If item is null, returns false and inventory does not change.
     *   If inventory is full, returns false and item is not added.
     *   Otherwise, adds item to inventory and returns true.
     */
    public boolean tryAdd(Item item) {
        if (item == null || isFull()) {
            return false;
        }
        items.add(item);
        return true;
    }

    public boolean remove(Item item) {
        return items.remove(item);
    }

    /*
     * requires: No special precondition. requiredKeyId may be null.
     * modifies: Nothing.
     * effects:
     * If requiredKeyId is null, returns null.
     * Searches inventory items for the first Key whose keyId matches requiredKeyId.
     * Matching is case-insensitive.
     * Returns the matching Key if found; otherwise returns null.
     */
    public Key findKey(String requiredKeyId) {
        if (requiredKeyId == null) {
            return null;
        }
        for (Item item : items) {
            if (item instanceof Key key && key.matches(requiredKeyId)) {
                return key;
            }
        }
        return null;
    }

    public boolean containsKey(String requiredKeyId) {
        return findKey(requiredKeyId) != null;
    }

    

}
