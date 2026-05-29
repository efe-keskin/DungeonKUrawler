package model;

import java.util.List;

/**
 * The set of {@link ShopOffer}s the vendor sells (GoF Factory / single source of
 * truth, mirroring {@link ValuableItemCatalog}). For now it holds a small
 * placeholder line-up so the BUY flow is exercisable; swap these for real pets /
 * permanent equipment once that content is designed.
 */
public final class ShopCatalog {

    private static final String SPRITE_DIR = "/items_objects/valuable_items/";

    private final List<ShopOffer> offers;

    public ShopCatalog() {
        this(List.of(
                new ShopOffer("Crystal Shard", 40, SPRITE_DIR + "crystal_shard_64x64.png"),
                new ShopOffer("Golden Idol", 75, SPRITE_DIR + "golden_idol_64x64.png"),
                new ShopOffer("Ruby Chalice", 120, SPRITE_DIR + "ruby_chalice_64x64.png")));
    }

    public ShopCatalog(List<ShopOffer> offers) {
        this.offers = List.copyOf(offers);
    }

    public List<ShopOffer> offers() {
        return offers;
    }
}
