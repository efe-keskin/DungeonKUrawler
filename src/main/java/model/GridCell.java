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
    private boolean discovered = false;
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

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    /**
     * requires: No special precondition.
     * modifies: Nothing.
     * effects:
     *   Returns false if the cell itself is not passable.
     *   Returns false if the cell contains at least one blocking item.
     *   Returns true only if the cell is passable and none of its items are blocking.
     */
    public boolean isWalkable() {
        if (!passable) {
            return false;
        }
        for (Item item : items) {
            if (item.isBlocking()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Live list — game logic mutates this; the view should only read via
     * {@link engine.GameEngine} snapshots or getters.
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Live list of entities on this tile.
     */
    public List<Entity> getEntities() {
        return entities;
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
