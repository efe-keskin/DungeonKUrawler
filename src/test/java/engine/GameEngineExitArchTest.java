package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import model.Arch;
import model.GridCell;
import model.Knight;

class GameEngineExitArchTest {

    @Test
    void openingExitPacifiesEnemiesWithoutBlockingHeroMovement() throws Exception {
        GameEngine engine = new GameEngine();
        engine.shutdown();
        try {
            GridCell enemyCell = engine.getDungeonMap().getCell(2, 1);
            enemyCell.getEntities().clear();
            enemyCell.getEntities().add(new Knight(2, 1, "Knight", 20, 100, 0, 5));

            spawnProjectile(engine, 4, 4, 1, 1, false);
            assertEquals(1, engine.getActiveProjectilesView().size());

            assertTrue(engine.openArch(new Arch()));
            assertTrue(engine.areEnemiesPacified());
            assertTrue(engine.canHeroAct());
            assertTrue(engine.getActiveProjectilesView().isEmpty());

            int hpAfterExitOpened = engine.getHero().getHp();
            invokeNoArg(engine, "updateKnightMeleeActions");
            assertEquals(hpAfterExitOpened, engine.getHero().getHp());

            spawnProjectile(engine, 4, 4, 1, 1, false);
            assertTrue(engine.getActiveProjectilesView().isEmpty());
            assertEquals("spawnEnemyProcedurally: exit is open", engine.spawnEnemyProcedurally());
        } finally {
            engine.shutdown();
        }
    }

    private static void spawnProjectile(GameEngine engine,
            int startX, int startY, int targetX, int targetY, boolean heroOwned) throws Exception {
        Method method = GameEngine.class.getDeclaredMethod("spawnProjectile",
                int.class, int.class, int.class, int.class, int.class, int.class, boolean.class);
        method.setAccessible(true);
        method.invoke(engine, startX, startY, targetX, targetY, 8, 4, heroOwned);
    }

    private static void invokeNoArg(GameEngine engine, String methodName) throws Exception {
        Method method = GameEngine.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(engine);
    }
}
