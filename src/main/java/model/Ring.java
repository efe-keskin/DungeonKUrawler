package model;

import java.util.List;

/**
 * Protective wearable ring that increases defense while equipped.
 */
public class Ring extends Item {

    private final int defBonus;

    public Ring(String name, int defBonus) {
        super(name);
        this.defBonus = defBonus;
    }

    public int getDefBonus() {
        return defBonus;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.WEAR, ItemAction.DISCARD);
    }
}
