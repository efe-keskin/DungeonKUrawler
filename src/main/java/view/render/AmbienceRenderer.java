package view.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import model.DungeonMap;
import model.GridCell;
import model.Item;
import model.SearchableObject;
import view.assets.AmbienceAsset;
import view.assets.AmbienceCatalog;

/**
 * Draws the dungeon's static ambience (floor and perimeter walls) from the
 * {@link AmbienceCatalog} tileset. Encapsulates which CSV labels are used so
 * {@link view.GamePanel} doesn't have to know about specific filenames or the
 * background_floor folder.
 *
 * <p>Wall sprites are not 1-cell-per-cell — each one's grid coverage is read
 * off {@link AmbienceAsset#cellsWide()} / {@link AmbienceAsset#cellsTall()},
 * which derive from the CSV's output_width/output_height in 32-pixel units. A
 * 96×32 sprite spans 3×1 grid cells, a 160×32 spans 5×1, and so on. The
 * renderer pre-computes a {@link Placement} list for the map (north row, south
 * row, west column, east column, plus interior wall fillers), packing variety
 * along each edge.
 *
 * <p>GRASP: Information Expert for "how do I dress a cell". GoF: Strategy —
 * swap this renderer to change the theme without touching the panel.
 */
public final class AmbienceRenderer {

    private static final String FLOOR_LABEL = "floor_worn_patch_round";

    /**
     * Non-searchable wall pool for the north/south rows. The 1-cell plain block
     * is intentionally repeated so it appears often; searchable details are
     * overlaid only on those plain-block cells.
     */
    private static final List<String> HORIZONTAL_WALL_LABELS = List.of(
            "wall_block_small",
            "wall_block_small",
            "wall_block_small",
            "wall_block_small",
            "wall_block_small",
            "wall_block_small",
            "wall_block_small",
            "wall_section_mid_arch_right",
            "wall_section_top_plain_left",
            "banner_brown",
            "banner_green",
            "banner_blue",
            "sign_gray");

    /**
     * East/west wall columns: sprite #11 from the CSV ({@code wall_frame_vertical_open},
     * 48×104 native ≈ 1.5×3 cells). Drawn at a forced 1-cell width (the frame
     * gets squeezed slightly horizontally) so the column doesn't extrude into
     * the playfield.
     */
    private static final List<String> VERTICAL_WALL_LABELS = List.of("wall_frame_vertical_open");

    /** Plain 1×1 block used to fill interior wall cells (non-perimeter). */
    private static final String INTERIOR_FILLER_LABEL = "wall_block_small";

    /** Fixed RNG seed — layouts are deterministic so they don't shuffle between repaints. */
    private static final long LAYOUT_SEED = 0xD0_6CE11L;

    private static final Color FALLBACK_FLOOR = new Color(32, 36, 48);
    private static final Color FALLBACK_WALL = new Color(200, 60, 60);

    private final AmbienceAsset floorAsset;
    private final List<AmbienceAsset> horizontalAssets;
    private final List<AmbienceAsset> verticalAssets;
    private final AmbienceAsset filler;

    private DungeonMap cachedLayoutMap;
    private List<Placement> cachedLayout;

    public AmbienceRenderer() {
        AmbienceCatalog cat = AmbienceCatalog.get();
        this.floorAsset = cat.byLabel(FLOOR_LABEL);
        this.horizontalAssets = HORIZONTAL_WALL_LABELS.stream()
                .map(cat::byLabel)
                .filter(Objects::nonNull)
                .filter(a -> a.cellsTall() == 1)
                .toList();
        // No cellsWide==1 filter: the column layout forces width=1 and squeezes
        // wider sprites (e.g. the 1.5-cell frame) into the column, so we can
        // accept any vertical-leaning art here.
        this.verticalAssets = VERTICAL_WALL_LABELS.stream()
                .map(cat::byLabel)
                .filter(Objects::nonNull)
                .toList();
        this.filler = cat.byLabel(INTERIOR_FILLER_LABEL);
    }

    /** Draws the floor sprite (or a fallback color) at the given cell rect. */
    public void drawFloor(Graphics2D g2, int px, int py, int tileSize) {
        BufferedImage img = floorAsset == null ? null : floorAsset.image();
        if (img != null) {
            g2.drawImage(img, px, py, tileSize, tileSize, null);
            return;
        }
        g2.setColor(FALLBACK_FLOOR);
        g2.fillRect(px, py, tileSize, tileSize);
    }

    /**
     * Paints every wall placement in one pass. Each placement spans whatever
     * grid coverage the source sprite implies (from sizes.csv), so wide
     * decorative pieces visibly span multiple cells instead of being squashed
     * into a square.
     */
    public void drawWalls(Graphics2D g2, DungeonMap map, int tileSize, int offsetX, int offsetY) {
        if (map == null) {
            return;
        }
        for (Placement p : layoutFor(map)) {
            int px = offsetX + p.x() * tileSize;
            int py = offsetY + p.y() * tileSize;
            int w = p.cw() * tileSize;
            int h = p.ch() * tileSize;
            BufferedImage img = p.asset() == null ? null : p.asset().image();
            if (img != null) {
                if (p.flipVertical()) {
                    g2.drawImage(img, px, py + h, w, -h, null);
                } else {
                    g2.drawImage(img, px, py, w, h, null);
                }
            } else {
                g2.setColor(FALLBACK_WALL);
                g2.fillRect(px, py, w, h);
            }
        }
    }

