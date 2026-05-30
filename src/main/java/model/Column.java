package model;

public class Column extends BreakableObject {

    public static final String PURPLE_SPRITE =
            "/background_floor/assets/breakable assets/column1.png";
    public static final String GRAY_SPRITE =
            "/background_floor/assets/breakable assets/column2.png";
    public static final String WALL_TOP_SPRITE =
            "/background_floor/assets/breakable assets/column3.png";

    public Column() {
        this(GRAY_SPRITE);
    }

    public Column(String spriteResource) {
        super("Stone Column", true, spriteResource);
    }
}
