package model;

import java.util.List;

/**
 * The exit of a tower floor. Sits on a walkable cell (non-blocking) so the hero
 * can step onto it. Reaching the door <em>after</em> the floor's hidden target
 * has been collected completes the floor — that rule is enforced by
 * {@link engine.GameEngine}; the door itself only marks the exit tile.
 */
public final class Door extends StaticObject {

    public static final String SPRITE = "/background_floor/assets/15_door_closed_wood.png";

    public Door() {
        super("Exit Door", false);
    }

    @Override
    public String spriteResource() {
        return SPRITE;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of();
    }
}
