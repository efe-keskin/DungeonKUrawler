package save;

import model.Armor;
import model.Arch;
import model.Book;
import model.BreakableObject;
import model.Chest;
import model.Coin;
import model.Column;
import model.Container;
import model.Crate;
import model.DecorativeObject;
import model.DefeatedEnemyMarker;
import model.EnergyPotion;
import model.Gargoyle;
import model.Grill;
import model.HealPotion;
import model.Hole;
import model.Item;
import model.Key;
import model.KeyColor;
import model.DragonPet;
import model.ManaPotion;
import model.MissingBrick;
import model.Pedestal;
import model.PenguinPet;
import model.Pet;
import model.PetState;
import model.Pool;
import model.Readable;
import model.Ring;
import model.RingEffectType;
import model.Scroll;
import model.SearchableObject;
import model.ShadowCloneScroll;
import model.ValuableItem;
import model.Vase;
import model.WaterPipe;
import model.Weapon;
import model.WeaponCatalog;
import model.WeaponType;
import save.SaveDtos.ItemDto;

/**
 * Factory/Adapter between polymorphic items and JSON-friendly DTO records.
 */
final class ItemDtoFactory {

    private static final String ARMOR = "ARMOR";
    private static final String ARCH = "ARCH";
    private static final String BOOK = "BOOK";
    private static final String CHEST = "CHEST";
    private static final String COIN = "COIN";
    private static final String COLUMN = "COLUMN";
    private static final String CONTAINER = "CONTAINER";
    private static final String CRATE = "CRATE";
    private static final String DECORATIVE = "DECORATIVE";
    private static final String DEFEATED_ENEMY = "DEFEATED_ENEMY";
    private static final String ENERGY_POTION = "ENERGY_POTION";
    private static final String GARGOYLE = "GARGOYLE";
    private static final String GRILL = "GRILL";
    private static final String HEAL_POTION = "HEAL_POTION";
    private static final String HOLE = "HOLE";
    private static final String KEY = "KEY";
    private static final String MANA_POTION = "MANA_POTION";
    private static final String MISSING_BRICK = "MISSING_BRICK";
    private static final String PENGUIN_PET = "PENGUIN_PET";
    private static final String DRAGON_PET = "DRAGON_PET";
    private static final String PEDESTAL = "PEDESTAL";
    private static final String POOL = "POOL";
    private static final String RING = "RING";
    private static final String SCROLL = "SCROLL";
    private static final String SHADOW_CLONE_SCROLL = "SHADOW_CLONE_SCROLL";
    private static final String SEARCHABLE = "SEARCHABLE";
    private static final String VALUABLE = "VALUABLE";
    private static final String VASE = "VASE";
    private static final String WATER_PIPE = "WATER_PIPE";
    private static final String WEAPON = "WEAPON";

    ItemDto toDto(Item item, ValuableItem missionTarget) {
        if (item == null) {
            return null;
        }

        ItemDto dto = new ItemDto();
        dto.type = typeOf(item);
        dto.name = item.getName();
        dto.spriteResource = item.spriteResource();
        dto.blocking = item.isBlocking();
        dto.missionTarget = item == missionTarget;

        if (item instanceof Coin coin) {
            dto.value = coin.getValue();
        } else if (item instanceof Arch arch) {
            dto.locked = !arch.isOpen();
            dto.requiredKeyId = arch.getRequiredKeyId();
        } else if (item instanceof Key key) {
            dto.keyId = key.getKeyId();
            dto.keyColor = key.getColor().name();
            dto.singleUse = key.isSingleUse();
        } else if (item instanceof Ring ring) {
            dto.ringEffectType = ring.getEffectType().name();
            dto.ringBonus = ring.getBonus();
            dto.defBonus = ring.getDefBonus();
        } else if (item instanceof Armor armor) {
            dto.defModifier = armor.getDefModifier();
        } else if (item instanceof Weapon weapon) {
            WeaponType type = weapon.getType();
            dto.weaponId = type.id();
            dto.weaponCategory = type.category();
            dto.weaponBaseAttack = type.baseAttack();
            dto.weaponRanged = type.ranged();
        } else if (item instanceof Readable readable) {
            dto.bookText = readable.read();
        } else if (item instanceof Container container) {
            dto.capacity = container.getCapacity();
            dto.locked = container.isLocked();
            dto.requiresKey = container.isRequiresKey();
            dto.requiredKeyId = container.getRequiredKeyId();
            dto.breakable = container.isBreakable();
            dto.breakStrengthRequired = container.getBreakStrengthRequired();
            dto.portable = container.isPortable();
            for (Item child : container.getContents()) {
                dto.contents.add(toDto(child, missionTarget));
            }
        }

        if (item instanceof SearchableObject searchableObject) {
            dto.hiddenItem = toDto(searchableObject.getHiddenItem(), missionTarget);
            dto.searched = searchableObject.isSearched();
        }
        if (item instanceof BreakableObject breakableObject) {
            dto.hiddenItem = toDto(breakableObject.getHiddenItem(), missionTarget);
        }

        if (item instanceof Pet pet) {
            dto.petHp = pet.getHp();
            dto.petMaxHp = pet.getMaxHp();
            dto.petState = pet.getState().name();
        }

        return dto;
    }

