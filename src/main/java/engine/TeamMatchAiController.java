package engine;

import java.util.ArrayList;
import java.util.List;

import model.AIState;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Item;
import model.Knight;
import model.Sorcerer;
import model.Team;
import model.Weapon;

/**
 * Strategy-style controller for Team Match AI. Standard play mode keeps its
 * hero-centric AI in {@link GameEngine}; Team Match uses this controller so
 * actors reason about teams and nearest opposing characters instead.
 */
final class TeamMatchAiController {

    private static final int SORCERER_SHOOT_RANGE = 6;

    private final CombatManager combatManager = new CombatManager();
    private final TeamMatchOutcomeEvaluator outcomeEvaluator = new TeamMatchOutcomeEvaluator();

    TeamMatchAiResult update(DungeonMap map, Hero hero) {
        if (map == null || hero == null) {
            return new TeamMatchAiResult(false, TeamMatchOutcome.ONGOING);
        }

        List<Entity> actors = aiActors(map);
        boolean changed = false;
        for (Entity actor : actors) {
            if (!isStillOnMap(map, actor)) {
                continue;
            }
            changed |= setAiState(actor, AIState.CHASING);
            if (actor instanceof Knight knight) {
                changed |= updateKnight(map, knight);
            } else if (actor instanceof Sorcerer sorcerer) {
                changed |= updateSorcerer(map, sorcerer);
            }
        }
        return new TeamMatchAiResult(changed, outcomeEvaluator.evaluate(map));
    }

    private List<Entity> aiActors(DungeonMap map) {
        List<Entity> actors = new ArrayList<>();
        for (Entity entity : teamCharacters(map)) {
            if (entity instanceof Hero) {
                continue;
            }
            if (entity instanceof Knight || entity instanceof Sorcerer) {
                actors.add(entity);
            }
        }
        return actors;
    }

    private boolean updateKnight(DungeonMap map, Knight knight) {
        // Weaponless knights should care about weapons before enemies. This is
        // why the nearest weapon path runs before the nearest opponent path.
        boolean changed = pickupWeaponOnCurrentCell(map, knight);
        if (!knight.hasWeapon()) {
            WeaponCell targetWeapon = nearestWeapon(map, knight);
            if (targetWeapon != null) {
                if (!sameCell(knight, targetWeapon.cell())) {
                    changed |= moveToward(map, knight, targetWeapon.cell());
                    changed |= pickupWeaponOnCurrentCell(map, knight);
                }
                return changed;
            }
        }

        // If no weapons are left, or this knight already has one, it commits to
        // combat. I did not add low-HP potion seeking in this version to keep
        // the Team Match AI simple and readable.
        Entity target = nearestOpponent(knight, teamCharacters(map));
        if (target == null) {
            return changed;
        }
        if (!isAdjacent(knight, target)) {
            boolean moved = moveToward(map, knight, target);
            return changed || moved;
        }
        CombatManager.AttackResult result = combatManager.teamKnightAttacks(knight, target);
        if (result == null) {
            return changed;
        }
        if (result.isDefenderDefeated()) {
            removeEntity(map, target);
        }
        return true;
    }

    private boolean updateSorcerer(DungeonMap map, Sorcerer sorcerer) {
        // Sorcerers do not need weapons or protective rings in this design.
        // They attack the nearest enemy with magic from the beginning.
        Entity target = nearestOpponent(sorcerer, teamCharacters(map));
        if (target == null) {
            return false;
        }
        if (canSorcererShoot(map, sorcerer, target)) {
            CombatManager.AttackResult result = combatManager.teamSorcererAttacks(sorcerer, target);
            if (result == null) {
                return false;
            }
            if (result.isDefenderDefeated()) {
                removeEntity(map, target);
            }
            return true;
        }
        return moveToward(map, sorcerer, target);
    }

