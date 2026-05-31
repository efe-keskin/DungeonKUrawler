package tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import engine.BuildMapFactory;
import engine.BuildMapPersistence;
import engine.BuildToolCatalog;
import engine.StandardBuildPlacementStrategy;
import model.Chest;
import model.Coin;
import model.Column;
import model.Crate;
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
import model.SearchableObject;
import model.ShadowCloneScroll;
import model.Vase;
import model.WaterPipe;

/**
 * Generates deterministic Build Mode JSON resources for the ten tower floors.
 *
 * <p>Run from the repository root after compiling:
 * {@code java -cp <classes>:<gson-jar> tools.TowerMapGenerator}.
 */
public final class TowerMapGenerator {

    private static final Path DEFAULT_OUTPUT = Path.of("src/main/resources/maps/tower");
    private static final String CHEST_DIR = "/items/chests/";
    private static final long LEVEL_SEED_STEP = 0x9E3779B97F4A7C15L;
    private static final List<LevelSpec> LEVELS = List.of(
            new LevelSpec(1, "Crypt Entrance", 16, 12, false, 8, 1, 2),
            new LevelSpec(2, "Dusty Halls", 16, 12, false, 14, 2, 3),
            new LevelSpec(3, "Forgotten Vault", 18, 13, false, 20, 3, 3),
            new LevelSpec(4, "Sunken Gallery", 18, 13, false, 27, 4, 4),
            new LevelSpec(5, "Warden's Lair", 20, 16, true, 35, 5, 4),
            new LevelSpec(6, "Ashen Catacombs", 20, 14, true, 43, 6, 5),
            new LevelSpec(7, "Veiled Passage", 20, 14, true, 51, 7, 5),
            new LevelSpec(8, "Shrouded Depths", 22, 16, true, 60, 8, 6),
            new LevelSpec(9, "Abyssal Threshold", 22, 16, true, 69, 9, 6),
            new LevelSpec(10, "Throne of the Dread King", 20, 16, true, 78, 11, 7));
    private static final List<String> CHEST_SPRITES = List.of(
            "01_chest_closed_blue_trim.png",
            "02_chest_closed_gold_trim.png",
            "07_ornate_chest_gold_tan.png");
    private final BuildMapPersistence persistence = new BuildMapPersistence(
            new BuildToolCatalog(), new BuildMapFactory(), new StandardBuildPlacementStrategy());

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? DEFAULT_OUTPUT : Path.of(args[0]);
        new TowerMapGenerator().generateAll(output);
    }

    public void generateAll(Path outputDirectory) throws IOException {
        for (LevelSpec level : LEVELS) {
            DungeonMap map = createMap(level);
            Path path = outputDirectory.resolve("floor%02d.json".formatted(level.number()));
            persistence.save(map, path);
            System.out.println(path + ": " + map.getWidth() + "x" + map.getHeight()
                    + ", blocked cells=" + countBlockedCells(map));
        }
    }

    private DungeonMap createMap(LevelSpec level) {
        DungeonMap map = createBorderedMap(level);
        Point exitApproach = new Point(level.width() - 2, level.height() / 2);
        Set<Point> reserved = Set.of(
                new Point(1, 1), new Point(2, 1), new Point(1, 2),
                new Point(2, 2), new Point(3, 1), new Point(1, 3),
                exitApproach, new Point(level.width() / 2, level.height() / 2));
        Set<Point> walls = addSafeWalls(level, reserved);
        Set<Point> breakables = addSafeBreakables(level, walls, reserved);
        Set<Point> openCells = reachable(level.width(), level.height(), walls, breakables);
        Set<Point> used = new HashSet<>(breakables);
        used.add(new Point(1, 1));
        used.add(exitApproach);

        for (Point wall : walls) {
            map.getCell(wall.x(), wall.y()).setPassable(false);
        }
        int index = 0;
        for (Point point : sorted(breakables)) {
            addItem(map, point, createBreakable(index++));
        }

        List<SearchableObject> searchables = placeSearchables(level, map, openCells);
        placeArchKey(level, map, openCells, used, searchables);
        placeLockedRewardKey(level, searchables);
        placeHiddenEnergyPotion(level, searchables);
        placeChests(level, map, openCells, used);

        if (!openCells.contains(exitApproach)) {
            throw new IllegalStateException("Floor " + level.number() + " exit is unreachable.");
        }
        return map;
    }

    private DungeonMap createBorderedMap(LevelSpec level) {
        DungeonMap map = new DungeonMap(level.name(), level.width(), level.height());
        map.setFogEnabled(level.fogEnabled());
        for (int y = 0; y < level.height(); y++) {
            for (int x = 0; x < level.width(); x++) {
                map.getCell(x, y).setPassable(!isBorder(level.width(), level.height(), x, y));
            }
        }
        return map;
    }

    private Set<Point> addSafeWalls(LevelSpec level, Set<Point> reserved) {
        Set<Point> walls = new LinkedHashSet<>();
        Point exitApproach = new Point(level.width() - 2, level.height() / 2);
        for (Point point : wallCandidates(level)) {
            if (walls.size() >= level.wallCount()) {
                break;
            }
            if (reserved.contains(point)) {
                continue;
            }
            Set<Point> proposal = new HashSet<>(walls);
            proposal.add(point);
            if (reachable(level.width(), level.height(), proposal, Set.of()).contains(exitApproach)) {
                walls.add(point);
            }
        }
        requireCount(level, "walls", walls.size(), level.wallCount());
        return walls;
    }

    private List<Point> wallCandidates(LevelSpec level) {
        List<Point> candidates = interiorPoints(level);
        java.util.Collections.shuffle(candidates, layoutRandom(level, 9000L));
        return candidates;
    }

    private Set<Point> addSafeBreakables(LevelSpec level, Set<Point> walls, Set<Point> reserved) {
        Set<Point> breakables = new LinkedHashSet<>();
        List<Point> candidates = interiorPoints(level);
        java.util.Collections.shuffle(candidates, layoutRandom(level, 12000L));
        Point exitApproach = new Point(level.width() - 2, level.height() / 2);
        for (Point point : candidates) {
            if (breakables.size() >= level.breakableCount()) {
                break;
            }
            if (walls.contains(point) || reserved.contains(point)) {
                continue;
            }
            Set<Point> proposal = new HashSet<>(breakables);
            proposal.add(point);
            if (reachable(level.width(), level.height(), walls, proposal).contains(exitApproach)) {
                breakables.add(point);
            }
        }
        requireCount(level, "breakables", breakables.size(), level.breakableCount());
        return breakables;
    }

    private List<SearchableObject> placeSearchables(LevelSpec level, DungeonMap map, Set<Point> openCells) {
        List<Point> wallSlots = new ArrayList<>();
        for (int[] row : new int[][] {{0, 1}, {level.height() - 1, level.height() - 2}}) {
            for (int x = 2; x < level.width() - 2; x++) {
                if (hasReachableNeighbor(openCells, x, row[1])) {
                    wallSlots.add(new Point(x, row[0]));
                }
            }
        }
        java.util.Collections.shuffle(wallSlots, layoutRandom(level, 15000L));
        requireCount(level, "searchable wall slots", wallSlots.size(), level.searchableCount());

        List<SearchableObject> searchables = new ArrayList<>();
        for (int index = 0; index < level.searchableCount(); index++) {
            SearchableObject searchable = createSearchable(index + level.number());
            addItem(map, wallSlots.get(index), searchable);
            searchables.add(searchable);
        }
        return searchables;
    }

    private void placeArchKey(LevelSpec level, DungeonMap map, Set<Point> openCells,
            Set<Point> used, List<SearchableObject> searchables) {
        Key archKey = new Key("arch-gold", KeyColor.GOLD);
        if (level.number() >= 5) {
            searchables.get(0).setHiddenItem(archKey);
            return;
        }
        Set<Point> available = available(openCells, used);
        Point keySpot = chooseFree(List.of(
                new Point(level.width() - 3, 2),
                new Point(level.width() / 2, 2),
                new Point(2, level.height() - 3)), available);
        addItem(map, keySpot, archKey);
        used.add(keySpot);
    }

    private void placeLockedRewardKey(LevelSpec level, List<SearchableObject> searchables) {
        if (level.number() < 6) {
            return;
        }
        KeyColor color = level.number() >= 9 ? KeyColor.ORANGE : KeyColor.SILVER;
        String keyId = level.number() >= 9 ? "orange" : "silver";
        searchables.get(1).setHiddenItem(new Key(keyId, color));
    }

    private void placeHiddenEnergyPotion(LevelSpec level, List<SearchableObject> searchables) {
        if (level.number() >= 8) {
            searchables.get(2).setHiddenItem(new EnergyPotion());
        }
    }

    private void placeChests(LevelSpec level, DungeonMap map, Set<Point> openCells, Set<Point> used) {
        int chestCount = level.number() < 5 ? 1 : level.number() < 9 ? 2 : 3;
        for (int index = 0; index < chestCount; index++) {
            Point chestSpot = chooseFree(List.of(
                    new Point(level.width() - 3 - index, level.height() - 3),
                    new Point(level.width() - 4, 2 + index),
                    new Point(2 + index, level.height() - 3)), available(openCells, used));
            boolean locked = level.number() >= 6 && index == chestCount - 1;
            String keyId = level.number() >= 9 ? "orange" : "silver";
            String label = locked ? "Locked Reward Chest" : "Floor " + level.number() + " Strongbox";
            Chest chest = locked
                    ? Chest.locked(label, 16, keyId, CHEST_DIR + CHEST_SPRITES.get(index))
                    : new Chest(label, 16, CHEST_DIR + CHEST_SPRITES.get(index));
            chest.addItem(index == 0 ? new HealPotion() : new ManaPotion());
            chest.addItem(new Coin(10 + level.number() * 3, "/items/golds_coins/20_coin_pile_gold.png"));
            // Roughly a third of chests also stash a Shadow Clone Scroll, one of the
            // few ways to obtain one now that it no longer spawns on the ground.
            if (layoutRandom(level, 21000L + index).nextDouble() < 0.34) {
                chest.addItem(new ShadowCloneScroll());
            }
            addItem(map, chestSpot, chest);
            used.add(chestSpot);
        }
    }

    private SearchableObject createSearchable(int index) {
        return switch (Math.floorMod(index, 11)) {
            case 0 -> new MissingBrick(MissingBrick.SPRITE_1, null);
            case 1 -> new MissingBrick(MissingBrick.SPRITE_2, null);
            case 2 -> new Gargoyle(Gargoyle.RED_LEFT_SPRITE, null);
            case 3 -> new Gargoyle(Gargoyle.GREEN_LEFT_SPRITE, null);
            case 4 -> new Gargoyle(Gargoyle.CYAN_LEFT_SPRITE, null);
            case 5 -> new Grill(Grill.HORIZONTAL_SPRITE, null);
            case 6 -> new Grill(Grill.VERTICAL_SPRITE, null);
            case 7 -> new Hole(Hole.SPRITE, null);
            case 8 -> new Hole(Hole.SPRITE_2, null);
            case 9 -> new Hole(Hole.SPRITE_3, null);
            default -> new Crate(Crate.WOOD_TALL_SPRITE, null);
        };
    }

    private Item createBreakable(int index) {
        return switch (Math.floorMod(index, 4)) {
            case 0 -> new Column(Column.GRAY_SPRITE);
            case 1 -> new Column(Column.PURPLE_SPRITE);
            case 2 -> new Vase();
            default -> new WaterPipe(WaterPipe.LARGE_RING_SPRITE);
        };
    }

    private Set<Point> reachable(int width, int height, Set<Point> walls, Set<Point> breakables) {
        Set<Point> blocked = new HashSet<>(walls);
        blocked.addAll(breakables);
        Point start = new Point(1, 1);
        if (blocked.contains(start)) {
            return Set.of();
        }
        Set<Point> seen = new HashSet<>();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            for (Point neighbor : List.of(
                    new Point(point.x() + 1, point.y()),
                    new Point(point.x() - 1, point.y()),
                    new Point(point.x(), point.y() + 1),
                    new Point(point.x(), point.y() - 1))) {
                if (neighbor.x() <= 0 || neighbor.y() <= 0
                        || neighbor.x() >= width - 1 || neighbor.y() >= height - 1
                        || blocked.contains(neighbor) || !seen.add(neighbor)) {
                    continue;
                }
                queue.addLast(neighbor);
            }
        }
        return seen;
    }

    private List<Point> interiorPoints(LevelSpec level) {
        List<Point> points = new ArrayList<>();
        for (int y = 2; y < level.height() - 2; y++) {
            for (int x = 2; x < level.width() - 2; x++) {
                points.add(new Point(x, y));
            }
        }
        return points;
    }

    private List<Point> sorted(Set<Point> points) {
        return points.stream()
                .sorted(Comparator.comparingInt(Point::x).thenComparingInt(Point::y))
                .toList();
    }

    private Set<Point> available(Set<Point> openCells, Set<Point> used) {
        Set<Point> available = new HashSet<>(openCells);
        available.removeAll(used);
        return available;
    }

    private Random layoutRandom(LevelSpec level, long salt) {
        return new Random(salt ^ (LEVEL_SEED_STEP * level.number()));
    }

    private Point chooseFree(List<Point> preferred, Set<Point> available) {
        for (Point point : preferred) {
            if (available.contains(point)) {
                return point;
            }
        }
        return available.stream()
                .min(Comparator.comparingInt(Point::y).thenComparingInt(Point::x))
                .orElseThrow(() -> new IllegalStateException("No open floor cell is available."));
    }

    private boolean hasReachableNeighbor(Set<Point> openCells, int x, int y) {
        for (int nearX = x - 1; nearX <= x + 1; nearX++) {
            if (openCells.contains(new Point(nearX, y))) {
                return true;
            }
        }
        return false;
    }

    private void addItem(DungeonMap map, Point point, Item item) {
        map.getCell(point.x(), point.y()).getItems().add(item);
    }

    private int countBlockedCells(DungeonMap map) {
        int count = 0;
        for (GridCell[] column : map.getCells()) {
            for (GridCell cell : column) {
                if (!cell.isPassable()) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isBorder(int width, int height, int x, int y) {
        return x == 0 || y == 0 || x == width - 1 || y == height - 1;
    }

    private void requireCount(LevelSpec level, String label, int actual, int required) {
        if (actual < required) {
            throw new IllegalStateException(
                    "Floor " + level.number() + " has only " + actual + "/" + required + " " + label + ".");
        }
    }

    private record LevelSpec(
            int number,
            String name,
            int width,
            int height,
            boolean fogEnabled,
            int wallCount,
            int breakableCount,
            int searchableCount) {
    }

    private record Point(int x, int y) {
    }
}
