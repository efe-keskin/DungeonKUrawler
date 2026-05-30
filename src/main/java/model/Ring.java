package model;

import java.util.List;

/**
 * Protective wearable ring that increases defense while equipped.
 */
public class Ring extends Item {

    private final int defBonus;
    private final String spriteResource;

    public Ring(String name, int defBonus) {
        this(name, defBonus, null);
    }

    public Ring(String name, int defBonus, String spriteResource) {
        super(name);
        this.defBonus = defBonus;
        this.spriteResource = spriteResource;
    }

    public int getDefBonus() {
        return defBonus;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.WEAR, ItemAction.DISCARD);
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
