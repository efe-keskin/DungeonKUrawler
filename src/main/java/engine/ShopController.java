package engine;

import java.util.Objects;

import model.FullGameInventory;
import model.Item;
import model.ShopCatalog;
import model.ShopOffer;
import model.ValuableItem;

/**
 * GRASP Controller for the shop use cases (UC-6 buy, UC-7 sell). It mediates
 * between the {@link view.ShopWindow} and the player's persistent
 * {@link FullGameInventory}, keeping gold-checking and inventory mutation out of
 * the Swing view. Persistence is the caller's concern (the session controller
 * saves on shop close); this class only mutates the in-memory model.
 */
public final class ShopController {

    /** Fraction of an item's notional worth refunded on sale. */
    private static final int SELL_PRICE = 25;

    public enum BuyResult {
        SUCCESS,
        NOT_ENOUGH_GOLD,
        INVALID
    }

    public enum SellResult {
        SUCCESS,
        NOT_SELLABLE,
        NOT_OWNED,
        INVALID
    }

    private final FullGameInventory inventory;
    private final ShopCatalog catalog;

    public ShopController(FullGameInventory inventory, ShopCatalog catalog) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.catalog = catalog == null ? new ShopCatalog() : catalog;
    }

    public ShopCatalog getCatalog() {
        return catalog;
    }

    public FullGameInventory getInventory() {
        return inventory;
    }

    /**
     * UC-6: buys {@code offer} if the player can afford it. On success the price
     * is deducted and a fresh item is added to the persistent inventory.
     */
    public BuyResult buy(ShopOffer offer) {
        if (offer == null) {
            return BuyResult.INVALID;
        }
        if (!inventory.spend(offer.getPrice())) {
            return BuyResult.NOT_ENOUGH_GOLD;
        }
        inventory.add(offer.createItem());
        return BuyResult.SUCCESS;
    }

    /**
     * UC-7: sells a persistent item for gold. Only valuables are sellable for
     * now; the item must currently be owned.
     */
    public SellResult sell(Item item) {
        if (item == null) {
            return SellResult.INVALID;
        }
        if (!(item instanceof ValuableItem)) {
            return SellResult.NOT_SELLABLE;
        }
        if (!inventory.remove(item)) {
            return SellResult.NOT_OWNED;
        }
        inventory.earn(sellPriceOf(item));
        return SellResult.SUCCESS;
    }

    /** Gold paid out when selling {@code item}. Flat for the placeholder catalog. */
    public int sellPriceOf(Item item) {
        return SELL_PRICE;
    }
}
