package save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import engine.GameEngine;
import model.AIState;
import model.Armor;
import model.GridCell;
import model.Hero;
import model.Knight;
import model.Ring;
import model.TowerProgress;
import model.Weapon;
import model.WeaponType;
import save.SaveDtos.SaveDescriptor;

class SaveGameServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRestoresPlayableState() throws Exception {
        SaveGameService service = new SaveGameService(
                new SaveFileRepository(tempDir, new GsonSaveSerializer()),
                new GameStateMapper());
        GameEngine engine = new GameEngine();
        GameEngine loaded = null;
        try {
            arrangeDistinctState(engine);

            SaveDescriptor descriptor = service.saveGame(engine, "slot one");
            assertTrue(Files.exists(descriptor.getPath()));

            List<SaveDescriptor> saves = service.listSaves();
            assertEquals(1, saves.size());

            loaded = service.loadGame(saves.get(0));

            Hero hero = loaded.getHero();
            assertEquals(2, hero.getX());
            assertEquals(1, hero.getY());
            assertEquals(9, hero.getHp());
            assertEquals(40, hero.getMana());
            assertEquals(130, hero.getEnergy());
            assertEquals(25, hero.getCoinBalance());
            assertEquals(4, hero.getBaseDef());
            assertEquals(9, hero.getDef());
            assertInstanceOf(Armor.class, hero.getEquippedArmor());
            assertInstanceOf(Weapon.class, hero.getEquippedWeapon());
            assertInstanceOf(Ring.class, hero.getEquippedRing());

            GridCell enemyCell = loaded.getDungeonMap().getCell(2, 2);
            Knight knight = assertInstanceOf(Knight.class, enemyCell.getEntitiesView().get(0));
            assertEquals(7, knight.getHp());
            assertEquals(AIState.CHASING, knight.getAiState());
            assertTrue(loaded.getTargetMission().isStarted());
        } finally {
            engine.shutdown();
            if (loaded != null) {
                loaded.shutdown();
            }
        }
    }

    @Test
    void saveLimitAndDeleteAreEnforced() throws Exception {
        SaveGameService service = new SaveGameService(
                new SaveFileRepository(tempDir, new GsonSaveSerializer()),
                new GameStateMapper());
        GameEngine engine = new GameEngine();
        try {
            for (int i = 1; i <= SaveGameService.MAX_SAVE_FILES; i++) {
                service.saveGame(engine, "slot " + i);
            }

            assertEquals(SaveGameService.MAX_SAVE_FILES, service.listSaves().size());
            assertThrows(SaveLimitExceededException.class,
                    () -> service.saveGame(engine, "slot 11"));

            SaveDescriptor deleteTarget = service.listSaves().get(0);
            service.deleteSave(deleteTarget);

            assertFalse(Files.exists(deleteTarget.getPath()));
            assertEquals(SaveGameService.MAX_SAVE_FILES - 1, service.listSaves().size());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void saveDescriptorDisplaysShortDate() {
        SaveDescriptor descriptor = new SaveDescriptor("alpha",
                "2026-05-28T17:21:56.422193+03:00",
                tempDir.resolve("alpha.json"));

        assertEquals("alpha (2026-05-28 17:21)", descriptor.getDisplayLabel());
    }

    @Test
    void scenarioSaveRoundTripsProgressAndLevelSaves() throws Exception {
        SaveGameService service = new SaveGameService(
                new SaveFileRepository(tempDir, new GsonSaveSerializer()),
                new GameStateMapper());
        GameEngine custom = new GameEngine();
        GameEngine floor = new GameEngine();
        GameEngine restoredFloor = null;
        try {
            floor.configureTowerLevel(3, false);
            floor.updateHeroPosition(2, 1);
            TowerProgress progress = TowerProgress.defaultProgress(10);
            progress.completeLevel(1);
            progress.completeLevel(2);

            SaveDescriptor customSave = service.saveCustomGame(custom, "custom map run");
            assertEquals(SaveGameType.CUSTOM_GAME, customSave.getSaveType());

            // A resumable per-level save captured from the active floor.
            SaveDtos.LevelSaveDto levelSave = service.captureLevel(floor);
            assertEquals(3, levelSave.levelNumber);

            SaveDescriptor scenarioSave = service.saveScenario(null, "main run",
                    floor.getHero(), progress, List.of(levelSave));

            assertEquals(SaveGameType.SCENARIO, scenarioSave.getSaveType());
            assertEquals(1, service.listSaves(SaveGameType.CUSTOM_GAME).size());
            assertEquals(1, service.listSaves(SaveGameType.SCENARIO).size());

            LoadedScenario loaded = service.loadScenario(scenarioSave);
            assertEquals(3, loaded.towerProgress().highestUnlockedLevel());
            assertEquals(1, loaded.levelSaves().size());

            restoredFloor = service.restoreLevel(loaded.levelSaves().get(0));
            assertTrue(restoredFloor.isTowerLevel());
            assertEquals(3, restoredFloor.getTowerLevelNumber());
            assertEquals(2, restoredFloor.getHero().getX());
            assertEquals(1, restoredFloor.getHero().getY());
        } finally {
            custom.shutdown();
            floor.shutdown();
            if (restoredFloor != null) {
                restoredFloor.shutdown();
            }
        }
    }

    @Test
    void scenarioUpdateReusesSameFileWithoutConsumingSlot() throws Exception {
        SaveGameService service = new SaveGameService(
                new SaveFileRepository(tempDir, new GsonSaveSerializer()),
                new GameStateMapper());
        GameEngine floor = new GameEngine();
        try {
            floor.configureTowerLevel(2, false);
            TowerProgress progress = TowerProgress.defaultProgress(10);

            SaveDescriptor first = service.saveScenario(null, "run", floor.getHero(), progress, List.of());
            int slotsAfterCreate = service.listSaves().size();

            SaveDtos.LevelSaveDto levelSave = service.captureLevel(floor);
            SaveDescriptor second = service.saveScenario(first, "run", floor.getHero(), progress,
                    List.of(levelSave));

            assertEquals(slotsAfterCreate, service.listSaves().size());
            assertEquals(first.getPath(), second.getPath());
            assertEquals(1, service.loadScenario(second).levelSaves().size());
        } finally {
            floor.shutdown();
        }
    }

    private static void arrangeDistinctState(GameEngine engine) {
        engine.updateHeroPosition(2, 1);
        Hero hero = engine.getHero();
        hero.setHp(9);
        hero.setMana(40);
        hero.setEnergy(130);
        hero.setDef(4);
        hero.setCoinBalance(25);

        Armor armor = new Armor("Test Armor", 3);
        Weapon weapon = new Weapon(new WeaponType("TEST_BLADE", "Test Blade",
                "swords", "/weapons/test.png", 5, false));
        Ring ring = new Ring("Test Ring", 2);
        hero.getInventory().tryAdd(armor);
        hero.getInventory().tryAdd(weapon);
        hero.getInventory().tryAdd(ring);
        hero.wearArmor(armor);
        hero.equipWeapon(weapon);
        hero.wearRing(ring);

        Knight knight = new Knight(2, 2, "Saved Knight", 20, 8, 4, 5);
        knight.setHp(7);
        knight.setAiState(AIState.CHASING);
        engine.getDungeonMap().getCell(2, 2).getEntities().add(knight);
    }
}
