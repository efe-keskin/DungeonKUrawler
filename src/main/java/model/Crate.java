package model;

import java.util.List;


public class Crate extends SearchableObject {

    private static final String SEARCHABLE_DIR = "/background_floor/assets/searchable assets/crates/";
    private static final String BREAKABLE_DIR = "/background_floor/assets/breakable assets/crates/";

    public static final String WOOD_RIGHT_SPRITE = SEARCHABLE_DIR + "16_crate_wood_tall_right.png";
    public static final String WOOD_TALL_SPRITE = SEARCHABLE_DIR + "17_crate_wood_tall_corrected.png";
    public static final String ORANGE_TALL_SPRITE = SEARCHABLE_DIR + "18_crate_orange_tall_corrected.png";
    public static final String BREAKABLE_WOOD_RIGHT_SPRITE = BREAKABLE_DIR + "16_crate_wood_tall_right.png";
    public static final String BREAKABLE_WOOD_TALL_SPRITE = BREAKABLE_DIR + "17_crate_wood_tall_corrected.png";
    public static final String BREAKABLE_ORANGE_TALL_SPRITE = BREAKABLE_DIR + "18_crate_orange_tall_corrected.png";

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
