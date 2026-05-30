package model;

public class Hole extends SearchableObject {

    public static final String SPRITE =
            "/background_floor/assets/searchable assets/hole1.png";
    public static final String SPRITE_2 = "/background_floor/assets/searchable assets/hole2.png";
    public static final String SPRITE_3 = "/background_floor/assets/searchable assets/hole3.png";

    public Hole() {
        this(null);
    }

    public Hole(Item hiddenItem) {
        this(SPRITE, hiddenItem);
    }

    public Hole(String spriteResource, Item hiddenItem) {
        super("Hole", true, spriteResource, hiddenItem);
    }
}
