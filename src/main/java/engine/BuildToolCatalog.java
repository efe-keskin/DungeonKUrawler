package engine;

import static engine.BuildTool.PlacementKind.FLOOR_BRUSH;
import static engine.BuildTool.PlacementKind.FLOOR_OBJECT;
import static engine.BuildTool.PlacementKind.HORIZONTAL_WALL_SEARCH;
import static engine.BuildTool.PlacementKind.WALL_BRUSH;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import model.Armor;
import model.Chest;
import model.Column;
import model.Crate;
import model.EnergyPotion;
import model.Gargoyle;
import model.HealPotion;
import model.Item;
import model.Key;
import model.KeyColor;
import model.ManaPotion;
import model.MissingBrick;
import model.Pedestal;
import model.Pool;
import model.Ring;
import model.SearchableObject;
import model.ValuableItem;
import model.ValuableItemCatalog;
import model.Vase;
import model.WaterPipe;
import model.Weapon;
import model.WeaponCatalog;

/**
 * Pure Fabrication / Creator: centralizes the build-mode palette and object
 * construction so Swing code does not know how to instantiate model items.
 */
public final class BuildToolCatalog {

    private static final String VALUABLE_SPRITE_DIR = "/items_objects/valuable_items/";

    private final List<BuildTool> tools;
    private final Map<String, BuildTool> byId;

    public BuildToolCatalog() {
        tools = List.of(
                brush("FLOOR", "Floor", FLOOR_BRUSH),
                brush("WALL", "Wall", WALL_BRUSH),
                object("CHEST", "Chest", () -> {
                    Chest chest = new Chest("Wooden Chest", 16);
                    chest.addItem(new HealPotion());
                    return chest;
                }),
                object("LOCKED_CHEST", "Locked Chest", () -> {
                    Chest chest = Chest.locked("Silver Chest", 16, "silver");
                    chest.addItem(new ManaPotion());
                    return chest;
                }),
                object("CRATE", "Crate", () -> new Crate(new EnergyPotion())),
                object("COLUMN", "Column", Column::new),
                object("VASE", "Vase", Vase::new),
                object("WATER_PIPE", "Water Pipe", WaterPipe::new),
                object("PEDESTAL", "Pedestal", () -> new Pedestal(new EnergyPotion())),
                wallSearch("POOL", "Pool", () -> new Pool(new ManaPotion())),
                wallSearch("GARGOYLE", "Gargoyle", () -> new Gargoyle(new Key("silver", KeyColor.SILVER))),
                wallSearch("MISSING_BRICK", "Missing Brick",
                        () -> new MissingBrick(MissingBrick.SPRITE_1, new EnergyPotion())),
                object("HEAL", "Heal", HealPotion::new),
                object("ENERGY", "Energy", EnergyPotion::new),
                object("MANA", "Mana", ManaPotion::new),
                object("KEY", "Key", () -> new Key("silver", KeyColor.SILVER)),
                object("WEAPON", "Weapon", () -> new Weapon(WeaponCatalog.get().byId("W002"))),
                object("ARMOR", "Armor", () -> new Armor("Leather Armor", 3)),
                object("RING", "Ring", () -> new Ring("Protective Ring", 2)),
                new BuildTool("VALUABLE", "Random Valuable", FLOOR_OBJECT,
                        () -> ValuableItemCatalog.randomValuable(ThreadLocalRandom.current()),
                        () -> valuable("Random Valuable", "crystal_shard_64x64.png")));

        byId = new LinkedHashMap<>();
        for (BuildTool tool : tools) {
            byId.put(tool.id(), tool);
        }
    }

    public List<BuildTool> tools() {
        return tools;
    }

    public BuildTool defaultTool() {
        return tools.get(0);
    }

    public BuildTool findById(String id) {
        return byId.get(id);
    }

    /**
     * Reverse lookup used by build-map persistence. The catalog remains the
     * Creator/Information Expert for palette objects in both directions.
     */
    public Optional<String> idForItem(Item item) {
        if (item == null) {
            return Optional.empty();
        }
        if (item instanceof Chest chest) {
            return Optional.of(chest.isLocked() ? "LOCKED_CHEST" : "CHEST");
        }
        if (item instanceof Crate) {
            return Optional.of("CRATE");
        }
        if (item instanceof Column) {
            return Optional.of("COLUMN");
        }
        if (item instanceof Vase) {
            return Optional.of("VASE");
        }
        if (item instanceof WaterPipe) {
            return Optional.of("WATER_PIPE");
        }
        if (item instanceof Pedestal) {
            return Optional.of("PEDESTAL");
        }
        if (item instanceof Pool) {
            return Optional.of("POOL");
        }
        if (item instanceof Gargoyle) {
            return Optional.of("GARGOYLE");
        }
        if (item instanceof MissingBrick) {
            return Optional.of("MISSING_BRICK");
        }
        if (item instanceof HealPotion) {
            return Optional.of("HEAL");
        }
        if (item instanceof EnergyPotion) {
            return Optional.of("ENERGY");
        }
        if (item instanceof ManaPotion) {
            return Optional.of("MANA");
        }
        if (item instanceof Key) {
            return Optional.of("KEY");
        }
        if (item instanceof Weapon) {
            return Optional.of("WEAPON");
        }
        if (item instanceof Armor) {
            return Optional.of("ARMOR");
        }
        if (item instanceof Ring) {
            return Optional.of("RING");
        }
        if (item instanceof ValuableItem) {
            return Optional.of("VALUABLE");
        }
        if (item instanceof SearchableObject) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static BuildTool brush(String id, String label, BuildTool.PlacementKind placementKind) {
        return new BuildTool(id, label, placementKind, null);
    }

    private static BuildTool object(String id, String label, java.util.function.Supplier<Item> itemFactory) {
        return new BuildTool(id, label, FLOOR_OBJECT, itemFactory);
    }

    private static BuildTool wallSearch(String id, String label, java.util.function.Supplier<Item> itemFactory) {
        return new BuildTool(id, label, HORIZONTAL_WALL_SEARCH, itemFactory);
    }

    private static ValuableItem valuable(String name, String spriteFile) {
        return new ValuableItem(name, VALUABLE_SPRITE_DIR + spriteFile);
    }
}