    private List<Placement> layoutFor(DungeonMap map) {
        if (map == cachedLayoutMap && cachedLayout != null) {
            return cachedLayout;
        }
        List<Placement> out = new ArrayList<>();
        Random rng = new Random(LAYOUT_SEED);
        int w = map.getWidth();
        int h = map.getHeight();

        // North and south rows span the full width and own the four corners.
        layoutHorizontalRow(out, rng, map, 0, w, 0, false);
        layoutHorizontalRow(out, rng, map, 0, w, h - 1, true);

        // West and east columns fill between the corners, which the rows already covered.
        layoutVerticalColumn(out, rng, 1, h - 1, 0);
        layoutVerticalColumn(out, rng, 1, h - 1, w - 1);

        // Any interior wall cells (e.g. the 2×2 block in the demo map) get the plain filler.
        for (int x = 1; x < w - 1; x++) {
            for (int y = 1; y < h - 1; y++) {
                GridCell cell = map.getCell(x, y);
                if (cell != null && !cell.isPassable()) {
                    out.add(new Placement(x, y, 1, 1, filler, false));
                }
            }
        }

        cachedLayoutMap = map;
        cachedLayout = out;
        return out;
    }

    private void layoutHorizontalRow(List<Placement> out, Random rng, DungeonMap map, int xStart, int xEnd, int y,
            boolean flipVertical) {
        int x = xStart;
        while (x < xEnd) {
            if (hasSearchableObject(map, x, y)) {
                out.add(new Placement(x, y, 1, 1, filler, flipVertical));
                x++;
                continue;
            }
            int remaining = Math.min(xEnd - x, cellsUntilNextSearchable(map, x, xEnd, y));
            AmbienceAsset pick = pickFitting(horizontalAssets, rng, remaining, true);
            int cw = pick == null ? 1 : pick.cellsWide();
            int ch = pick == null ? 1 : pick.cellsTall();
            out.add(new Placement(x, y, cw, ch, pick, flipVertical));
            x += Math.max(1, cw);
        }
    }

    private int cellsUntilNextSearchable(DungeonMap map, int xStart, int xEnd, int y) {
        int count = 0;
        for (int x = xStart; x < xEnd; x++) {
            if (hasSearchableObject(map, x, y)) {
                break;
            }
            count++;
        }
        return Math.max(1, count);
    }

    private boolean hasSearchableObject(DungeonMap map, int x, int y) {
        GridCell cell = map.getCell(x, y);
        if (cell == null) {
            return false;
        }
        for (Item item : cell.getItemsView()) {
            if (item instanceof SearchableObject) {
                return true;
            }
        }
        return false;
    }

    /**
     * Side wall layout. Width is forced to 1 cell so wider sprites (e.g. the
     * 1.5-cell frame) get squeezed into the column instead of leaking onto the
     * floor. Height is the asset's native {@code cellsTall} clamped to whatever
     * space remains — so the final cell of the column scales the sprite down
     * to fit instead of falling back to a different tile.
     */
    private void layoutVerticalColumn(List<Placement> out, Random rng, int yStart, int yEnd, int x) {
        int y = yStart;
        while (y < yEnd) {
            int remaining = yEnd - y;
            AmbienceAsset pick = verticalAssets.isEmpty()
                    ? filler
                    : verticalAssets.get(rng.nextInt(verticalAssets.size()));
            int nativeTall = pick == null ? 1 : Math.max(1, pick.cellsTall());
            int ch = Math.min(remaining, nativeTall);
            out.add(new Placement(x, y, 1, ch, pick, false));
            y += ch;
        }
    }

    /**
     * Random pick from {@code pool} restricted to entries that fit within
     * {@code maxCells} along the requested axis. Falls back to the plain filler
     * when nothing fits (shouldn't happen for {@code maxCells >= 1}).
     */
    private AmbienceAsset pickFitting(List<AmbienceAsset> pool, Random rng, int maxCells, boolean horizontalAxis) {
        if (pool.isEmpty()) {
            return filler;
        }
        List<AmbienceAsset> fitting = new ArrayList<>(pool.size());
        for (AmbienceAsset a : pool) {
            int span = horizontalAxis ? a.cellsWide() : a.cellsTall();
            if (span <= maxCells) {
                fitting.add(a);
            }
        }
        if (fitting.isEmpty()) {
            return filler;
        }
        return fitting.get(rng.nextInt(fitting.size()));
    }

    /** A laid-out wall sprite: anchor cell, grid span, and the asset to draw. */
    private record Placement(int x, int y, int cw, int ch, AmbienceAsset asset, boolean flipVertical) {
    }
}
