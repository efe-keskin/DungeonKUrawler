package engine;

import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Knight;
import model.Sorcerer;

/**
 * GRASP Controller for player-initiated combat in play mode.
 */
public class CombatController {

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
        Hero hero = engine.getHero();
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
            if (!result.isDefenderDefeated()) {
                maybePanicTeleport(sorcerer);
            }
        } else {
            return null;
        }

        if (result.isDefenderDefeated()) {
            cell.getEntities().remove(target);
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

    /** Pairs an attack result with the tile that was hit. */
    public record TargetedAttack(CombatManager.AttackResult result, int x, int y) {
    }

    private Entity firstEnemy(GridCell cell) {
        for (Entity entity : cell.getEntities()) {
            if (entity instanceof Knight || entity instanceof Sorcerer) {
                return entity;
            }
        }
        return null;
    }

    /**
     * One-shot self-preservation: the first time a Sorcerer's HP drops to
     * half or below from a hero attack, it teleports to a random empty
     * cell. After that the flag stays set and this method becomes a no-op
     * for that sorcerer for the rest of its life.
     *
     * <p>Lives in the controller (not CombatManager) because teleporting
     * requires the map, and CombatManager is intentionally a stateless
     * formula service. Coexists with the 7-second passive teleport timer
     * in GameEngine — both can fire on the same sorcerer in a session.
     */
    private void maybePanicTeleport(Sorcerer sorcerer) {
        if (sorcerer.isPanicTeleportUsed()) {
            return;
        }
        if (sorcerer.getHp() * 2 > sorcerer.getMaxHp()) {
            // Still above half HP — no panic yet. We compare 2*hp vs maxHp
            // rather than using doubles to avoid floating-point edge cases
            // on the boundary (e.g. 5/10 must trigger).
            return;
        }
        boolean moved = engine.requestSorcererTeleport(sorcerer);
        if (moved) {
            sorcerer.setPanicTeleportUsed(true);
            System.out.printf("[COMBAT] Wounded sorcerer (HP %d/%d) panic-teleported.%n",
                    sorcerer.getHp(), sorcerer.getMaxHp());
            // TODO(ui-team): brief visual cue at source + destination
            // (smoke puff or fade animation). For now, console log only.
        }
        // If moved == false (no empty cell available), the flag stays
        // unset so a future hit can retry. That's intentional.
    }
}
