package model;

import java.util.List;
import java.util.Random;

/**
 * Factory (GoF) for {@link ValuableItem} instances drawn from a hard-coded
 * placeholder pool. Once the real valuable-items design lands, this catalog
 * is the single place to swap in: persistent IDs, sprites per valuable, etc.
 */
public final class ValuableItemCatalog {

    /** Classpath folder holding the 64x64 valuable sprites. */
    private static final String SPRITE_DIR = "/items_objects/valuable_items/";

    /** One catalog entry: display name paired with its sprite file. */
    private record Entry(String name, String spriteFile) {
    }

    private static final List<Entry> POOL = List.of(
            new Entry("Crystal Shard", "crystal_shard_64x64.png"),
            new Entry("Golden Idol", "golden_idol_64x64.png"),
            new Entry("Ancient Amulet", "ancient_amulet_64x64.png"),
            new Entry("Ruby Chalice", "ruby_chalice_64x64.png"),
            new Entry("Silver Tiara", "silver_tiara_64x64.png"),
            new Entry("Emerald Pendant", "emerald_pendant_64x64.png"),
            new Entry("Obsidian Dagger Hilt", "obsidian_dagger_hilt_64x64.png"));

    private ValuableItemCatalog() {
    }

    /** Picks one entry from the pool and returns a fresh sprite-bound {@link ValuableItem}. */
    public static ValuableItem randomValuable(Random random) {
        Random r = random == null ? new Random() : random;
        Entry entry = POOL.get(r.nextInt(POOL.size()));
        return new ValuableItem(entry.name(), SPRITE_DIR + entry.spriteFile());
    }
}
