package model;

import java.util.function.Supplier;

/**
 * A single purchasable entry in the shop (GoF: lightweight factory). Holds
 * display data plus a price; {@link #createItem()} mints a fresh model item to
 * drop into the buyer's {@link FullGameInventory}, so repeated purchases never
 * share one instance.
 *
 * <p>The {@code factory} decouples the offer from the concrete item type: a
 * valuable offer mints a {@link ValuableItem}, a pet offer mints a {@link Pet}.
 */
public final class ShopOffer {

    private final String name;
    private final int price;
    private final String spritePath;
    private final Supplier<Item> factory;

    /** Offer for a generic collectible valuable (default factory). */
    public ShopOffer(String name, int price, String spritePath) {
        this(name, price, spritePath, () -> new ValuableItem(name, spritePath));
    }

    /** Offer with an explicit item factory (e.g. {@code () -> new PenguinPet()}). */
    public ShopOffer(String name, int price, String spritePath, Supplier<Item> factory) {
        this.name = name;
        this.price = Math.max(0, price);
        this.spritePath = spritePath;
        this.factory = factory;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String spriteResource() {
        return spritePath;
    }

    /** A fresh item instance representing this offer, ready for the inventory. */
    public Item createItem() {
        return factory.get();
    }
}
