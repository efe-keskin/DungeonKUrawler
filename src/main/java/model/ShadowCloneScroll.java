package model;

import java.util.List;

/**
 * Collectible readable scroll that summons a shadow clone when read.
 */
public class ShadowCloneScroll extends Scroll {

    /** Canonical display name shared by every spawn site. */
    public static final String DISPLAY_NAME = "Shadow Clone Scroll";
    private static final String FLAVOR_TEXT = "A faded scroll that hums faintly.";

    public ShadowCloneScroll(String name, String text) {
        super(name, text);
    }

    /** Standard scroll as dropped by breakables, chests, and searchables. */
    public ShadowCloneScroll() {
        this(DISPLAY_NAME, FLAVOR_TEXT);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.READ, ItemAction.DISCARD);
    }
}
