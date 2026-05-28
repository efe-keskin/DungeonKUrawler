package model;

public class Column extends SearchableObject {

    public static final String PURPLE_SPRITE =
            "/background_floor/assets/searchable assets/39_pillar_purple.png";
    public static final String GRAY_SPRITE =
            "/background_floor/assets/searchable assets/40_pillar_gray.png";
    public static final String WALL_TOP_SPRITE =
            "/background_floor/assets/searchable assets/10_wall_column_round_top.png";

    public Column() {
        this(null);
    }

    public Column(Item hiddenItem) {
        this(GRAY_SPRITE, hiddenItem);
    }

    public Column(String spriteResource, Item hiddenItem) {
        super("Stone Column", true, spriteResource, hiddenItem);
    }
}
