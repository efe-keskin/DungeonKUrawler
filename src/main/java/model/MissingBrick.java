package model;

public class MissingBrick extends SearchableObject {

    public static final String SPRITE_1 = "/background_floor/assets/searchable assets/missingbrick1.png";
    public static final String SPRITE_2 = "/background_floor/assets/searchable assets/missingbrick2.png";

    public MissingBrick() {
        this(null);
    }

    public MissingBrick(Item hiddenItem) {
        this(SPRITE_1, hiddenItem);
    }

    public MissingBrick(String spriteResource, Item hiddenItem) {
        super("Missing Brick", false, spriteResource, hiddenItem);
    }
}
