package engine;

import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.BossEnemy;
import model.Knight;
import model.Sorcerer;
import model.Weapon;

/**
 * GRASP Controller for player-initiated combat in play mode.
 */
public class CombatController {

    private static final int RANGED_AUTO_AIM_TILES = 2;

    private final GameEngine engine;
    private final CombatManager combatManager;

    public CombatController(GameEngine engine) {
        this.engine = engine;
        this.combatManager = new CombatManager();
    }

    /**
     * Attacks the first enemy at {@code (x, y)} when that tile is within the hero's
     * 3x3 interaction range.
     *
     * @return attack result when an enemy was hit; {@code null} when no valid target exists.
     */
    public CombatManager.AttackResult attackAt(int x, int y) {
        if (engine.isHeroAttackOnCooldown()) {
            return null;
        }

        Hero hero = engine.getHero();
        Weapon weapon = hero.getEquippedWeapon();
        if (weapon != null && weapon.isRanged()) {
            return engine.launchHeroRangedAttackAt(x, y);
        }

        DungeonMap map = engine.getDungeonMap();
        if (!map.isHeroAdjacent(hero, x, y)) {
            return null;
        }

        GridCell cell = map.getCell(x, y);
        if (cell == null) {
            return null;
        }

        Entity target = firstEnemy(cell);
        if (target == null) {
            return null;
        }

        CombatManager.AttackResult result;
        if (target instanceof Knight knight) {
            result = combatManager.heroAttacksKnight(hero, knight);
        } else if (target instanceof Sorcerer sorcerer) {
            result = combatManager.heroAttacksSorcerer(hero, sorcerer);
        } else if (target instanceof BossEnemy boss) {
            result = combatManager.heroAttacksBoss(hero, boss);
        } else {
            return null;
        }

        if (result.isDefenderDefeated()) {
            cell.getEntities().remove(target);
        }
        engine.recordHeroAttackPacing();
        engine.fireHeroAttack(result);
        if (result.isDefenderDefeated()) {
            engine.fireEnemyDefeated(target);
        }
        engine.notifyGameStateChanged();
        return result;
    }

    /**
     * Attacks the first enemy found in the hero's 3x3 interaction range,
     * scanning adjacent tiles before the hero's own tile. Used by the
     * keyboard hit shortcut where no specific target tile is supplied.
     *
     * @return the attack result and the tile that was hit, or {@code null}
     *         when no enemy is within reach.
     */
    public TargetedAttack attackNearestEnemy() {
        Hero hero = engine.getHero();
        Weapon weapon = hero.getEquippedWeapon();
        if (weapon != null && weapon.isRanged()) {
            return attackNearestEnemyInRangedRange(Integer.MAX_VALUE);
        }

        DungeonMap map = engine.getDungeonMap();
        int hx = hero.getX();
        int hy = hero.getY();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int tx = hx + dx;
                int ty = hy + dy;
                GridCell cell = map.getCell(tx, ty);
                if (cell == null || firstEnemy(cell) == null) {
                    continue;
                }
                CombatManager.AttackResult result = attackAt(tx, ty);
                if (result != null) {
                    return new TargetedAttack(result, tx, ty);
                }
            }
        }
        return null;
    }

    /**
     * Auto-aim ranged attack for the {@code P} shortcut: picks the nearest shootable
     * enemy within {@link #RANGED_AUTO_AIM_TILES} tiles (Chebyshev).
     */
    public TargetedAttack autoAimRangedAttack() {
        Hero hero = engine.getHero();
        Weapon weapon = hero.getEquippedWeapon();
        if (weapon == null || !weapon.isRanged()) {
            return null;
        }
        return attackNearestEnemyInRangedRange(RANGED_AUTO_AIM_TILES);
    }

    private TargetedAttack attackNearestEnemyInRangedRange(int maxChebyshevDistance) {
        Hero hero = engine.getHero();
        DungeonMap map = engine.getDungeonMap();
        int hx = hero.getX();
        int hy = hero.getY();
        int bestChebyshev = Integer.MAX_VALUE;
        int bestX = -1;
        int bestY = -1;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (!engine.canHeroShootAt(x, y)) {
                    continue;
                }
                int dist = Math.max(Math.abs(x - hx), Math.abs(y - hy));
                if (dist > maxChebyshevDistance) {
                    continue;
                }
                if (dist < bestChebyshev) {
                    bestChebyshev = dist;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        if (bestX < 0) {
            return null;
        }
        CombatManager.AttackResult result = engine.launchHeroRangedAttackAt(bestX, bestY);
        if (result == null) {
            return null;
        }
        return new TargetedAttack(result, bestX, bestY);
    }

    /** Pairs an attack result with the tile that was hit. */
    public record TargetedAttack(CombatManager.AttackResult result, int x, int y) {
    }

    private Entity firstEnemy(GridCell cell) {
        for (Entity entity : cell.getEntities()) {
            if (entity instanceof Knight || entity instanceof Sorcerer || entity instanceof BossEnemy) {
                return entity;
            }
        }
        return null;
    }

}
