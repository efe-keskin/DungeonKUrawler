package model;


import java.util.List;

public class WaterPipe extends StaticObject {

    public static final String SMALL_RING_SPRITE =
            "/background_floor/assets/searchable assets/23_wall_detail_ring_small_blue.png";
    public static final String LARGE_RING_SPRITE =
            "/background_floor/assets/searchable assets/24_wall_detail_ring_large_blue.png";
    public static final String TEARDROP_RING_SPRITE =
            "/background_floor/assets/searchable assets/25_wall_detail_ring_teardrop_blue.png";

    private final String spriteResource;

    public WaterPipe() {
        this(LARGE_RING_SPRITE);
    }

    public WaterPipe(String spriteResource) {
        super("Water Pipe", true);
        this.spriteResource = spriteResource;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.BREAK);
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
