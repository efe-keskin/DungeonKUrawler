package model;

/**
 * A single purchasable entry in the shop (GoF: lightweight prototype/factory).
 * Holds display data plus a price; {@link #createItem()} mints a fresh model
 * item to drop into the buyer's {@link FullGameInventory}, so repeated purchases
 * never share one instance.
 *
 * <p>Placeholder offers reuse the valuable-item sprites until the real shop
 * catalog (pets, permanent equipment) is designed.
 */
public final class ShopOffer {

    private final String name;
    private final int price;
    private final String spritePath;

    public ShopOffer(String name, int price, String spritePath) {
        this.name = name;
        this.price = Math.max(0, price);
        this.spritePath = spritePath;
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
        return new ValuableItem(name, spritePath);
    }
}
