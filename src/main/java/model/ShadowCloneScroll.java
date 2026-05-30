package model;

import java.util.List;

/**
 * Collectible readable scroll that summons a shadow clone when read.
 */
public class ShadowCloneScroll extends Book {

    public ShadowCloneScroll(String name, String text) {
        super(name, text);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.READ, ItemAction.DISCARD);
    }
}
