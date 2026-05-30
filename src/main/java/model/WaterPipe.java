package model;


import java.util.List;

public class WaterPipe extends SearchableObject {

    public static final String SMALL_RING_SPRITE =
            "/background_floor/assets/searchable assets/water_pipes.png";
    public static final String LARGE_RING_SPRITE =
            "/background_floor/assets/searchable assets/water_pipes.png";
    public static final String TEARDROP_RING_SPRITE =
            "/background_floor/assets/searchable assets/water_pipes.png";

    public WaterPipe() {
        this(LARGE_RING_SPRITE);
    }

    public WaterPipe(String spriteResource) {
        this(spriteResource, null);
    }

    public WaterPipe(String spriteResource, Item hiddenItem) {
        super("Water Pipe", true, spriteResource, hiddenItem);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH, ItemAction.BREAK);
    }
}
