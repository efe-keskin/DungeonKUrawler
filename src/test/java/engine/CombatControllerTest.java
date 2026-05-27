package engine;

import model.GridCell;
import model.Knight;
import model.Sorcerer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatControllerTest {

    private GameEngine engine;
    private CombatController combatController;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        engine.shutdown();
        combatController = new CombatController(engine);
    }

    @Test
    void attackAtAdjacentEnemyDamagesTarget() {
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        targetCell.getEntities().clear();
        Knight knight = new Knight(2, 1, "Knight", 20, 8, 4, 5);
        targetCell.getEntities().add(knight);

        CombatManager.AttackResult result = combatController.attackAt(2, 1);

        assertNotNull(result);
        assertTrue(knight.getHp() < 20);
        assertFalse(result.isDefenderDefeated());
        assertTrue(targetCell.getEntities().contains(knight));
    }

    @Test
    void attackAtRemovesDefeatedEnemy() {
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        targetCell.getEntities().clear();
        Knight knight = new Knight(2, 1, "Knight", 1, 0, 0, 5);
        targetCell.getEntities().add(knight);

        CombatManager.AttackResult result = combatController.attackAt(2, 1);

        assertNotNull(result);
        assertTrue(result.isDefenderDefeated());
        assertFalse(targetCell.getEntities().contains(knight));
    }

    @Test
    void attackAtNonAdjacentTileReturnsNull() {
        GridCell targetCell = engine.getDungeonMap().getCell(5, 5);
        targetCell.getEntities().clear();
        targetCell.getEntities().add(new Knight(5, 5, "Knight", 20, 8, 4, 5));

        assertNull(combatController.attackAt(5, 5));
    }

    @Test
    void sorcererAboveHalfHpDoesNotPanicTeleport() {
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        targetCell.getEntities().clear();
        Sorcerer sorcerer = new Sorcerer(2, 1, "Sorcerer", 10, 30, 0, false);
        targetCell.getEntities().add(sorcerer);

        int startX = sorcerer.getX();
        int startY = sorcerer.getY();

        CombatManager.AttackResult result = combatController.attackAt(startX, startY);

        assertNotNull(result);
        assertEquals(startX, sorcerer.getX());
        assertEquals(startY, sorcerer.getY());
        assertFalse(sorcerer.isPanicTeleportUsed());
    }

    @Test
    void sorcererCrossingHalfHpPanicTeleports() {
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        targetCell.getEntities().clear();
        Sorcerer sorcerer = new Sorcerer(2, 1, "Sorcerer", 10, 30, 0, false);
        sorcerer.setHp(6);
        targetCell.getEntities().add(sorcerer);

        int startX = sorcerer.getX();
        int startY = sorcerer.getY();

        CombatManager.AttackResult result = combatController.attackAt(startX, startY);

        assertNotNull(result);
        assertFalse(sorcerer.getX() == startX && sorcerer.getY() == startY);
        assertFalse(targetCell.getEntities().contains(sorcerer));
        assertTrue(sorcerer.isPanicTeleportUsed());
    }

    @Test
    void sorcererBelowHalfHpDoesNotTeleportAgain() {
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        targetCell.getEntities().clear();
        Sorcerer sorcerer = new Sorcerer(2, 1, "Sorcerer", 10, 30, 0, false);
        sorcerer.setHp(4);
        sorcerer.setPanicTeleportUsed(true);
        targetCell.getEntities().add(sorcerer);

        int startX = sorcerer.getX();
        int startY = sorcerer.getY();

        CombatManager.AttackResult result = combatController.attackAt(startX, startY);

        assertNotNull(result);
        assertEquals(startX, sorcerer.getX());
        assertEquals(startY, sorcerer.getY());
        assertTrue(targetCell.getEntities().contains(sorcerer));
        assertTrue(sorcerer.isPanicTeleportUsed());
    }
}
