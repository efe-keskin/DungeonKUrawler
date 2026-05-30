package model;

import java.util.List;


public class Crate extends SearchableObject {

    public static final String WOOD_RIGHT_SPRITE = "/items/crates/16_crate_wood_tall_right.png";
    public static final String WOOD_TALL_SPRITE = "/items/crates/17_crate_wood_tall_corrected.png";
    public static final String ORANGE_TALL_SPRITE = "/items/crates/18_crate_orange_tall_corrected.png";

    public Crate() {
        this(null);
    }

    public Crate(Item hiddenItem) {
        this(WOOD_TALL_SPRITE, hiddenItem);
    }

    public Crate(String spriteResource, Item hiddenItem) {
        super("Wooden Crate", true, spriteResource, hiddenItem);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH, ItemAction.BREAK);
    }
}
