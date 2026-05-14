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
    MANA_POTION("/items_objects/manapotion.png"),

    // UI surfaces.
    INVENTORY_BACKGROUND("/Inventory x4.png"),
    INVENTORY_CHEST_ICON("/inventorychest.png"),
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
