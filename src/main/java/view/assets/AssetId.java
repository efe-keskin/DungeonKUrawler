package view.assets;

/**
 * Stable, hand-curated identifiers for view assets. Decouples calling code from
 * resource paths: if a file moves under {@code resources/}, only the enum entry
 * changes. Used by {@link AssetManager} as the lookup key.
 *
 * <p>For the large, data-driven ambience tileset under
 * {@code resources/background_floor/}, see {@link AmbienceCatalog}; it keys
 * assets by their CSV label rather than by enum.
 */
public enum AssetId {

    // Hero walk-cycle frames.
    HERO_FRAME_1("/characters/hero1.png"),
    HERO_FRAME_2("/characters/hero2.png"),
    HERO_FRAME_3("/characters/hero3.png"),
    HERO_FRAME_4("/characters/hero4.png"),
    HERO_FRAME_5("/characters/hero5.png"),
    HERO_FRAME_6("/characters/hero6.png"),
    HERO_FRAME_7("/characters/hero7.png"),
    HERO_FRAME_8("/characters/hero8.png"),
    HERO_FRAME_9("/characters/hero9.png"),

    // Enemies.
    KNIGHT("/characters/knight1.png"),
    RED_KNIGHT("/characters/redknight1.png"),
    SORCERER("/characters/wizard1.png"),
    WIZARD("/characters/wizard1.png"),
    RED_WIZARD("/characters/redWizard1.png"),

    // Items.
    HEAL_POTION("/items/potions/07_potion_red.png"),
    ENERGY_POTION("/items/potions/09_potion_green.png"),
    MANA_POTION("/items/potions/08_potion_blue.png"),
    COIN_PILE("/items/golds_coins/20_coin_pile_gold.png"),
    RING_RED_GEM("/items/rings/10_ring_red_gem.png"),
    BOOK_RED("/items/books/22_book_red.png"),
    SHADOW_CLONE_SCROLL("/items/scrolls/shadow_clone_scroll.png"),
    GEM_WHITE("/items/golds_coins/16_gem_white.png"),
    DEFEATED_ENEMY_MARKER("/items/tombstones/26_tombstone_skull.png"),

    // Torch animation frames (8 frames for inventory animation).
    TORCH_FRAME_1("/background_floor/assets/torches/50_torch_lit_01.png"),
    TORCH_FRAME_2("/background_floor/assets/torches/51_torch_lit_02.png"),
    TORCH_FRAME_3("/background_floor/assets/torches/52_torch_lit_03.png"),
    TORCH_FRAME_4("/background_floor/assets/torches/53_torch_lit_04.png"),
    TORCH_FRAME_5("/background_floor/assets/torches/54_torch_lit_05.png"),
    TORCH_FRAME_6("/background_floor/assets/torches/55_torch_lit_06.png"),
    TORCH_FRAME_7("/background_floor/assets/torches/56_torch_lit_07.png"),
    TORCH_FRAME_8("/background_floor/assets/torches/57_torch_lit_08.png"),

    // Containers.
    CHEST_CLOSED("/items/chests/01_chest_closed_blue_trim.png"),
    CHEST_OPEN("/items/chests/05_chest_open_loot_blue_trim.png"),

    // Keys.
    KEY_OLIVE("/items/keys/01_key_olive.png"),
    KEY_SILVER("/items/keys/02_key_silver.png"),
    KEY_GOLD("/items/keys/03_key_gold.png"),
    KEY_ORANGE("/items/keys/04_key_orange.png"),
    KEY_BENT_SILVER("/items/keys/05_key_bent_silver.png"),
    KEY_LONG_GOLD("/items/keys/06_key_long_gold.png"),

    // UI surfaces.
    INVENTORY_BACKGROUND("/inventorychest.png"),
    INVENTORY_CHEST_ICON("/inventorychest.png"),
    CHEST_BACKGROUND("/inventorychest.png"),
    MAIN_MENU_BACKGROUND("/mainmenu_background.jpg"),
    HELP_QUESTION_MARK("/questionmark_minecraft.png"),
    AUDIO_ON("/audio_on.png"),
    AUDIO_OFF("/audio_off.png");

    private final String resourcePath;

    AssetId(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String resourcePath() {
        return resourcePath;
    }
}
