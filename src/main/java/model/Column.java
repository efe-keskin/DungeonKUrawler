package model;

import java.util.List;


public class Column extends SearchableObject {

    public static final String PURPLE_SPRITE =
            "/background_floor/assets/searchable assets/column1.png";
    public static final String GRAY_SPRITE =
            "/background_floor/assets/searchable assets/column2.png";
    public static final String WALL_TOP_SPRITE =
            "/background_floor/assets/searchable assets/column3.png";

    public Column() {
        this(GRAY_SPRITE);
    }

    public Column(String spriteResource) {
        this(spriteResource, null);
    }

    public Column(String spriteResource, Item hiddenItem) {
        super("Stone Column", true, spriteResource, hiddenItem);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH, ItemAction.BREAK);
    }
}
