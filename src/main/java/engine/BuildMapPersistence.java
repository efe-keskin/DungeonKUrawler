package engine;

import static java.nio.charset.StandardCharsets.UTF_8;


import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import model.Armor;
import model.Book;
import model.Chest;
import model.Coin;
import model.Column;
import model.Container;
import model.Crate;
import model.DecorativeObject;
import model.DefeatedEnemyMarker;
import model.DungeonMap;
import model.EnergyPotion;
import model.Gargoyle;
import model.GridCell;
import model.Grill;
import model.HealPotion;
import model.Hole;
import model.Item;
import model.Key;
import model.KeyColor;
import model.ManaPotion;
import model.MissingBrick;
import model.Pedestal;
import model.Pool;
import model.Ring;
import model.SearchableObject;
import model.Torch;
import model.ValuableItem;
import model.Vase;
import model.WaterPipe;
import model.Weapon;
import model.WeaponCatalog;
import model.WeaponType;

/**
 * Memento-style JSON persistence boundary for build-mode maps.
 *
 * <p>Schema root:
 * {@code schema, version, levelName, width, height, cells[]}. Each cell stores
 * {@code x, y, passable, items[]}. Items store a {@code type} discriminator and
 * type-specific state such as container contents, locks, and searchable hidden
 * items.
 */
public final class BuildMapPersistence {

    private static final String SCHEMA = "DungeonKUrawler.DungeonMap";
    private static final int VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final BuildMapFactory mapFactory;

    public BuildMapPersistence(BuildToolCatalog catalog, BuildMapFactory mapFactory,
            BuildPlacementStrategy placementStrategy) {
        this.mapFactory = mapFactory;
    }

    public void save(DungeonMap map, Path path) throws IOException {
        if (map == null) {
            throw new IOException("No build map is available to save.");
        }
        if (path == null) {
            throw new IOException("No save file was selected.");
        }
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (Writer writer = Files.newBufferedWriter(path, UTF_8)) {
            GSON.toJson(toDto(map), writer);
        }
    }

    public DungeonMap load(Path path) throws IOException {
        if (path == null) {
            throw new IOException("No map file was selected.");
        }

        MapDto dto;
        try (Reader reader = Files.newBufferedReader(path, UTF_8)) {
            dto = GSON.fromJson(reader, MapDto.class);
        } catch (RuntimeException ex) {
            throw new IOException("Map JSON is not valid.", ex);
        }

        validate(dto);
        DungeonMap map = mapFactory.createEmptyMap(dto.levelName, dto.width, dto.height);
        map.setFogEnabled(dto.fogEnabled != null && dto.fogEnabled);
        for (CellDto cellDto : dto.cells) {
            GridCell cell = map.getCell(cellDto.x, cellDto.y);
            if (cell == null) {
                throw new IOException("Cell is out of bounds: " + cellDto.x + "," + cellDto.y);
            }
            cell.setPassable(cellDto.passable);
            cell.getItems().clear();
            cell.getEntities().clear();
            if (cellDto.items != null) {
                for (ItemDto itemDto : cellDto.items) {
                    cell.getItems().add(fromDto(itemDto));
                }
            }
        }
        return map;
    }

