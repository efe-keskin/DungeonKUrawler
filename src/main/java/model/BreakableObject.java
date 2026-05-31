package model;

import java.util.List;

/**
 * Static scenery that can be destroyed but does not hide searchable loot.
 */
public class BreakableObject extends StaticObject {

    private final String spriteResource;

    public BreakableObject(String name, boolean blocking, String spriteResource) {
        super(name, blocking);
        this.spriteResource = spriteResource;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.BREAK);
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
