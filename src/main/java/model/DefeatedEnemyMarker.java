package model;

import java.util.List;

/**
 * Small non-interactive marker left where an enemy was defeated.
 */
public class DefeatedEnemyMarker extends Item {

    public DefeatedEnemyMarker() {
        super("Defeated Enemy");
    }

    @Override
    public boolean isTakable() {
        return false;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of();
    }
}
