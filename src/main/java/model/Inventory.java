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
}
