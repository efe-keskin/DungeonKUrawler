package engine;

import java.util.Objects;
import java.util.function.Supplier;

import model.Item;

/**
 * One selectable tool in build mode.
 *
 * <p>Factory Method: each object tool owns the factory that creates the model
 * item to place. The view only displays and passes around tools.
 */
public final class BuildTool {

    public enum PlacementKind {
        FLOOR_BRUSH,
        WALL_BRUSH,
        FLOOR_OBJECT,
        HORIZONTAL_WALL_SEARCH
    }

    private final String id;
    private final String label;
    private final PlacementKind placementKind;
    private final Supplier<Item> itemFactory;
    private final Supplier<Item> previewFactory;

    public BuildTool(String id, String label, PlacementKind placementKind, Supplier<Item> itemFactory) {
        this(id, label, placementKind, itemFactory, itemFactory);
    }

    public BuildTool(String id, String label, PlacementKind placementKind,
            Supplier<Item> itemFactory, Supplier<Item> previewFactory) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.placementKind = Objects.requireNonNull(placementKind, "placementKind");
        this.itemFactory = itemFactory;
        this.previewFactory = previewFactory;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public PlacementKind placementKind() {
        return placementKind;
    }

    public boolean isFloorBrush() {
        return placementKind == PlacementKind.FLOOR_BRUSH;
    }

    public boolean isWallBrush() {
        return placementKind == PlacementKind.WALL_BRUSH;
    }

    public boolean isHorizontalWallSearch() {
        return placementKind == PlacementKind.HORIZONTAL_WALL_SEARCH;
    }

    public Item createItem() {
        return itemFactory == null ? null : itemFactory.get();
    }

    public Item previewItem() {
        return previewFactory == null ? null : previewFactory.get();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BuildTool tool && id.equals(tool.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
