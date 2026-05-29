package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import model.DungeonMap;
import model.GridCell;
import model.Weapon;
import model.WeaponCatalog;
import model.WeaponType;

/**
 * GRASP Pure Fabrication for Team Match weapon seeding. It uses the existing
 * {@link WeaponCatalog} as the source of weapon sprites/names, then creates
 * Team Match-specific weapon types with distinct ATK values.
 */
final class TeamMatchWeaponSeeder {

    static final int WEAPON_COUNT = 6;

    // Six different ATK values satisfy the requirement and make weapon pickup
    // matter without adding random stat imbalance.
    private static final int[] TEAM_MATCH_ATK_VALUES = { 2, 3, 4, 5, 6, 7 };
    private static final List<String> PREFERRED_CATEGORIES = List.of(
            "daggers", "swords", "axes", "maces", "polearms", "bows");

    private final WeaponCatalog weaponCatalog;
    private final Random random;

    TeamMatchWeaponSeeder() {
        this(WeaponCatalog.get(), ThreadLocalRandom.current());
    }

    TeamMatchWeaponSeeder(WeaponCatalog weaponCatalog, Random random) {
        this.weaponCatalog = weaponCatalog;
        this.random = random;
    }

    void seedWeapons(DungeonMap map) {
        if (map == null) {
            throw new IllegalArgumentException(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE);
        }

        // Design decision: Team Match seeds only weapons for now. I did not add
        // armor, rings, or health potions here so the first version stays focused
        // on the required weapon race and team combat loop.
        List<GridCell> candidates = shuffledAvailableCells(map);
        if (candidates.size() < WEAPON_COUNT) {
            throw new IllegalArgumentException(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE);
        }

        List<Weapon> weapons = createTeamMatchWeapons();
        for (int i = 0; i < WEAPON_COUNT; i++) {
            candidates.get(i).getItems().add(weapons.get(i));
        }
    }

    private List<Weapon> createTeamMatchWeapons() {
        List<WeaponType> sources = chooseCatalogSources();
        if (sources.size() < WEAPON_COUNT) {
            throw new IllegalArgumentException(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE);
        }

        List<Weapon> weapons = new ArrayList<>();
        for (int i = 0; i < WEAPON_COUNT; i++) {
            WeaponType source = sources.get(i);
            int atk = TEAM_MATCH_ATK_VALUES[i];
            WeaponType teamMatchType = new WeaponType(
                    "TM-" + atk + "-" + source.id(),
                    source.displayName(),
                    source.category(),
                    source.spritePath(),
                    atk,
                    source.ranged());
            weapons.add(new Weapon(teamMatchType));
        }
        return weapons;
    }

    private List<WeaponType> chooseCatalogSources() {
        List<WeaponType> chosen = new ArrayList<>();
        Set<String> chosenIds = new HashSet<>();
        for (String category : PREFERRED_CATEGORIES) {
            WeaponType type = weaponCatalog.randomIn(category, random);
            if (type != null && chosenIds.add(type.id())) {
                chosen.add(type);
            }
        }

        if (chosen.size() < WEAPON_COUNT) {
            List<WeaponType> fallback = new ArrayList<>(weaponCatalog.all());
            Collections.shuffle(fallback, random);
            for (WeaponType type : fallback) {
                if (chosenIds.add(type.id())) {
                    chosen.add(type);
                    if (chosen.size() == WEAPON_COUNT) {
                        break;
                    }
                }
            }
        }
        return chosen;
    }

    private List<GridCell> shuffledAvailableCells(DungeonMap map) {
        List<GridCell> cells = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (isAvailable(cell)) {
                    cells.add(cell);
                }
            }
        }
        Collections.shuffle(cells, random);
        return cells;
    }

    private boolean isAvailable(GridCell cell) {
        return cell != null
                && cell.isWalkable()
                && cell.getItemsView().isEmpty()
                && cell.getEntitiesView().isEmpty();
    }
}
