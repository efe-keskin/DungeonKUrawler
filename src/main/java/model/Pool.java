package model;

public class Pool extends SearchableObject {

    public static final String CYAN_DRIP_SPRITE =
            "/background_floor/assets/searchable assets/28_wall_detail_drip_cyan_left.png";
    public static final String GREEN_DRIP_SPRITE =
            "/background_floor/assets/searchable assets/30_wall_detail_drip_green_mid.png";
    public static final String RED_DRIP_SPRITE =
            "/background_floor/assets/searchable assets/29_wall_detail_drip_red_mid.png";

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
