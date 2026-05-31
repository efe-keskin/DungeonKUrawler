package model;

public class Pool extends SearchableObject {

    public static final String CYAN_DRIP_SPRITE =
            "/background_floor/assets/searchable assets/gargoyle_blue.png";
    public static final String GREEN_DRIP_SPRITE =
            "/background_floor/assets/searchable assets/gargoyle_green.png";
    public static final String RED_DRIP_SPRITE =
            "/background_floor/assets/searchable assets/gargoyle_red.png";

    public Pool() {
        this(null);
    }

    public Pool(Item hiddenItem) {
        this(CYAN_DRIP_SPRITE, hiddenItem);
    }

    public Pool(String spriteResource, Item hiddenItem) {
        super("Murky Pool", true, spriteResource, hiddenItem);
    }
}
