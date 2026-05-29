package view.assets;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Chest;
import model.Coin;
import model.Book;
import model.BossEnemy;
import model.DefeatedEnemyMarker;
import model.Entity;
import model.HealPotion;
import model.Item;
import model.Key;
import model.KeyColor;
import model.Knight;
import model.EnergyPotion;
import model.ManaPotion;
import model.Ring;
import model.Sorcerer;
import model.ValuableItem;

/**
 * Maps model types (entities, items) to their {@link AssetId}. Lets the view
 * pick a sprite without {@code instanceof} chains in the renderer (GRASP
 * Polymorphism via type lookup; GoF Registry).
 *
 * <p>The model stays image-free: only the view side knows that a {@link Knight}
 * is rendered as {@code knight1.png}. New entity/item types register here
 * rather than adding another branch to {@code GamePanel#spriteFor}.
 */
public final class SpriteRegistry {

    private static final Map<Class<? extends Entity>, AssetId> ENTITY_SPRITES = new HashMap<>();
    private static final Map<Class<? extends Item>, AssetId> ITEM_SPRITES = new HashMap<>();
    /** Keys share one class but render per-color, so they get their own table. */
    private static final Map<KeyColor, AssetId> KEY_SPRITES = new EnumMap<>(KeyColor.class);

    private static final List<AssetId> HERO_ANIMATION_FRAMES = List.of(
            AssetId.HERO_FRAME_1,
            AssetId.HERO_FRAME_2,
            AssetId.HERO_FRAME_3,
            AssetId.HERO_FRAME_4,
            AssetId.HERO_FRAME_5,
            AssetId.HERO_FRAME_6,
            AssetId.HERO_FRAME_7,
            AssetId.HERO_FRAME_8,
            AssetId.HERO_FRAME_9);

    static {
        registerEntity(Knight.class, AssetId.KNIGHT);
        registerEntity(Sorcerer.class, AssetId.SORCERER);

        registerItem(HealPotion.class, AssetId.HEAL_POTION);
        registerItem(EnergyPotion.class, AssetId.ENERGY_POTION);
        registerItem(ManaPotion.class, AssetId.MANA_POTION);
        registerItem(Coin.class, AssetId.COIN_PILE);
        registerItem(Ring.class, AssetId.RING_RED_GEM);
        registerItem(Book.class, AssetId.BOOK_RED);
        registerItem(ValuableItem.class, AssetId.GEM_WHITE);
        registerItem(DefeatedEnemyMarker.class, AssetId.DEFEATED_ENEMY_MARKER);
        registerItem(Chest.class, AssetId.CHEST_CLOSED);

        registerKey(KeyColor.OLIVE, AssetId.KEY_OLIVE);
        registerKey(KeyColor.SILVER, AssetId.KEY_SILVER);
        registerKey(KeyColor.GOLD, AssetId.KEY_GOLD);
        registerKey(KeyColor.ORANGE, AssetId.KEY_ORANGE);
        registerKey(KeyColor.BENT_SILVER, AssetId.KEY_BENT_SILVER);
        registerKey(KeyColor.LONG_GOLD, AssetId.KEY_LONG_GOLD);
    }

    public static void registerKey(KeyColor color, AssetId id) {
        KEY_SPRITES.put(color, id);
    }

    private SpriteRegistry() {
    }

    public static void registerEntity(Class<? extends Entity> type, AssetId id) {
        ENTITY_SPRITES.put(type, id);
    }

    public static void registerItem(Class<? extends Item> type, AssetId id) {
        ITEM_SPRITES.put(type, id);
    }

    public static AssetId assetFor(Entity entity) {
        return entity == null ? null : ENTITY_SPRITES.get(entity.getClass());
    }

    public static AssetId assetFor(Item item) {
        if (item == null) {
            return null;
        }
        if (item instanceof Key key) {
            return KEY_SPRITES.get(key.getColor());
        }
        return ITEM_SPRITES.get(item.getClass());
    }

    /**
     * Convenience: look up the sprite via {@link AssetManager}, falling back to
     * a sibling asset (used so a missing Sorcerer falls back to Wizard).
     */
    public static BufferedImage spriteFor(Entity entity) {
        AssetId primary = assetFor(entity);
        if (primary == null) {
            return null;
        }
        if (primary == AssetId.SORCERER) {
            return AssetManager.get().imageOrFallback(AssetId.SORCERER, AssetId.WIZARD);
        }
        return AssetManager.get().image(primary);
    }

    public static BufferedImage walkFrameFor(Entity entity, int index) {
        if (entity == null) {
            return null;
        }
        String prefix;
        if (entity instanceof Knight) {
            prefix = "/characters/bot";
        } else if (entity instanceof Sorcerer) {
            prefix = "/characters/wizard";
        } else if (entity instanceof BossEnemy) {
            int safe = Math.floorMod(index, 6) + 1;
            String suffix = safe < 10 ? "0" + safe : Integer.toString(safe);
            BufferedImage frame = AssetManager.get().image("/characters/boss1_move_" + suffix + ".png");
            return frame != null ? frame : spriteFor(entity);
        } else {
            return spriteFor(entity);
        }
        int safe = Math.floorMod(index, 9) + 1;
        BufferedImage frame = AssetManager.get().image(prefix + safe + ".png");
        return frame != null ? frame : spriteFor(entity);
    }

    public static BufferedImage spriteFor(Item item) {
        if (item == null) {
            return null;
        }
        String override = item.spriteResource();
        if (override != null) {
            return AssetManager.get().image(override);
        }
        AssetId id = assetFor(item);
        return id == null ? null : AssetManager.get().image(id);
    }

    public static List<AssetId> heroAnimationFrames() {
        return HERO_ANIMATION_FRAMES;
    }

    public static BufferedImage heroFrame(int index) {
        if (HERO_ANIMATION_FRAMES.isEmpty()) {
            return null;
        }
        int safe = Math.floorMod(index, HERO_ANIMATION_FRAMES.size());
        return AssetManager.get().image(HERO_ANIMATION_FRAMES.get(safe));
    }

    public static int heroFrameCount() {
        return HERO_ANIMATION_FRAMES.size();
    }
}
