package engine;

import static engine.BuildTool.PlacementKind.FLOOR_BRUSH;
import static engine.BuildTool.PlacementKind.FLOOR_OBJECT;
import static engine.BuildTool.PlacementKind.DOOR_OBJECT;
import static engine.BuildTool.PlacementKind.HORIZONTAL_WALL_SEARCH;
import static engine.BuildTool.PlacementKind.WALL_BRUSH;
import static engine.BuildTool.PlacementKind.WALL_OBJECT;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import model.Armor;
import model.Book;
import model.Chest;
import model.Coin;
import model.Column;
import model.Container;
import model.Crate;
import model.DecorativeObject;
import model.EnergyPotion;
import model.Gargoyle;
import model.Grill;
import model.HealPotion;
import model.Hole;
import model.Item;
import model.Key;
import model.KeyColor;
import model.ManaPotion;
import model.MissingBrick;
import model.Pedestal;
import model.Pool;
import model.Ring;
import model.RingEffectType;
import model.SearchableObject;
import model.ValuableItem;
import model.ValuableItemCatalog;
import model.Vase;
import model.WaterPipe;
import model.Weapon;
import model.WeaponCatalog;
import model.WeaponType;

/**
 * Pure Fabrication / Creator: centralizes the build-mode palette and object
 * construction so Swing code does not know how to instantiate model items.
 */
public final class BuildToolCatalog {

    private static final String VALUABLE_SPRITE_DIR = "/items/valuable_items/";
    private static final String ITEM_DIR = "/items/";
    private static final String BACKGROUND_DIR = "/background_floor/assets/";
    public static final String CLOSED_DOOR_SPRITE_RESOURCE =
            BACKGROUND_DIR + "doors/15_door_closed_wood.png";
    public static final String OPEN_DOOR_SPRITE_RESOURCE =
            BACKGROUND_DIR + "doors/17_door_open_wood.png";

    private final List<BuildTool> tools;
    private final Map<String, BuildTool> byId;

