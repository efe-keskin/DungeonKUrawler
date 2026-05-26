package model;

import java.util.List;

/**
 * Equipment that contributes attack; {@code isRanged} distinguishes bow vs sword, etc.
 */
public class Weapon extends Item {

    private int atkValue;
    private boolean isRanged;

    public Weapon(String name, int atkValue, boolean isRanged) {
        super(name);
        this.atkValue = atkValue;
        this.isRanged = isRanged;
    }

    public int getAtkValue() {
        return atkValue;
    }

    public void setAtkValue(int atkValue) {
        this.atkValue = atkValue;
    }

    public boolean isRanged() {
        return isRanged;
    }

    public void setRanged(boolean ranged) {
        isRanged = ranged;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.EQUIP, ItemAction.DISCARD);
    }
}
