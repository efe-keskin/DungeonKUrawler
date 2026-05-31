package engine;

import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Item;
import model.Torch;

/**
 * Computes per-cell visibility for the Fear-of-the-Dark mechanic
 * and marks cells as discovered as the hero explores.
 *
 * <p>Strategy pattern: visibility logic is pluggable. Default is a
 * Euclidean circle of radius 3. A hero carrying a Torch swaps to a
 * radius-5 strategy automatically - same engine, different rule,
 * no conditional inside the visibility math itself.
 *
 * <p>Three-state per cell, derived (not stored):
 * <ul>
 *   <li>VISIBLE - currently within strategy.isVisible
 *   <li>DISCOVERED - was previously visible at some point
 *   <li>HIDDEN - never seen
 * </ul>
 * Only DISCOVERED is persisted; VISIBLE is recomputed every
 * paint frame from the hero's current position.
 */
public final class FearOfTheDarkEngine {

    public static final double DEFAULT_VISION_RADIUS = 3.0;
    public static final double TORCH_VISION_RADIUS = 5.0;

    private final VisibilityStrategy darkStrategy =
            new CircularVisibility(DEFAULT_VISION_RADIUS);
    private final VisibilityStrategy torchStrategy =
            new CircularVisibility(TORCH_VISION_RADIUS);

    /**
     * Auxiliary light from a companion (the dragon pet): a square of Chebyshev
     * radius {@link #auxLightRadius} centered on (auxLightX, auxLightY). Unlike
     * the hero's vision this is a separate, moving light source that the engine
     * keeps in sync. Inactive while {@code auxLightRadius < 0}.
     */
    private int auxLightX;
    private int auxLightY;
    private int auxLightRadius = -1;

    /**
     * Hero-aware reveal: picks the strategy by checking the hero's
     * inventory, then marks all visible cells as discovered.
     * No-op when fog is disabled on the map or hero is null.
     */
    public void revealAround(DungeonMap map, Hero hero) {
        if (map == null || hero == null || !map.isFogEnabled()) {
            return;
        }
        VisibilityStrategy strategy = selectStrategy(hero);
        int hx = hero.getX();
        int hy = hero.getY();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (strategy.isVisible(map, hx, hy, x, y) || auxLit(x, y)) {
                    GridCell cell = map.getCell(x, y);
                    if (cell != null) {
                        cell.setDiscovered(true);
                    }
                }
            }
        }
    }

    /**
     * Hero-aware visibility query for the renderer. Honors torch
     * presence so the bright circle matches reveal exactly.
     */
    public boolean isVisible(DungeonMap map, Hero hero,
                             int cellX, int cellY) {
        if (hero == null) {
            return false;
        }
        return selectStrategy(hero).isVisible(
                map, hero.getX(), hero.getY(), cellX, cellY)
                || auxLit(cellX, cellY);
    }

    /**
     * Sets the companion light source as a square of the given Chebyshev radius
     * (radius 1 = a 3x3 area) centered on the cell. The engine calls this each
     * tick to track the dragon pet's position.
     */
    public void setAuxiliaryLight(int x, int y, int chebyshevRadius) {
        this.auxLightX = x;
        this.auxLightY = y;
        this.auxLightRadius = chebyshevRadius;
    }

    /** Disables the companion light (e.g. when no dragon pet is present). */
    public void clearAuxiliaryLight() {
        this.auxLightRadius = -1;
    }

    private boolean auxLit(int cellX, int cellY) {
        return auxLightRadius >= 0
                && Math.abs(cellX - auxLightX) <= auxLightRadius
                && Math.abs(cellY - auxLightY) <= auxLightRadius;
    }

    private VisibilityStrategy selectStrategy(Hero hero) {
        for (Item item : hero.getInventory().getItems()) {
            if (item instanceof Torch) {
                return torchStrategy;
            }
        }
        return darkStrategy;
    }
}
