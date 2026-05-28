package model;

public class Gargoyle extends SearchableObject {

    public static final String RED_LEFT_SPRITE =
            "/background_floor/assets/searchable assets/26_wall_detail_drip_red_left(below).png";
    public static final String GREEN_LEFT_SPRITE =
            "/background_floor/assets/searchable assets/27_wall_detail_drip_green_left.png";
    public static final String CYAN_LEFT_SPRITE =
            "/background_floor/assets/searchable assets/28_wall_detail_drip_cyan_left(above).png";
    public static final String RED_MID_SPRITE =
            "/background_floor/assets/searchable assets/29_wall_detail_drip_red_mid.png";
    public static final String GREEN_MID_SPRITE =
            "/background_floor/assets/searchable assets/30_wall_detail_drip_green_mid.png";
    public static final String CYAN_MID_SPRITE =
            "/background_floor/assets/searchable assets/31_wall_detail_drip_cyan_mid.png";
    public static final String GREEN_RIGHT_SPRITE =
            "/background_floor/assets/searchable assets/32_wall_detail_drip_green_right.png";
    public static final String CYAN_RIGHT_SPRITE =
            "/background_floor/assets/searchable assets/33_wall_detail_drip_cyan_right.png";
    public static final String RED_RIGHT_SPRITE =
            "/background_floor/assets/searchable assets/34_wall_detail_drip_red_right.png";

    public Gargoyle() {
        this(null);
    }

    public Gargoyle(Item hiddenItem) {
        this(RED_LEFT_SPRITE, hiddenItem);
    }

    public Gargoyle(String spriteResource, Item hiddenItem) {
        super("Stone Gargoyle", true, spriteResource, hiddenItem);
    }
}