    Item fromDto(ItemDto dto, RestoreContext context) {
        if (dto == null) {
            return null;
        }

        Item item = switch (fallback(dto.type, VALUABLE)) {
            case HEAL_POTION -> new HealPotion();
            case MANA_POTION -> new ManaPotion();
            case ENERGY_POTION -> new EnergyPotion();
            case COIN -> new Coin(positive(dto.value, 1), dto.spriteResource);
            case KEY -> new Key(fallback(dto.keyId, "key"), parseKeyColor(dto.keyColor), dto.singleUse);
            case RING -> new Ring(fallback(dto.name, "Protective Ring"),
                    parseRingEffectType(dto.ringEffectType),
                    ringBonus(dto),
                    dto.spriteResource);
            case ARMOR -> new Armor(fallback(dto.name, "Armor"), dto.defModifier);
            case ARCH -> restoreArch(dto);
            case WEAPON -> new Weapon(resolveWeaponType(dto));
            case BOOK -> new Book(fallback(dto.name, "Book"), fallback(dto.bookText, ""));
            case SHADOW_CLONE_SCROLL -> new ShadowCloneScroll(
                    fallback(dto.name, "Shadow Clone Scroll"), fallback(dto.bookText, ""));
            case SCROLL -> new Scroll(fallback(dto.name, "Scroll"), fallback(dto.bookText, ""));
            case CHEST -> restoreContainer(
                    new Chest(fallback(dto.name, "Chest"), positive(dto.capacity, 1), dto.spriteResource),
                    dto, context);
            case CONTAINER -> restoreContainer(new Container(fallback(dto.name, "Container"),
                    dto.locked, dto.requiresKey, positive(dto.capacity, 1), dto.portable, dto.spriteResource),
                    dto, context);
            case MISSING_BRICK -> new MissingBrick(fallback(dto.spriteResource, MissingBrick.SPRITE_1),
                    fromDto(dto.hiddenItem, context));
            case WATER_PIPE -> restoreBreakable(
                    new WaterPipe(fallback(dto.spriteResource, WaterPipe.LARGE_RING_SPRITE)), dto, context);
            case GARGOYLE -> new Gargoyle(fallback(dto.spriteResource, Gargoyle.RED_LEFT_SPRITE),
                    fromDto(dto.hiddenItem, context));
            case HOLE -> new Hole(fallback(dto.spriteResource, Hole.SPRITE), fromDto(dto.hiddenItem, context));
            case GRILL -> new Grill(fallback(dto.spriteResource, Grill.HORIZONTAL_SPRITE),
                    fromDto(dto.hiddenItem, context));
            case COLUMN -> restoreBreakable(
                    new Column(fallback(dto.spriteResource, Column.GRAY_SPRITE)), dto, context);
            case POOL -> new Pool(fallback(dto.spriteResource, Pool.CYAN_DRIP_SPRITE),
                    fromDto(dto.hiddenItem, context));
            case SEARCHABLE -> new SearchableObject(fallback(dto.name, "Searchable Location"),
                    dto.blocking, dto.spriteResource, fromDto(dto.hiddenItem, context));
            case CRATE -> new Crate(fallback(dto.spriteResource, Crate.WOOD_TALL_SPRITE),
                    fromDto(dto.hiddenItem, context));
            case DECORATIVE -> new DecorativeObject(fallback(dto.name, "Decorative Object"),
                    dto.blocking, dto.spriteResource);
            case VASE -> restoreBreakable(new Vase(Vase.BROKEN_SPRITE.equals(dto.spriteResource)), dto, context);
            case PEDESTAL -> new Pedestal(fromDto(dto.hiddenItem, context));
            case DEFEATED_ENEMY -> new DefeatedEnemyMarker();
            case PENGUIN_PET -> new PenguinPet();
            case DRAGON_PET -> new DragonPet();
            case VALUABLE -> new ValuableItem(fallback(dto.name, "Valuable Item"), dto.spriteResource);
            default -> new ValuableItem(fallback(dto.name, "Unknown Item"), dto.spriteResource);
        };

        if (dto.name != null && !dto.name.isBlank()) {
            item.setName(dto.name);
        }
        if (item instanceof Pet pet && dto.petMaxHp > 0) {
            pet.setHp(dto.petHp);
            pet.setState(parsePetState(dto.petState));
        }
        if (item instanceof SearchableObject searchableObject) {
            searchableObject.setSearched(dto.searched);
        }
        if (dto.missionTarget && item instanceof ValuableItem valuableItem && context != null) {
            context.missionTarget = valuableItem;
        }
        return item;
    }

