package engine;

import java.util.EnumMap;
import java.util.Map;

import model.Armor;
import model.Container;
import model.Hero;
import model.Item;
import model.ItemAction;
import model.Potion;
import model.Readable;
import model.Ring;
import model.Weapon;

/**
 * Strategy/Command registry for {@link ItemAction}. Decouples the action tag
 * chosen by the player from the effect produced when it is applied — adding a
 * new action becomes a matter of registering an {@link Effect}, not extending
 * a switch in {@link GameEngine}.
 */
public final class ItemActionEffects {

    /** Strategy: the effect produced when an {@link ItemAction} is applied. */
    public interface Effect {
        boolean apply(Hero hero, Item item);

        /** Whether observers should be notified after a successful apply. */
        default boolean notifyAfterApply() {
            return true;
        }
    }

    private static final Map<ItemAction, Effect> REGISTRY = new EnumMap<>(ItemAction.class);

    static {
        REGISTRY.put(ItemAction.DRINK, new DrinkEffect());
        REGISTRY.put(ItemAction.WEAR, new WearEffect());
        REGISTRY.put(ItemAction.EQUIP, new EquipEffect());
        REGISTRY.put(ItemAction.READ, new ReadEffect());
        REGISTRY.put(ItemAction.REMOVE, new RemoveEffect());
        REGISTRY.put(ItemAction.DISCARD, new DiscardEffect());
        REGISTRY.put(ItemAction.SEARCH, new SearchEffect());
        REGISTRY.put(ItemAction.BREAK, new BreakEffect());
        REGISTRY.put(ItemAction.OPEN, new OpenEffect());
        REGISTRY.put(ItemAction.EAT, new EatEffect());
        REGISTRY.put(ItemAction.CAST, new CastEffect());
    }

    private ItemActionEffects() {
    }

    /** Returns the effect bound to {@code action}, or {@code null} if none is registered. */
    public static Effect forAction(ItemAction action) {
        return action == null ? null : REGISTRY.get(action);
    }

    static final class DrinkEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            if (!(item instanceof Potion potion) || !hero.getInventory().remove(item)) {
                return false;
            }
            potion.drink(hero);
            return true;
        }
    }

    static final class WearEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            if (item instanceof Armor armor) {
                return hero.wearArmor(armor);
            }
            if (item instanceof Ring ring) {
                return hero.wearRing(ring);
            }
            return false;
        }
    }

    static final class EquipEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            if (item instanceof Weapon weapon) {
                return hero.equipWeapon(weapon);
            }
            if (item instanceof Armor armor) {
                return hero.wearArmor(armor);
            }
            return false;
        }
    }

    static final class ReadEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            return item instanceof Readable;
        }

        @Override
        public boolean notifyAfterApply() {
            return false;
        }
    }

    static final class RemoveEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            return hero.removeEquipment(item);
        }
    }

    static final class DiscardEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            hero.removeEquipment(item);
            return hero.getInventory().remove(item);
        }
    }

    static final class SearchEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            return item instanceof Container;
        }

        @Override
        public boolean notifyAfterApply() {
            return false;
        }
    }

    static final class BreakEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            if (!(item instanceof Container container) || !container.isBreakable()) {
                return false;
            }
            if (hero.getStr() < container.getBreakStrengthRequired()) {
                return false;
            }
            container.setLocked(false);
            return true;
        }
    }

    static final class OpenEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            return item instanceof Container container && !container.isLocked();
        }

        @Override
        public boolean notifyAfterApply() {
            return false;
        }
    }

    static final class EatEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            if (!(item instanceof Potion potion) || !hero.getInventory().remove(item)) {
                return false;
            }
            potion.drink(hero);
            return true;
        }
    }

    static final class CastEffect implements Effect {
        @Override
        public boolean apply(Hero hero, Item item) {
            return item instanceof Readable;
        }

        @Override
        public boolean notifyAfterApply() {
            return false;
        }
    }
}
