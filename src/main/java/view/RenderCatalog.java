package view;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import model.Armor;
import model.Container;
import model.Entity;
import model.HealPotion;
import model.Item;
import model.Knight;
import model.ManaPotion;
import model.Sorcerer;
import model.Weapon;

/**
 * Central registry for Swing render metadata so views don't need per-type branching.
 */
public final class RenderCatalog {

    public record ItemRenderData(String marker, Color color, String spritePath) {
    }

    public record EntityRenderData(Color color) {
    }

    private static final ItemRenderData DEFAULT_ITEM = new ItemRenderData("ITM", new Color(95, 85, 120), null);
    private static final EntityRenderData DEFAULT_ENTITY = new EntityRenderData(Color.LIGHT_GRAY);

    private static final Map<Class<?>, ItemRenderData> ITEM_DATA = new HashMap<>();
    private static final Map<Class<?>, EntityRenderData> ENTITY_DATA = new HashMap<>();
    private static final Map<String, BufferedImage> SPRITES = new HashMap<>();

    static {
        ITEM_DATA.put(HealPotion.class, new ItemRenderData("POT", new Color(70, 120, 70), "/items_objects/healpotion.png"));
        ITEM_DATA.put(ManaPotion.class, new ItemRenderData("POT", new Color(70, 120, 70), "/items_objects/manapotion.png"));
        ITEM_DATA.put(Weapon.class, new ItemRenderData("WPN", new Color(120, 85, 40), null));
        ITEM_DATA.put(Armor.class, new ItemRenderData("ARM", new Color(60, 95, 130), null));
        ITEM_DATA.put(Container.class, new ItemRenderData("CNT", new Color(130, 105, 65), null));

        ENTITY_DATA.put(Knight.class, new EntityRenderData(new Color(220, 55, 55)));
        ENTITY_DATA.put(Sorcerer.class, new EntityRenderData(new Color(160, 70, 220)));
    }

    private RenderCatalog() {
    }

    public static ItemRenderData getItemData(Item item) {
        if (item == null) {
            return DEFAULT_ITEM;
        }
        return ITEM_DATA.getOrDefault(item.getClass(), DEFAULT_ITEM);
    }

    public static EntityRenderData getEntityData(Entity entity) {
        if (entity == null) {
            return DEFAULT_ENTITY;
        }
        return ENTITY_DATA.getOrDefault(entity.getClass(), DEFAULT_ENTITY);
    }

    public static BufferedImage loadSprite(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return SPRITES.computeIfAbsent(path, RenderCatalog::readSprite);
    }

    private static BufferedImage readSprite(String path) {
        try (InputStream in = RenderCatalog.class.getResourceAsStream(path)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
            // Missing sprite falls back to color/marker metadata.
        }
        return null;
    }
}