    private MapDto toDto(DungeonMap map) {
        MapDto dto = new MapDto();
        dto.schema = SCHEMA;
        dto.version = VERSION;
        dto.levelName = map.getLevelName();
        dto.width = map.getWidth();
        dto.height = map.getHeight();
        dto.fogEnabled = map.isFogEnabled();
        dto.cells = new ArrayList<>();

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                CellDto cellDto = new CellDto();
                cellDto.x = x;
                cellDto.y = y;
                cellDto.passable = cell.isPassable();
                cellDto.items = new ArrayList<>();
                for (Item item : cell.getItemsView()) {
                    cellDto.items.add(toDto(item));
                }
                dto.cells.add(cellDto);
            }
        }
        return dto;
    }

    private ItemDto toDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.name = item.getName();
        dto.spriteResource = item.spriteResource();

        if (item instanceof Chest chest) {
            dto.type = "chest";
            addContainerState(dto, chest);
        } else if (item instanceof Container container) {
            dto.type = "container";
            addContainerState(dto, container);
        } else if (item instanceof Crate crate) {
            dto.type = "crate";
            addSearchableState(dto, crate);
        } else if (item instanceof Pedestal pedestal) {
            dto.type = "pedestal";
            addSearchableState(dto, pedestal);
        } else if (item instanceof Pool pool) {
            dto.type = "pool";
            addSearchableState(dto, pool);
        } else if (item instanceof Gargoyle gargoyle) {
            dto.type = "gargoyle";
            addSearchableState(dto, gargoyle);
        } else if (item instanceof MissingBrick missingBrick) {
            dto.type = "missingBrick";
            addSearchableState(dto, missingBrick);
        } else if (item instanceof Hole hole) {
            dto.type = "hole";
            addSearchableState(dto, hole);
        } else if (item instanceof Grill grill) {
            dto.type = "grill";
            addSearchableState(dto, grill);
        } else if (item instanceof DecorativeObject decorativeObject) {
            dto.type = "decorativeObject";
            dto.blocking = decorativeObject.isBlocking();
        } else if (item instanceof Column column) {
            dto.type = "column";
            addSearchableState(dto, column);
        } else if (item instanceof WaterPipe waterPipe) {
            dto.type = "waterPipe";
            addSearchableState(dto, waterPipe);
        } else if (item instanceof SearchableObject searchableObject) {
            dto.type = "searchableObject";
            dto.blocking = searchableObject.isBlocking();
            addSearchableState(dto, searchableObject);
        } else if (item instanceof Vase) {
            dto.type = "vase";
        } else if (item instanceof HealPotion) {
            dto.type = "healPotion";
        } else if (item instanceof EnergyPotion) {
            dto.type = "energyPotion";
        } else if (item instanceof ManaPotion) {
            dto.type = "manaPotion";
        } else if (item instanceof Torch) {
            dto.type = "torch";
        } else if (item instanceof Key key) {
            dto.type = "key";
            dto.keyId = key.getKeyId();
            dto.keyColor = key.getColor().name();
            dto.singleUse = key.isSingleUse();
        } else if (item instanceof Weapon weapon) {
            dto.type = "weapon";
            WeaponType weaponType = weapon.getType();
            dto.weaponId = weaponType.id();
            dto.weaponCategory = weaponType.category();
            dto.weaponSpritePath = weaponType.spritePath();
            dto.baseAttack = weaponType.baseAttack();
            dto.ranged = weaponType.ranged();
        } else if (item instanceof Armor armor) {
            dto.type = "armor";
            dto.defModifier = armor.getDefModifier();
        } else if (item instanceof Ring ring) {
            dto.type = "ring";
            dto.defBonus = ring.getDefBonus();
        } else if (item instanceof ValuableItem) {
            dto.type = "valuable";
        } else if (item instanceof Coin coin) {
            dto.type = "coin";
            dto.value = coin.getValue();
        } else if (item instanceof Book book) {
            dto.type = "book";
            dto.text = book.read();
        } else if (item instanceof DefeatedEnemyMarker) {
            dto.type = "defeatedEnemy";
        } else {
            dto.type = "item";
        }
        return dto;
    }

    private void addContainerState(ItemDto dto, Container container) {
        dto.capacity = container.getCapacity();
        dto.locked = container.isLocked();
        dto.requiresKey = container.isRequiresKey();
        dto.requiredKeyId = container.getRequiredKeyId();
        dto.breakable = container.isBreakable();
        dto.breakStrengthRequired = container.getBreakStrengthRequired();
        dto.portable = container.isPortable();
        dto.contents = new ArrayList<>();
        for (Item content : container.getContents()) {
            dto.contents.add(toDto(content));
        }
    }

    private void addSearchableState(ItemDto dto, SearchableObject searchableObject) {
        Item hidden = searchableObject.getHiddenItem();
        dto.hiddenItem = hidden == null ? null : toDto(hidden);
    }

    private Item fromDto(ItemDto dto) throws IOException {
        if (dto == null || dto.type == null || dto.type.isBlank()) {
            throw new IOException("Map item is missing a type.");
        }

        return switch (dto.type) {
            case "chest" -> restoreContainer(
                    new Chest(name(dto, "Wooden Chest"), positive(dto.capacity, 16), dto.spriteResource), dto);
            case "container" -> restoreContainer(new Container(name(dto, "Container"),
                    bool(dto.locked), bool(dto.requiresKey), positive(dto.capacity, 8),
                    bool(dto.portable), dto.spriteResource), dto);
            case "crate" -> dto.spriteResource == null
                    ? new Crate(fromNullableDto(dto.hiddenItem))
                    : new Crate(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "pedestal" -> new Pedestal(fromNullableDto(dto.hiddenItem));
            case "pool" -> dto.spriteResource == null
                    ? new Pool(fromNullableDto(dto.hiddenItem))
                    : new Pool(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "gargoyle" -> dto.spriteResource == null
                    ? new Gargoyle(fromNullableDto(dto.hiddenItem))
                    : new Gargoyle(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "missingBrick" -> dto.spriteResource == null
                    ? new MissingBrick(fromNullableDto(dto.hiddenItem))
                    : new MissingBrick(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "hole" -> dto.spriteResource == null
                    ? new Hole(fromNullableDto(dto.hiddenItem))
                    : new Hole(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "grill" -> dto.spriteResource == null
                    ? new Grill(fromNullableDto(dto.hiddenItem))
                    : new Grill(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "decorativeObject" -> new DecorativeObject(name(dto, "Decorative Object"),
                    bool(dto.blocking), dto.spriteResource);
            case "searchableObject" -> new SearchableObject(name(dto, "Searchable Object"),
                    bool(dto.blocking), dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "column" -> dto.spriteResource == null
                    ? new Column()
                    : new Column(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "vase" -> new Vase();
            case "waterPipe" -> dto.spriteResource == null
                    ? new WaterPipe()
                    : new WaterPipe(dto.spriteResource, fromNullableDto(dto.hiddenItem));
            case "healPotion" -> new HealPotion();
            case "energyPotion" -> new EnergyPotion();
            case "manaPotion" -> new ManaPotion();
            case "torch" -> new Torch();
            case "key" -> new Key(valueOr(dto.keyId, "silver"), keyColor(dto.keyColor), bool(dto.singleUse));
            case "weapon" -> new Weapon(weaponType(dto));
            case "armor" -> new Armor(name(dto, "Armor"), intOr(dto.defModifier, 0));
            case "ring" -> new Ring(name(dto, "Ring"), intOr(dto.defBonus, 0), dto.spriteResource);
            case "valuable" -> new ValuableItem(name(dto, "Valuable"), dto.spriteResource);
            case "coin" -> new Coin(positive(dto.value, 1), dto.spriteResource);
            case "book" -> new Book(name(dto, "Book"), valueOr(dto.text, ""));
            case "defeatedEnemy" -> new DefeatedEnemyMarker();
            case "item" -> new ValuableItem(name(dto, "Item"), dto.spriteResource);
            default -> throw new IOException("Unsupported map item type: " + dto.type);
        };
    }

    private Item fromNullableDto(ItemDto dto) throws IOException {
        return dto == null ? null : fromDto(dto);
    }

    private <T extends Container> T restoreContainer(T container, ItemDto dto) throws IOException {
        container.setLocked(bool(dto.locked));
        container.setRequiresKey(bool(dto.requiresKey));
        container.setRequiredKeyId(dto.requiredKeyId);
        container.setBreakable(bool(dto.breakable));
        container.setBreakStrengthRequired(Math.max(0, intOr(dto.breakStrengthRequired, 0)));
        if (dto.contents != null) {
            for (ItemDto content : dto.contents) {
                if (!container.addItem(fromDto(content))) {
                    throw new IOException("Container is full while loading: " + container.getName());
                }
            }
        }
        return container;
    }

    private WeaponType weaponType(ItemDto dto) {
        WeaponType catalogType = dto.weaponId == null ? null : WeaponCatalog.get().byId(dto.weaponId);
        if (catalogType != null) {
            return catalogType;
        }
        return new WeaponType(
                valueOr(dto.weaponId, "custom"),
                name(dto, "Weapon"),
                valueOr(dto.weaponCategory, "tools"),
                dto.weaponSpritePath,
                intOr(dto.baseAttack, 2) == 0 ? 2 : intOr(dto.baseAttack, 2),
                bool(dto.ranged));
    }

    private void validate(MapDto dto) throws IOException {
        if (dto == null) {
            throw new IOException("Map JSON is empty.");
        }
        if (!SCHEMA.equals(dto.schema)) {
            throw new IOException("Unsupported map schema.");
        }
        // v1: pre-fog maps load as fog-disabled
        // v2: adds fogEnabled flag on the map root
        if (dto.version < 1 || dto.version > VERSION) {
            throw new IOException("Unsupported map version: " + dto.version);
        }
        if (dto.width <= 0 || dto.height <= 0) {
            throw new IOException("Map size is missing or invalid.");
        }
        if (dto.cells == null) {
            throw new IOException("Map cells are missing.");
        }
    }

    private static String name(ItemDto dto, String fallback) {
        return valueOr(dto.name, fallback);
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean bool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static int intOr(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static int positive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private static KeyColor keyColor(String value) {
        if (value != null) {
            try {
                return KeyColor.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                // Fall through to default.
            }
        }
        return KeyColor.SILVER;
    }

    private static final class MapDto {
        String schema;
        int version;
        String levelName;
        int width;
        int height;
        Boolean fogEnabled;
        List<CellDto> cells;
    }

    private static final class CellDto {
        int x;
        int y;
        boolean passable;
        List<ItemDto> items;
    }

    private static final class ItemDto {
        String type;
        String name;
        String spriteResource;
        Boolean blocking;

        Integer capacity;
        Boolean locked;
        Boolean requiresKey;
        String requiredKeyId;
        Boolean breakable;
        Integer breakStrengthRequired;
        Boolean portable;
        List<ItemDto> contents;

        ItemDto hiddenItem;

        String keyId;
        String keyColor;
        Boolean singleUse;

        String weaponId;
        String weaponCategory;
        String weaponSpritePath;
        Integer baseAttack;
        Boolean ranged;

        Integer defModifier;
        Integer defBonus;
        Integer value;
        String text;
    }
}
