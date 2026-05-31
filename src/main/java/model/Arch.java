package model;

import java.util.List;

/**
 * The exit of a tower floor: an arch set into the wall. It starts closed (a
 * wooden barrier blocking the gap) and is opened with the {@code O} key once the
 * floor's hidden treasure has been found. While closed it blocks movement;
 * once open it becomes passable, and stepping through it clears the floor.
 *
 * <p>Open/closed is reflected by the sprite and by {@link #isBlocking()}, which
 * {@link GridCell#isWalkable()} consults — so the same tile is impassable when
 * closed and walkable when open.
 */
public final class Arch extends StaticObject {

    public static final String CLOSED_SPRITE = "/background_floor/assets/doors/15_door_closed_wood.png";
    public static final String OPEN_SPRITE = "/background_floor/assets/doors/17_door_open_wood.png";

    private boolean open;

    public Arch() {
        super("Stone Arch", true);
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        this.open = true;
    }

    @Override
    public boolean isBlocking() {
        return !open;
    }

    @Override
    public String spriteResource() {
        return open ? OPEN_SPRITE : CLOSED_SPRITE;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of();
    }
}