    public BuildToolCatalog() {
        List<BuildTool> baseTools = List.of(
                brush("FLOOR", "Floor Brush", FLOOR_BRUSH),
                brush("WALL", "Wall Brush", WALL_BRUSH),
                decor("FLOOR_WORN_PATCH", "Floor Worn Patch", false,
                        BACKGROUND_DIR + "floors/04_floor_worn_patch_round.png"),
                wallDecor("WALL_TOP_PLAIN", "Wall Top Plain",
                        BACKGROUND_DIR + "walls/01_wall_section_top_plain_left.png"),
                wallDecor("WALL_FRAME_VERTICAL", "Wall Frame Vertical",
                        BACKGROUND_DIR + "walls/11_wall_frame_vertical_open.png"),
                wallDecor("WALL_WINDOW", "Wall Window",
                        BACKGROUND_DIR + "walls/12_wall_frame_window.png"),
                wallDecor("WALL_BLOCK_SMALL", "Wall Block Small",
                        BACKGROUND_DIR + "walls/13_wall_block_small.png"),
                wallDecor("WALL_DOOR_LEFT", "Wall Door Left",
                        BACKGROUND_DIR + "walls/14_wall_section_mid_door_left.png"),
                wallDecor("WALL_DOOR_RIGHT", "Wall Door Right",
                        BACKGROUND_DIR + "walls/16_wall_section_mid_door_right.png"),
                wallDecor("WALL_ARCH_RIGHT", "Wall Arch Right",
                        BACKGROUND_DIR + "walls/18_wall_section_mid_arch_right.png"),
                doorDecor("DOOR_CLOSED", "Door Closed", true,
                        CLOSED_DOOR_SPRITE_RESOURCE),
                doorDecor("DOOR_OPEN", "Door Open", false,
                        OPEN_DOOR_SPRITE_RESOURCE),
                decor("RUG_ORANGE_LARGE_V", "Rug Orange Large V", false,
                        BACKGROUND_DIR + "rugs/41_rug_orange_large_vertical.png"),
                decor("RUG_ORANGE_LARGE_H", "Rug Orange Large H", false,
                        BACKGROUND_DIR + "rugs/42_rug_orange_large_horizontal.png"),
                decor("RUG_BEIGE_LARGE_V", "Rug Beige Large V", false,
                        BACKGROUND_DIR + "rugs/43_rug_beige_large_vertical.png"),
                decor("RUG_BEIGE_LARGE_H", "Rug Beige Large H", false,
                        BACKGROUND_DIR + "rugs/44_rug_beige_large_horizontal.png"),
                decor("RUG_ORANGE_SMALL_V", "Rug Orange Small V", false,
                        BACKGROUND_DIR + "rugs/45_rug_orange_small_vertical.png"),
                decor("RUG_ORANGE_SMALL_H", "Rug Orange Small H", false,
                        BACKGROUND_DIR + "rugs/46_rug_orange_small_horizontal.png"),
                decor("RUG_BEIGE_SMALL_V", "Rug Beige Small V", false,
                        BACKGROUND_DIR + "rugs/47_rug_beige_small_vertical.png"),
                decor("RUG_BEIGE_SMALL_H", "Rug Beige Small H", false,
                        BACKGROUND_DIR + "rugs/48_rug_beige_small_horizontal.png"),
                decor("BANNER_BROWN", "Banner Brown", false,
                        BACKGROUND_DIR + "banners/35_banner_brown.png"),
                decor("BANNER_GREEN", "Banner Green", false,
                        BACKGROUND_DIR + "banners/36_banner_green.png"),
                decor("BANNER_BLUE", "Banner Blue", false,
                        BACKGROUND_DIR + "banners/37_banner_blue.png"),
                decor("BANNER_YELLOW", "Banner Yellow", false,
                        BACKGROUND_DIR + "banners/38_banner_yellow.png"),
                decor("SIGN_ORANGE", "Sign Orange", false,
                        BACKGROUND_DIR + "signs/66_sign_orange.png"),
                decor("SIGN_GRAY", "Sign Gray", false,
                        BACKGROUND_DIR + "signs/67_sign_gray.png"),
                decor("TORCH_OFF", "Torch Off", false,
                        BACKGROUND_DIR + "torches/49_torch_extinguished.png"),
                decor("TORCH_LIT", "Torch Lit", false,
                        BACKGROUND_DIR + "torches/50_torch_lit_01.png"),
                decor("STAIRS_LEFT", "Stairs Left", false,
                        BACKGROUND_DIR + "stairs/59_stairs_left.png"),
                decor("STAIRS_UPPER_RIGHT", "Stairs Upper Right", false,
                        BACKGROUND_DIR + "stairs/60_stairs_upper_right.png"),
                decor("STAIRS_LOWER_RIGHT", "Stairs Lower Right", false,
                        BACKGROUND_DIR + "stairs/63_stairs_lower_right.png"),
                decor("TRAP_FLOOR", "Trap Floor", false,
                        BACKGROUND_DIR + "trap_floors/58_trap_floor.png"),
                decor("TRAP_HOLES", "Trap Holes", false,
                        BACKGROUND_DIR + "trap_floors/61_trap_floor_holes.png"),
                decor("TRAP_SPIKES", "Trap Spikes", false,
                        BACKGROUND_DIR + "trap_floors/62_trap_floor_spikes.png"),
                decor("SKULL_BEIGE", "Skull Beige", false,
                        ITEM_DIR + "skulls/13_skull_beige.png"),
                decor("SKULL_DARK", "Skull Dark", false,
                        ITEM_DIR + "skulls/14_skull_dark.png"),
                decor("TOMBSTONE_CRACK", "Tombstone Crack", false,
                        ITEM_DIR + "tombstones/23_tombstone_crack.png"),
                decor("TOMBSTONE_LINES", "Tombstone Lines", false,
                        ITEM_DIR + "tombstones/24_tombstone_lines.png"),
                decor("TOMBSTONE_CROSS", "Tombstone Cross", false,
                        ITEM_DIR + "tombstones/25_tombstone_cross.png"),
                decor("TOMBSTONE_SKULL", "Tombstone Skull", false,
                        ITEM_DIR + "tombstones/26_tombstone_skull.png"),
                chest("CHEST", "Locked Blue Chest", "01_chest_closed_blue_trim.png", true),
                chest("LOCKED_CHEST", "Locked Gold Chest", "02_chest_closed_gold_trim.png", true),
                chest("CHEST_ORNATE_TAN", "Locked Ornate Tan Chest", "07_ornate_chest_gold_tan.png", true),
                chest("CHEST_ORNATE_RED", "Locked Ornate Red Chest", "08_ornate_chest_gold_red.png", true),
                chest("CHEST_ORNATE_BLUE", "Locked Ornate Blue Chest", "09_ornate_chest_gold_blue.png", true),
                chest("CHEST_ORANGE_FRAME_1", "Locked Orange Chest 1", "10_orange_chest_closed_frame1.png", true),
                chest("CHEST_ORANGE_FRAME_2", "Locked Orange Chest 2", "13_orange_chest_closed_frame2.png", true),
                emptyChest("CHEST_OPEN_EMPTY_BLUE", "Open Empty Blue Chest",
                        "03_chest_open_empty_blue_trim.png"),
                emptyChest("CHEST_OPEN_EMPTY_GOLD", "Open Empty Gold Chest",
                        "04_chest_open_empty_gold_trim.png"),
                emptyChest("CHEST_ORANGE_OPEN_EMPTY_1", "Open Empty Orange Chest 1",
                        "11_orange_chest_open_empty_frame1.png"),
                emptyChest("CHEST_ORANGE_OPEN_EMPTY_2", "Open Empty Orange Chest 2",
                        "12_orange_chest_open_empty_frame2.png"),
                chest("CHEST_OPEN_LOOT_BLUE", "Open Loot Blue Chest",
                        "05_chest_open_loot_blue_trim.png", false),
                chest("CHEST_OPEN_LOOT_GOLD", "Open Loot Gold Chest",
                        "06_chest_open_loot_gold_trim.png", false),
                chest("CHEST_ORANGE_OPEN_LOOT_1", "Open Loot Orange Chest 1",
                        "14_orange_chest_open_loot_frame1.png", false),
                chest("CHEST_ORANGE_OPEN_LOOT_2", "Open Loot Orange Chest 2",
                        "15_orange_chest_open_loot_frame2.png", false),
                object("BAG_BROWN", "Bag Brown", () -> bag("Bag Brown", "19_bag_brown.png")),
                object("BAG_BLUE", "Bag Blue", () -> bag("Bag Blue", "20_bag_blue.png")),
                wallSearch("CRATE", "Searchable Crate Wood Tall",
                        () -> new Crate(Crate.WOOD_TALL_SPRITE, null)),
                wallSearch("CRATE_WOOD_RIGHT", "Searchable Crate Wood Right",
                        () -> new Crate(Crate.WOOD_RIGHT_SPRITE, null)),
                wallSearch("CRATE_ORANGE", "Searchable Crate Orange",
                        () -> new Crate(Crate.ORANGE_TALL_SPRITE, null)),
                wallSearch("HOLE", "Hole 1", () -> new Hole(Hole.SPRITE, null)),
                wallSearch("HOLE_2", "Hole 2", () -> new Hole(Hole.SPRITE_2, null)),
                wallSearch("HOLE_3", "Hole 3", () -> new Hole(Hole.SPRITE_3, null)),
                wallSearch("GRILL", "Grill 1", () -> new Grill(Grill.HORIZONTAL_SPRITE, null)),
                wallSearch("GRILL_2", "Grill 2", () -> new Grill(Grill.VERTICAL_SPRITE, null)),
                wallSearch("GARGOYLE", "Gargoyle Red",
                        () -> new Gargoyle(Gargoyle.RED_LEFT_SPRITE, null)),
                wallSearch("GARGOYLE_GREEN", "Gargoyle Green",
                        () -> new Gargoyle(Gargoyle.GREEN_LEFT_SPRITE, null)),
                wallSearch("GARGOYLE_BLUE", "Gargoyle Blue",
                        () -> new Gargoyle(Gargoyle.CYAN_LEFT_SPRITE, null)),
                wallSearch("MISSING_BRICK", "Missing Brick 1",
                        () -> new MissingBrick(MissingBrick.SPRITE_1, null)),
                wallSearch("MISSING_BRICK_2", "Missing Brick 2",
                        () -> new MissingBrick(MissingBrick.SPRITE_2, null)),
                object("COLUMN", "Breakable Column Gray", () -> new Column(Column.GRAY_SPRITE)),
                object("COLUMN_PURPLE", "Breakable Column Purple", () -> new Column(Column.PURPLE_SPRITE)),
                object("COLUMN_TOP", "Breakable Column Top", () -> new Column(Column.WALL_TOP_SPRITE)),
                object("VASE", "Breakable Vase", Vase::new),
                object("WATER_PIPE", "Breakable Water Pipe", () -> new WaterPipe(WaterPipe.LARGE_RING_SPRITE)),
                wallSearch("BREAKABLE_CRATE", "Breakable Crate Wood Tall",
                        () -> new Crate(Crate.BREAKABLE_WOOD_TALL_SPRITE, null)),
                wallSearch("BREAKABLE_CRATE_WOOD_RIGHT", "Breakable Crate Wood Right",
                        () -> new Crate(Crate.BREAKABLE_WOOD_RIGHT_SPRITE, null)),
                wallSearch("BREAKABLE_CRATE_ORANGE", "Breakable Crate Orange",
                        () -> new Crate(Crate.BREAKABLE_ORANGE_TALL_SPRITE, null)),
                object("HEAL", "Heal", HealPotion::new),
                object("ENERGY", "Energy", EnergyPotion::new),
                object("MANA", "Mana", ManaPotion::new),
                object("BOOK", "Red Book", () -> new Book("Red Book", "A dusty dungeon journal.")),
                object("KEY", "Key Silver", () -> new Key("silver", KeyColor.SILVER)),
                object("KEY_OLIVE", "Key Olive", () -> new Key("olive", KeyColor.OLIVE)),
                object("KEY_GOLD", "Key Gold", () -> new Key("gold", KeyColor.GOLD)),
                object("KEY_ORANGE", "Key Orange", () -> new Key("orange", KeyColor.ORANGE)),
                object("KEY_BENT_SILVER", "Key Bent Silver",
                        () -> new Key("bent-silver", KeyColor.BENT_SILVER)),
                object("KEY_LONG_GOLD", "Key Long Gold", () -> new Key("long-gold", KeyColor.LONG_GOLD)),
                object("WEAPON", "Weapon", () -> new Weapon(WeaponCatalog.get().byId("W002"))),
                b23Weapon("B23_BOW", "Wooden Bow"),
                b23Weapon("B23_WAND", "Magic Wand"),
                object("ARMOR", "Armor", () -> new Armor("Leather Armor", 3)),
                object("RING", "Ring Red Gem",
                        () -> ring("Power Ring", RingEffectType.STRENGTH, 3, "10_ring_red_gem.png")),
                object("RING_GREEN", "Ring Green Gem",
                        () -> ring("Energy Ring", RingEffectType.ENERGY, 6, "11_ring_green_gem.png")),
                object("RING_BLUE", "Ring Blue Gem",
                        () -> ring("Mana Ring", RingEffectType.MANA, 6, "12_ring_blue_gem.png")),
                object("COIN_SINGLE", "Gold Coin", () -> coin(1, "15_coin_gold_single.png")),
                object("COIN_PILE", "Gold Coin Pile", () -> coin(10, "20_coin_pile_gold.png")),
                object("TREASURE_GEM_WHITE", "White Gem",
                        () -> treasure("White Gem", "16_gem_white.png")),
                object("TREASURE_BAR_ORANGE", "Orange Bar",
                        () -> treasure("Orange Bar", "17_bar_orange.png")),
                object("TREASURE_BAR_GOLD", "Gold Bar",
                        () -> treasure("Gold Bar", "18_bar_gold_orange.png")),
                object("TREASURE_NUGGET_GOLD", "Gold Nugget",
                        () -> treasure("Gold Nugget", "19_nugget_gold.png")),
                object("VALUABLE_CRYSTAL", "Crystal Shard",
                        () -> valuable("Crystal Shard", "crystal_shard_64x64.png")),
                object("VALUABLE_IDOL", "Golden Idol",
                        () -> valuable("Golden Idol", "golden_idol_64x64.png")),
                object("VALUABLE_AMULET", "Ancient Amulet",
                        () -> valuable("Ancient Amulet", "ancient_amulet_64x64.png")),
                object("VALUABLE_CHALICE", "Ruby Chalice",
                        () -> valuable("Ruby Chalice", "ruby_chalice_64x64.png")),
                object("VALUABLE_TIARA", "Silver Tiara",
                        () -> valuable("Silver Tiara", "silver_tiara_64x64.png")),
                object("VALUABLE_PENDANT", "Emerald Pendant",
                        () -> valuable("Emerald Pendant", "emerald_pendant_64x64.png")),
                object("VALUABLE_DAGGER_HILT", "Obsidian Dagger Hilt",
                        () -> valuable("Obsidian Dagger Hilt", "obsidian_dagger_hilt_64x64.png")),
                new BuildTool("VALUABLE", "Random Valuable", FLOOR_OBJECT,
                        () -> ValuableItemCatalog.randomValuable(ThreadLocalRandom.current()),
                        () -> valuable("Random Valuable", "crystal_shard_64x64.png")));

        // Expose every catalog weapon as its own palette entry so build mode can
        // place any sprite/rarity, not just the generic sword. B23_BOW/B23_WAND
        // are already listed above, so skip their ids to avoid duplicates.
        List<BuildTool> allTools = new java.util.ArrayList<>(baseTools);
        for (WeaponType type : catalogWeaponsForPalette(baseTools)) {
            allTools.add(weaponTool(type));
        }
        tools = List.copyOf(allTools);

        byId = new LinkedHashMap<>();
        for (BuildTool tool : tools) {
            byId.put(tool.id(), tool);
        }
        // Kept out of the visible palette: older build maps/tests still refer to
        // POOL, but its sprite is now represented by the gargoyle variants.
        byId.put("POOL", wallSearch("POOL", "Legacy Gargoyle Pool",
                () -> new Pool(Pool.CYAN_DRIP_SPRITE, new ManaPotion())));
        byId.put("PEDESTAL", wallSearch("PEDESTAL", "Legacy Pedestal",
                () -> new Pedestal(new EnergyPotion())));
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

    public static boolean isDoorSpriteResource(String spriteResource) {
        return CLOSED_DOOR_SPRITE_RESOURCE.equals(spriteResource)
                || OPEN_DOOR_SPRITE_RESOURCE.equals(spriteResource);
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
        if (item instanceof Weapon weapon) {
            String catalogId = weapon.getType().id();
            if (byId.containsKey(catalogId)) {
                return Optional.of(catalogId);
            }
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

    private static BuildTool wallDecor(String id, String label, String spriteResource) {
        return new BuildTool(id, label, WALL_OBJECT,
                () -> new DecorativeObject(label, true, spriteResource));
    }

    private static BuildTool doorDecor(String id, String label, boolean blocking, String spriteResource) {
        return new BuildTool(id, label, DOOR_OBJECT,
                () -> new DecorativeObject(label, blocking, spriteResource));
    }

    private static BuildTool decor(String id, String label, boolean blocking, String spriteResource) {
        return object(id, label, () -> new DecorativeObject(label, blocking, spriteResource));
    }

    private static BuildTool chest(String id, String label, String spriteFile, boolean locked) {
        return object(id, label, () -> {
            Chest chest = locked
                    ? Chest.locked(label, 16, "silver", ITEM_DIR + "chests/" + spriteFile)
                    : new Chest(label, 16, ITEM_DIR + "chests/" + spriteFile);
            chest.addItem(locked ? new ManaPotion() : new HealPotion());
            return chest;
        });
    }

    private static BuildTool emptyChest(String id, String label, String spriteFile) {
        return object(id, label, () -> new Chest(label, 16, ITEM_DIR + "chests/" + spriteFile));
    }

    private static Container bag(String name, String spriteFile) {
        return new Container(name, false, false, 8, true, ITEM_DIR + "bags/" + spriteFile);
    }

    private static Ring ring(String name, RingEffectType effectType, int bonus, String spriteFile) {
        return new Ring(name, effectType, bonus, ITEM_DIR + "rings/" + spriteFile);
    }

    private static Coin coin(int value, String spriteFile) {
        return new Coin(value, ITEM_DIR + "golds_coins/" + spriteFile);
    }

    private static ValuableItem treasure(String name, String spriteFile) {
        return new ValuableItem(name, ITEM_DIR + "golds_coins/" + spriteFile);
    }

    private static ValuableItem valuable(String name, String spriteFile) {
        return new ValuableItem(name, VALUABLE_SPRITE_DIR + spriteFile);
    }

    private static BuildTool b23Weapon(String catalogId, String label) {
        return object(catalogId, label, () -> new Weapon(WeaponCatalog.get().byId(catalogId)));
    }

    /** Palette entry for a catalog weapon; the label shows its attack for builders. */
    private static BuildTool weaponTool(WeaponType type) {
        String id = type.id();
        String label = type.displayName() + " (" + type.baseAttack() + ")";
        return object(id, label, () -> new Weapon(WeaponCatalog.get().byId(id)));
    }

    /**
     * Catalog weapons to surface in the palette, sorted by category then ascending
     * attack so each subclass reads as a low-to-high block. Ids already present in
     * {@code baseTools} (the B23 hero weapons) are skipped.
     */
    private static List<WeaponType> catalogWeaponsForPalette(List<BuildTool> baseTools) {
        java.util.Set<String> existing = new java.util.HashSet<>();
        for (BuildTool tool : baseTools) {
            existing.add(tool.id());
        }
        List<WeaponType> weapons = new java.util.ArrayList<>();
        for (WeaponType type : WeaponCatalog.get().all()) {
            if (!existing.contains(type.id())) {
                weapons.add(type);
            }
        }
        weapons.sort(java.util.Comparator.comparing(WeaponType::category)
                .thenComparingInt(WeaponType::baseAttack)
                .thenComparing(WeaponType::id));
        return weapons;
    }
}