    private static String typeOf(Item item) {
        if (item instanceof HealPotion) {
            return HEAL_POTION;
        }
        if (item instanceof ManaPotion) {
            return MANA_POTION;
        }
        if (item instanceof EnergyPotion) {
            return ENERGY_POTION;
        }
        if (item instanceof Coin) {
            return COIN;
        }
        if (item instanceof Key) {
            return KEY;
        }
        if (item instanceof Ring) {
            return RING;
        }
        if (item instanceof Armor) {
            return ARMOR;
        }
        if (item instanceof Arch) {
            return ARCH;
        }
        if (item instanceof Weapon) {
            return WEAPON;
        }
        if (item instanceof ShadowCloneScroll) {
            return SHADOW_CLONE_SCROLL;
        }
        if (item instanceof Scroll) {
            return SCROLL;
        }
        if (item instanceof Book) {
            return BOOK;
        }
        if (item instanceof Chest) {
            return CHEST;
        }
        if (item instanceof Container) {
            return CONTAINER;
        }
        if (item instanceof MissingBrick) {
            return MISSING_BRICK;
        }
        if (item instanceof WaterPipe) {
            return WATER_PIPE;
        }
        if (item instanceof Gargoyle) {
            return GARGOYLE;
        }
        if (item instanceof Hole) {
            return HOLE;
        }
        if (item instanceof Grill) {
            return GRILL;
        }
        if (item instanceof Column) {
            return COLUMN;
        }
        if (item instanceof Pool) {
            return POOL;
        }
        if (item instanceof Crate) {
            return CRATE;
        }
        if (item instanceof Vase) {
            return VASE;
        }
        if (item instanceof Pedestal) {
            return PEDESTAL;
        }
        if (item instanceof DecorativeObject) {
            return DECORATIVE;
        }
        if (item instanceof SearchableObject) {
            return SEARCHABLE;
        }
        if (item instanceof DefeatedEnemyMarker) {
            return DEFEATED_ENEMY;
        }
        if (item instanceof PenguinPet) {
            return PENGUIN_PET;
        }
        if (item instanceof DragonPet) {
            return DRAGON_PET;
        }
        if (item instanceof ValuableItem) {
            return VALUABLE;
        }
        return VALUABLE;
    }

    private Container restoreContainer(Container container, ItemDto dto, RestoreContext context) {
        container.setLocked(dto.locked);
        container.setRequiresKey(dto.requiresKey);
        container.setRequiredKeyId(dto.requiredKeyId);
        container.setBreakable(dto.breakable);
        container.setBreakStrengthRequired(dto.breakStrengthRequired);
        if (dto.contents != null) {
            for (ItemDto child : dto.contents) {
                Item item = fromDto(child, context);
                if (item != null) {
                    container.addItem(item);
                }
            }
        }
        return container;
    }

    private <T extends BreakableObject> T restoreBreakable(T breakableObject, ItemDto dto,
            RestoreContext context) {
        breakableObject.setHiddenItem(fromDto(dto.hiddenItem, context));
        return breakableObject;
    }

    private Arch restoreArch(ItemDto dto) {
        Arch arch = new Arch(fallback(dto.requiredKeyId, Arch.DEFAULT_REQUIRED_KEY_ID));
        if (!dto.locked) {
            arch.open();
        }
        return arch;
    }

    private static WeaponType resolveWeaponType(ItemDto dto) {
        WeaponType catalogType = dto.weaponId == null ? null : WeaponCatalog.get().byId(dto.weaponId);
        if (catalogType != null) {
            return catalogType;
        }
        return new WeaponType(
                fallback(dto.weaponId, "CUSTOM"),
                fallback(dto.name, "Weapon"),
                fallback(dto.weaponCategory, "tools"),
                dto.spriteResource,
                positive(dto.weaponBaseAttack, 2),
                dto.weaponRanged);
    }

    private static KeyColor parseKeyColor(String value) {
        if (value == null || value.isBlank()) {
            return KeyColor.SILVER;
        }
        try {
            return KeyColor.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return KeyColor.SILVER;
        }
    }

    private static PetState parsePetState(String value) {
        if (value == null || value.isBlank()) {
            return PetState.UNEQUIPPED;
        }
        try {
            return PetState.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return PetState.UNEQUIPPED;
        }
    }

    private static RingEffectType parseRingEffectType(String value) {
        if (value == null || value.isBlank()) {
            return RingEffectType.DEFENSE;
        }
        try {
            return RingEffectType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return RingEffectType.DEFENSE;
        }
    }

    private static int ringBonus(ItemDto dto) {
        if (dto.ringBonus != 0) {
            return dto.ringBonus;
        }
        return dto.defBonus;
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    static final class RestoreContext {
        ValuableItem missionTarget;
    }
}
