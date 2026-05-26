package view.assets;

/**
 * Stable, hand-curated identifiers for view assets. Decouples calling code from
 * resource paths: if a file moves under {@code resources/}, only the enum entry
 * changes. Used by {@link AssetManager} as the lookup key.
 *
 * <p>For the large, data-driven ambience tileset under
 * {@code resources/background_floor/}, see {@link AmbienceCatalog} — it keys
 * assets by their CSV label rather than by enum.
 */
public enum AssetId {

    // Hero walk-cycle frames.
    HERO_FRAME_1("/characters/hero1.png"),
    HERO_FRAME_2("/characters/hero2.png"),
    HERO_FRAME_3("/characters/hero3.png"),
    HERO_FRAME_4("/characters/hero4.png"),
    HERO_FRAME_5("/characters/hero5.png"),

    // Enemies.
    KNIGHT("/characters/knight1.png"),
    SORCERER("/characters/sorcerer1.png"),
    WIZARD("/characters/wizard1.png"),

    // Items.
    HEAL_POTION("/items_objects/healpotion.png"),
    ENERGY_POTION("/items_objects/energypotion.png"),
    MANA_POTION("/items_objects/manapotion.png"),
    COIN_PILE("/items_keys_extracted/assets/20_coin_pile_gold.png"),
    RING_RED_GEM("/items_keys_extracted/assets/10_ring_red_gem.png"),
    BOOK_RED("/items_keys_extracted/assets/22_book_red.png"),
    GEM_WHITE("/items_keys_extracted/assets/16_gem_white.png"),
    DEFEATED_ENEMY_MARKER("/items_keys_extracted/assets/26_tombstone_skull.png"),

    // Containers.
    CHEST_CLOSED("/chest_models_plus_crates_sacks_v6/assets/01_chest_closed_blue_trim.png"),
    CHEST_OPEN("/chest_models_plus_crates_sacks_v6/assets/05_chest_open_loot_blue_trim.png"),

    // Keys.
    KEY_OLIVE("/items_keys_extracted/assets/01_key_olive.png"),
    KEY_SILVER("/items_keys_extracted/assets/02_key_silver.png"),
    KEY_GOLD("/items_keys_extracted/assets/03_key_gold.png"),
    KEY_ORANGE("/items_keys_extracted/assets/04_key_orange.png"),
    KEY_BENT_SILVER("/items_keys_extracted/assets/05_key_bent_silver.png"),
    KEY_LONG_GOLD("/items_keys_extracted/assets/06_key_long_gold.png"),

    // UI surfaces.
    INVENTORY_BACKGROUND("/Inventory x4.png"),
    INVENTORY_CHEST_ICON("/inventorychest.png"),
    CHEST_BACKGROUND("/chest_models_plus_crates_sacks_v6/bag - empty.png"),
    MAIN_MENU_BACKGROUND("/mainmenu_background.jpg"),
    HELP_QUESTION_MARK("/questionmark_minecraft.png");

    private final String resourcePath;

    AssetId(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String resourcePath() {
        return resourcePath;
    }
}
