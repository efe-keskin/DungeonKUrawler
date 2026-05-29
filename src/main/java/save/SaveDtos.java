package save;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain DTO graph used as the custom JSON save format.
 */
public final class SaveDtos {

    private SaveDtos() {
    }

    public static final class SaveGameDto {
        public String saveName;
        public String savedAt;
        public GameStateDto gameState;
    }

    public static final class GameStateDto {
        public MapDto map;
        public HeroDto hero;
        public boolean missionStarted;
        public boolean missionWon;
        public String missionTargetName;
        public String missionTargetSprite;
        /** Null for saves created before tower mode; restored to a default in that case. */
        public TowerProgressDto towerProgress;
    }

    public static final class TowerProgressDto {
        public int highestUnlockedLevel;
        public List<LevelProgressDto> levels = new ArrayList<>();
    }

    public static final class LevelProgressDto {
        public int levelNumber;
        /** {@code LevelStatus} name kept as a plain string for Gson stability. */
        public String status;
    }

    public static final class MapDto {
        public String levelName;
        public int width;
        public int height;
        public List<CellDto> cells = new ArrayList<>();
    }

    public static final class CellDto {
        public int x;
        public int y;
        public boolean passable;
        public List<ItemDto> items = new ArrayList<>();
        public List<EntityDto> entities = new ArrayList<>();
    }

    public static final class HeroDto {
        public int x;
        public int y;
        public String name;
        public int hp;
        public int maxHp;
        public int str;
        public int mana;
        public int maxMana;
        public int baseDef;
        public int energy;
        public int maxEnergy;
        public int coinBalance;
        public int equippedArmorIndex = -1;
        public int equippedWeaponIndex = -1;
        public int equippedRingIndex = -1;
        public List<ItemDto> inventory = new ArrayList<>();
        /** Run-wide persistent items (valuables + shop purchases); gold rides in coinBalance. */
        public List<ItemDto> fullInventory = new ArrayList<>();
        /** Index into {@link #fullInventory} of the equipped pet, or -1 for none. */
        public int equippedPetIndex = -1;
    }

    public static final class ItemDto {
        public String type;
        public String name;
        public String spriteResource;
        public boolean missionTarget;

        public int value;
        public String keyId;
        public String keyColor;
        public boolean singleUse;
        public int defBonus;
        public int defModifier;
        public String weaponId;
        public String weaponCategory;
        public int weaponBaseAttack;
        public boolean weaponRanged;
        public String bookText;

        public int capacity;
        public boolean locked;
        public boolean requiresKey;
        public String requiredKeyId;
        public boolean breakable;
        public int breakStrengthRequired;
        public boolean portable;
        public List<ItemDto> contents = new ArrayList<>();

        public boolean blocking;
        public ItemDto hiddenItem;

        public int petHp;
        public int petMaxHp;
        public String petState;
    }

    public static final class EntityDto {
        public String type;
        public int x;
        public int y;
        public String name;
        public int hp;
        public int maxHp;
        public int str;
        public int def;
        public int visionRange;
        public int mana;
        public boolean hasMagicRing;
        public boolean panicTeleportUsed;
        public String aiState;
    }

    public static final class SaveDescriptor {
        private static final DateTimeFormatter SHORT_DATE =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final String saveName;
        private final String savedAt;
        private final Path path;

        public SaveDescriptor(String saveName, String savedAt, Path path) {
            this.saveName = saveName;
            this.savedAt = savedAt;
            this.path = path;
        }

        public String getSaveName() {
            return saveName;
        }

        public String getSavedAt() {
            return savedAt;
        }

        public Path getPath() {
            return path;
        }

        public String getShortSavedAt() {
            if (savedAt == null || savedAt.isBlank()) {
                return "unknown";
            }
            try {
                return OffsetDateTime.parse(savedAt).format(SHORT_DATE);
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDateTime.parse(savedAt).format(SHORT_DATE);
                } catch (DateTimeParseException ignoredAgain) {
                    return savedAt;
                }
            }
        }

        public String getDisplayLabel() {
            return saveName + " (" + getShortSavedAt() + ")";
        }

        @Override
        public String toString() {
            return getDisplayLabel();
        }
    }
}
