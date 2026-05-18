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
     * @return true if the item was added; false if inventory is full
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

    /**
     * Finds the first {@link Key} whose id matches {@code requiredKeyId}.
     * @return the matching key, or {@code null} when none is carried.
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
