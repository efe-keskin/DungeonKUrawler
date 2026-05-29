package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Run-wide persistent inventory (GRASP Information Expert): the things a player
 * keeps for the whole tower run — gold, valuables collected from levels, and
 * shop purchases (future pets / permanent equipment).
 *
 * <p>Unlike the per-level {@link InGameInventory} bag, this store is unbounded
 * and survives floor transitions: {@code DungeonLevelFactory.carryOverHero}
 * copies it onto the fresh hero, and the save layer persists it. Gold lives
 * here as the single source of truth; {@link Hero} delegates its coin methods.
 */
public final class FullGameInventory {

    private int gold;
    private final List<Item> items = new ArrayList<>();

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    /**
     * Adds a positive amount of gold.
     *
     * @return true when the balance changed; false for zero or negative amounts.
     */
    public boolean earn(int amount) {
        if (amount <= 0) {
            return false;
        }
        gold += amount;
        return true;
    }

    /**
     * Spends gold if the balance can cover it.
     *
     * @return true when {@code amount} was deducted; false if non-positive or
     *         the balance is insufficient (balance unchanged).
     */
    public boolean spend(int amount) {
        if (amount <= 0 || amount > gold) {
            return false;
        }
        gold -= amount;
        return true;
    }

    /** Unmodifiable view — callers mutate through {@link #add}/{@link #remove}. */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public boolean add(Item item) {
        if (item == null) {
            return false;
        }
        return items.add(item);
    }

    public void addAll(Collection<? extends Item> newItems) {
        if (newItems == null) {
            return;
        }
        for (Item item : newItems) {
            add(item);
        }
    }

    public boolean remove(Item item) {
        return items.remove(item);
    }

    /** Replaces this inventory's gold and items with {@code source}'s (carry-over). */
    public void copyFrom(FullGameInventory source) {
        if (source == null) {
            return;
        }
        this.gold = source.gold;
        this.items.clear();
        this.items.addAll(source.items);
    }
}