    private List<Entity> teamCharacters(DungeonMap map) {
        List<Entity> characters = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Entity entity : cell.getEntitiesView()) {
                    if (entity.getTeam() != Team.NONE) {
                        characters.add(entity);
                    }
                }
            }
        }
        return characters;
    }

    private Entity nearestOpponent(Entity actor, List<Entity> targets) {
        Entity nearest = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Entity candidate : targets) {
            if (candidate == actor || candidate.getTeam() == actor.getTeam()) {
                continue;
            }
            int distance = manhattanDistance(actor, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private WeaponCell nearestWeapon(DungeonMap map, Entity actor) {
        WeaponCell nearest = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null || !cell.isWalkable()) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (!(item instanceof Weapon)) {
                        continue;
                    }
                    int distance = Math.abs(actor.getX() - x) + Math.abs(actor.getY() - y);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        nearest = new WeaponCell(cell);
                    }
                }
            }
        }
        return nearest;
    }

    private boolean pickupWeaponOnCurrentCell(DungeonMap map, Knight knight) {
        if (knight.hasWeapon()) {
            return false;
        }
        GridCell cell = map.getCell(knight.getX(), knight.getY());
        if (cell == null) {
            return false;
        }
        for (Item item : new ArrayList<>(cell.getItems())) {
            if (item instanceof Weapon weapon) {
                knight.setWeapon(weapon);
                cell.getItems().remove(weapon);
                return true;
            }
        }
        return false;
    }

    private boolean moveToward(DungeonMap map, Entity actor, Entity target) {
        return moveToward(map, actor, map.getCell(target.getX(), target.getY()));
    }

    private boolean moveToward(DungeonMap map, Entity actor, GridCell target) {
        if (target == null) {
            return false;
        }
        int dx = Integer.compare(target.getX(), actor.getX());
        int dy = Integer.compare(target.getY(), actor.getY());
        int absDx = Math.abs(target.getX() - actor.getX());
        int absDy = Math.abs(target.getY() - actor.getY());

        if (absDx >= absDy) {
            if (tryMove(map, actor, actor.getX() + dx, actor.getY())) {
                return true;
            }
            return tryMove(map, actor, actor.getX(), actor.getY() + dy);
        }
        if (tryMove(map, actor, actor.getX(), actor.getY() + dy)) {
            return true;
        }
        return tryMove(map, actor, actor.getX() + dx, actor.getY());
    }

    private boolean tryMove(DungeonMap map, Entity actor, int nx, int ny) {
        if (nx == actor.getX() && ny == actor.getY()) {
            return false;
        }
        GridCell from = map.getCell(actor.getX(), actor.getY());
        GridCell to = map.getCell(nx, ny);
        if (from == null || to == null || !to.isWalkable() || !to.getEntitiesView().isEmpty()) {
            return false;
        }
        from.getEntities().remove(actor);
        actor.setX(nx);
        actor.setY(ny);
        to.getEntities().add(actor);
        return true;
    }

    private boolean setAiState(Entity entity, AIState state) {
        if (entity instanceof Knight knight && knight.getAiState() != state) {
            knight.setAiState(state);
            return true;
        }
        if (entity instanceof Sorcerer sorcerer && sorcerer.getAiState() != state) {
            sorcerer.setAiState(state);
            return true;
        }
        return false;
    }

    private boolean canSorcererShoot(DungeonMap map, Sorcerer sorcerer, Entity target) {
        if (sorcerer.getMana() < CombatManager.sorcererProjectileManaCost()) {
            return false;
        }
        int sx = sorcerer.getX();
        int sy = sorcerer.getY();
        int tx = target.getX();
        int ty = target.getY();
        if (sx == tx && sy == ty) {
            return true;
        }
        if (!onStraightRay(sx, sy, tx, ty)) {
            return false;
        }
        return hasClearProjectilePath(map, sx, sy, tx, ty, SORCERER_SHOOT_RANGE);
    }

    private boolean hasClearProjectilePath(DungeonMap map, int sx, int sy, int tx, int ty, int maxRange) {
        int dx = Integer.signum(tx - sx);
        int dy = Integer.signum(ty - sy);
        int cx = sx + dx;
        int cy = sy + dy;
        int steps = 1;
        while (cx != tx || cy != ty) {
            GridCell cell = map.getCell(cx, cy);
            if (cell == null || !cell.isWalkable() || steps > maxRange) {
                return false;
            }
            cx += dx;
            cy += dy;
            steps++;
        }
        return steps <= maxRange;
    }

    private boolean onStraightRay(int sx, int sy, int tx, int ty) {
        return sx == tx || sy == ty || Math.abs(tx - sx) == Math.abs(ty - sy);
    }

    private boolean removeEntity(DungeonMap map, Entity entity) {
        GridCell current = map.getCell(entity.getX(), entity.getY());
        if (current != null && current.getEntities().remove(entity)) {
            return true;
        }
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (cell != null && cell.getEntities().remove(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isStillOnMap(DungeonMap map, Entity entity) {
        GridCell cell = map.getCell(entity.getX(), entity.getY());
        return cell != null && cell.getEntitiesView().contains(entity);
    }

    private boolean sameCell(Entity actor, GridCell cell) {
        return cell != null && actor.getX() == cell.getX() && actor.getY() == cell.getY();
    }

    private boolean isAdjacent(Entity a, Entity b) {
        return Math.abs(a.getX() - b.getX()) <= 1
                && Math.abs(a.getY() - b.getY()) <= 1;
    }

    private int manhattanDistance(Entity a, Entity b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    private record WeaponCell(GridCell cell) {
    }
}
