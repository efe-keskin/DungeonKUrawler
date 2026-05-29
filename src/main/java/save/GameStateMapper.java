package save;

import engine.GameEngine;
import engine.TargetItemMission;
import java.util.LinkedHashMap;
import java.util.Map;

import model.Armor;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Pet;
import model.Item;
import model.LevelStatus;
import model.Ring;
import model.TowerProgress;
import model.ValuableItem;
import model.Weapon;
import save.ItemDtoFactory.RestoreContext;
import save.SaveDtos.CellDto;
import save.SaveDtos.EntityDto;
import save.SaveDtos.GameStateDto;
import save.SaveDtos.HeroDto;
import save.SaveDtos.ItemDto;
import save.SaveDtos.LevelProgressDto;
import save.SaveDtos.MapDto;
import save.SaveDtos.SaveGameDto;
import save.SaveDtos.TowerProgressDto;

/**
 * Pure Fabrication: keeps save DTO concerns out of the model and view layers.
 */
public final class GameStateMapper {

    private final ItemDtoFactory itemFactory = new ItemDtoFactory();
    private final EntityDtoFactory entityFactory = new EntityDtoFactory();

    public GameStateDto toDto(GameEngine engine) throws SaveGameException {
        return toDto(engine, null);
    }

    public GameStateDto toDto(GameEngine engine, TowerProgress towerProgress) throws SaveGameException {
        if (engine == null) {
            throw new SaveGameException("No active game session to save.");
        }

        TargetItemMission mission = engine.getTargetMission();
        ValuableItem target = mission == null ? null : mission.getTarget();

        GameStateDto dto = new GameStateDto();
        dto.map = mapToDto(engine.getDungeonMap(), target);
        dto.hero = heroToDto(engine.getHero(), target);
        dto.missionStarted = mission != null && mission.isStarted();
        dto.missionWon = mission != null && mission.isWon();
        if (target != null) {
            dto.missionTargetName = target.getName();
            dto.missionTargetSprite = target.spriteResource();
        }
        dto.towerProgress = toDto(towerProgress);
        return dto;
    }

    /**
     * Maps tower progress to its save DTO. Returns {@code null} when there is
     * no progress to persist, leaving the field absent for legacy-style saves.
     */
    public TowerProgressDto toDto(TowerProgress progress) {
        if (progress == null) {
            return null;
        }
        TowerProgressDto dto = new TowerProgressDto();
        dto.highestUnlockedLevel = progress.highestUnlockedLevel();
        for (Map.Entry<Integer, LevelStatus> entry : progress.levelProgress().entrySet()) {
            LevelProgressDto levelDto = new LevelProgressDto();
            levelDto.levelNumber = entry.getKey();
            levelDto.status = entry.getValue().name();
            dto.levels.add(levelDto);
        }
        return dto;
    }

    /**
     * Rebuilds tower progress from a save DTO. Falls back to the default
     * (Level 1 unlocked) when the save predates tower mode or carries no level
     * data, so old saves load cleanly. Unknown status strings degrade to
     * {@link LevelStatus#LOCKED}.
     */
    public TowerProgress toTowerProgress(TowerProgressDto dto, int levelCount) {
        if (dto == null || dto.levels == null || dto.levels.isEmpty()) {
            return TowerProgress.defaultProgress(levelCount);
        }
        Map<Integer, LevelStatus> progress = new LinkedHashMap<>();
        Map<Integer, LevelStatus> savedStatuses = new LinkedHashMap<>();
        for (LevelProgressDto levelDto : dto.levels) {
            savedStatuses.put(levelDto.levelNumber, parseStatus(levelDto.status));
        }
        for (int level = 1; level <= levelCount; level++) {
            LevelStatus saved = savedStatuses.getOrDefault(level, LevelStatus.LOCKED);
            if (saved == LevelStatus.COMPLETED) {
                progress.put(level, LevelStatus.COMPLETED);
            } else if (level <= dto.highestUnlockedLevel) {
                progress.put(level, LevelStatus.UNLOCKED);
            } else {
                progress.put(level, saved == LevelStatus.HIDDEN ? LevelStatus.HIDDEN : LevelStatus.LOCKED);
            }
        }
        return TowerProgress.fromState(dto.highestUnlockedLevel, progress);
    }

    private static LevelStatus parseStatus(String raw) {
        if (raw == null) {
            return LevelStatus.LOCKED;
        }
        try {
            return LevelStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return LevelStatus.LOCKED;
        }
    }

    public GameEngine toEngine(SaveGameDto saveGame) throws SaveGameException {
        if (saveGame == null || saveGame.gameState == null) {
            throw new SaveGameException("Save file does not contain game state.");
        }
        GameStateDto state = saveGame.gameState;
        if (state.map == null || state.hero == null) {
            throw new SaveGameException("Save file is missing map or hero data.");
        }
        if (state.map.width <= 0 || state.map.height <= 0) {
            throw new SaveGameException("Save file contains an invalid map size.");
        }

        RestoreContext context = new RestoreContext();
        DungeonMap map = new DungeonMap(fallback(state.map.levelName, "Loaded Dungeon"),
                state.map.width, state.map.height);
        restoreMapCells(map, state.map, context);
        Hero hero = restoreHero(state.hero, context);
        ValuableItem target = context.missionTarget;
        if (target == null && state.missionTargetName != null && !state.missionTargetName.isBlank()) {
            target = new ValuableItem(state.missionTargetName, state.missionTargetSprite);
        }
        return new GameEngine(map, hero, target, state.missionStarted, state.missionWon);
    }

