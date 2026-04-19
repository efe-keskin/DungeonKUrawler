package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One tile on the {@link DungeonMap}.
 *
 * <p>
 * Fields: grid coordinates {@code x}/{@code y}, walkability
 * {@link #isPassable}, loose
 * {@link Item}s on the ground, and {@link Entity} instances currently occupying
 * this cell. The
 * lists are mutable by design for the game engine; UI code should prefer
 * {@link #getItemsView()} and
 * {@link #getEntitiesView()} for read-only snapshots.
 */
public class GridCell {

    private final int x;
    private final int y;
    private boolean passable;
    private final List<Item> items;
    private final List<Entity> entities;

    public GridCell(int x, int y, boolean passable) {
        this.x = x;
        this.y = y;
        this.passable = passable;
        this.items = new ArrayList<>();
        this.entities = new ArrayList<>();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isPassable() {
        return passable;
    }

    public void setPassable(boolean passable) {
        this.passable = passable;
    }

    /**
     * Information Expert: the cell itself decides whether it can be walked on.
     * Currently equivalent to {@link #isPassable()}, but kept separate so future
     * conditions (traps, locked doors, etc.) can be added here without changing
     * callers.
     */
    public boolean isWalkable() {
        return passable;
    }

    public boolean addItem(Item item) {
        if (item == null) {
            return false;
        }
        return items.add(item);
    }

    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    public boolean addEntity(Entity entity) {
        if (entity == null || entities.contains(entity)) {
            return false;
        }
        return entities.add(entity);
    }

    public boolean removeEntity(Entity entity) {
        return entities.remove(entity);
    }

    public boolean hasEntities() {
        return !entities.isEmpty();
    }

    public boolean containsEntity(Entity entity) {
        return entities.contains(entity);
    }

    /**
     * Safe read-only copy for UI layers that must not mutate model collections
     * accidentally.
     */
    public List<Item> getItemsView() {
        return Collections.unmodifiableList(items);
    }

    public List<Entity> getEntitiesView() {
        return Collections.unmodifiableList(entities);
    }
}
