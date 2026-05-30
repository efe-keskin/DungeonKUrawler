package model;

import java.util.List;

/**
 * Equipment that modifies defense when worn.
 */
public class Armor extends Item {

    private int defModifier;

    public Armor(String name, int defModifier) {
        super(name);
        this.defModifier = defModifier;
    }

    public int getDefModifier() {
        return defModifier;
    }

    public void setDefModifier(int defModifier) {
        this.defModifier = defModifier;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.WEAR, ItemAction.EQUIP, ItemAction.DISCARD);
    }
}