    private MapDto mapToDto(DungeonMap map, ValuableItem missionTarget) throws SaveGameException {
        if (map == null) {
            throw new SaveGameException("Cannot save a missing map.");
        }
        MapDto dto = new MapDto();
        dto.levelName = map.getLevelName();
        dto.width = map.getWidth();
        dto.height = map.getHeight();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                CellDto cellDto = new CellDto();
                cellDto.x = x;
                cellDto.y = y;
                cellDto.passable = cell.isPassable();
                for (Item item : cell.getItemsView()) {
                    ItemDto itemDto = itemFactory.toDto(item, missionTarget);
                    if (itemDto != null) {
                        cellDto.items.add(itemDto);
                    }
                }
                for (Entity entity : cell.getEntitiesView()) {
                    if (entity instanceof Hero) {
                        continue;
                    }
                    EntityDto entityDto = entityFactory.toDto(entity);
                    if (entityDto != null && entityDto.type != null) {
                        cellDto.entities.add(entityDto);
                    }
                }
                dto.cells.add(cellDto);
            }
        }
        return dto;
    }

    private HeroDto heroToDto(Hero hero, ValuableItem missionTarget) throws SaveGameException {
        if (hero == null) {
            throw new SaveGameException("Cannot save a missing hero.");
        }
        HeroDto dto = new HeroDto();
        dto.x = hero.getX();
        dto.y = hero.getY();
        dto.name = hero.getName();
        dto.hp = hero.getHp();
        dto.maxHp = hero.getMaxHp();
        dto.str = hero.getStr();
        dto.mana = hero.getMana();
        dto.maxMana = hero.getMaxMana();
        dto.baseDef = hero.getBaseDef();
        dto.energy = hero.getEnergy();
        dto.maxEnergy = hero.getMaxEnergy();
        dto.coinBalance = hero.getCoinBalance();

        int index = 0;
        for (Item item : hero.getInventory().getItems()) {
            if (item == hero.getEquippedArmor()) {
                dto.equippedArmorIndex = index;
            }
            if (item == hero.getEquippedWeapon()) {
                dto.equippedWeaponIndex = index;
            }
            if (item == hero.getEquippedRing()) {
                dto.equippedRingIndex = index;
            }
            dto.inventory.add(itemFactory.toDto(item, missionTarget));
            index++;
        }

        int fullIndex = 0;
        for (Item item : hero.getFullInventory().getItems()) {
            if (item == hero.getEquippedPet()) {
                dto.equippedPetIndex = fullIndex;
            }
            dto.fullInventory.add(itemFactory.toDto(item, missionTarget));
            fullIndex++;
        }
        return dto;
    }

    private void restoreMapCells(DungeonMap map, MapDto mapDto, RestoreContext context) {
        if (mapDto.cells == null) {
            return;
        }
        for (CellDto cellDto : mapDto.cells) {
            GridCell cell = map.getCell(cellDto.x, cellDto.y);
            if (cell == null) {
                continue;
            }
            cell.setPassable(cellDto.passable);
            cell.getItems().clear();
            cell.getEntities().clear();
            if (cellDto.items != null) {
                for (ItemDto itemDto : cellDto.items) {
                    Item item = itemFactory.fromDto(itemDto, context);
                    if (item != null) {
                        cell.getItems().add(item);
                    }
                }
            }
            if (cellDto.entities != null) {
                for (EntityDto entityDto : cellDto.entities) {
                    Entity entity = entityFactory.fromDto(entityDto);
                    if (entity != null) {
                        cell.getEntities().add(entity);
                    }
                }
            }
        }
    }

    private Hero restoreHero(HeroDto dto, RestoreContext context) {
        Hero hero = new Hero(dto.x, dto.y, fallback(dto.name, "Hero"),
                positive(dto.maxHp, positive(dto.hp, 17)),
                dto.str,
                positive(dto.maxMana, positive(dto.mana, 80)),
                dto.baseDef,
                positive(dto.maxEnergy, positive(dto.energy, 100)));
        hero.setHp(positive(dto.hp, hero.getMaxHp()));
        hero.setMana(dto.mana);
        hero.setEnergy(dto.energy);
        hero.setCoinBalance(dto.coinBalance);

        if (dto.inventory != null) {
            for (ItemDto itemDto : dto.inventory) {
                Item item = itemFactory.fromDto(itemDto, context);
                if (item != null) {
                    hero.getInventory().tryAdd(item);
                }
            }
        }

        if (dto.fullInventory != null) {
            for (ItemDto itemDto : dto.fullInventory) {
                Item item = itemFactory.fromDto(itemDto, context);
                if (item != null) {
                    hero.getFullInventory().add(item);
                }
            }
        }

        if (dto.equippedPetIndex >= 0 && dto.equippedPetIndex < hero.getFullInventory().getItems().size()
                && hero.getFullInventory().getItems().get(dto.equippedPetIndex) instanceof Pet pet) {
            hero.setEquippedPet(pet);
        }

        equip(hero, dto.equippedArmorIndex, Armor.class);
        equip(hero, dto.equippedWeaponIndex, Weapon.class);
        equip(hero, dto.equippedRingIndex, Ring.class);
        return hero;
    }

    private void equip(Hero hero, int index, Class<? extends Item> expectedType) {
        if (index < 0 || index >= hero.getInventory().getItems().size()) {
            return;
        }
        Item item = hero.getInventory().getItems().get(index);
        if (!expectedType.isInstance(item)) {
            return;
        }
        if (item instanceof Armor armor) {
            hero.wearArmor(armor);
        } else if (item instanceof Weapon weapon) {
            hero.equipWeapon(weapon);
        } else if (item instanceof Ring ring) {
            hero.wearRing(ring);
        }
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
