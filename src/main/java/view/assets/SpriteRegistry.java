package view.assets;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Chest;
import model.Entity;
import model.HealPotion;
import model.Item;
import model.Knight;
import model.ManaPotion;
import model.Sorcerer;

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

    private static final List<AssetId> HERO_ANIMATION_FRAMES = List.of(
            AssetId.HERO_FRAME_1,
            AssetId.HERO_FRAME_2,
            AssetId.HERO_FRAME_3,
            AssetId.HERO_FRAME_4,
            AssetId.HERO_FRAME_5);

    static {
        registerEntity(Knight.class, AssetId.KNIGHT);
        registerEntity(Sorcerer.class, AssetId.SORCERER);

        registerItem(HealPotion.class, AssetId.HEAL_POTION);
        registerItem(ManaPotion.class, AssetId.MANA_POTION);
        registerItem(Chest.class, AssetId.CHEST_CLOSED);
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
        return item == null ? null : ITEM_SPRITES.get(item.getClass());
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

    public static BufferedImage spriteFor(Item item) {
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
