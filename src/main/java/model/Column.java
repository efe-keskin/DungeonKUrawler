package model;

import java.util.List;


public class Column extends StaticObject {

    public static final String PURPLE_SPRITE =
            "/background_floor/assets/searchable assets/39_pillar_purple.png";
    public static final String GRAY_SPRITE =
            "/background_floor/assets/searchable assets/40_pillar_gray.png";
    public static final String WALL_TOP_SPRITE =
            "/background_floor/assets/searchable assets/10_wall_column_round_top.png";

    public Column() {
        this(GRAY_SPRITE);
    }

    public Column(String spriteResource) {
        super("Stone Column", true);
        this.spriteResource = spriteResource;
    }

    private final String spriteResource;

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.BREAK);
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
