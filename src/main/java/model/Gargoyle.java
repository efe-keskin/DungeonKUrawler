package model;

public class Gargoyle extends SearchableObject {

    public static final String RED_LEFT_SPRITE =
            "/background_floor/assets/searchable assets/gargoyle_red.png";
    public static final String GREEN_LEFT_SPRITE =
            "/background_floor/assets/searchable assets/gargoyle_green.png";
    public static final String CYAN_LEFT_SPRITE =
            "/background_floor/assets/searchable assets/gargoyle_blue.png";
    public static final String RED_MID_SPRITE =
            RED_LEFT_SPRITE;
    public static final String GREEN_MID_SPRITE =
            GREEN_LEFT_SPRITE;
    public static final String CYAN_MID_SPRITE =
            CYAN_LEFT_SPRITE;
    public static final String GREEN_RIGHT_SPRITE =
            GREEN_LEFT_SPRITE;
    public static final String CYAN_RIGHT_SPRITE =
            CYAN_LEFT_SPRITE;
    public static final String RED_RIGHT_SPRITE =
            RED_LEFT_SPRITE;

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
