package view;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import engine.BuildModeController;
import engine.BuildRandomItemPlacer;
import engine.BuildTool;
import engine.GameEngine;
import engine.TeamMatchController;
import model.DecorativeObject;
import model.DungeonMap;
import model.GridCell;
import model.Item;
import view.assets.SpriteRegistry;

/**
 * Build-mode screen for sketching a map before handing it to play mode.
 *
 * <p>GRASP: this class stays a View. It delegates use-case decisions to
 * {@link BuildModeController}; object creation lives in the build tool catalog,
 * and placement rules live behind a Strategy.
 */
public class DesignWindow extends JFrame {

    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 620;

    private static final Color CONTROL_BACKGROUND = new Color(18, 17, 22);
    private static final Color CONTROL_BORDER = new Color(103, 91, 75);
    private static final Color GOLD = new Color(214, 170, 70);
    private static final Color TEXT = new Color(240, 222, 180);
    private static final Map<String, Color> TOOL_COLORS = Map.ofEntries(
            Map.entry("FLOOR", new Color(42, 45, 57)),
            Map.entry("WALL", new Color(94, 82, 72)),
            Map.entry("CHEST", new Color(135, 82, 34)),
            Map.entry("LOCKED_CHEST", new Color(156, 117, 50)),
            Map.entry("CRATE", new Color(119, 78, 43)),
            Map.entry("COLUMN", new Color(120, 115, 132)),
            Map.entry("VASE", new Color(165, 105, 79)),
            Map.entry("WATER_PIPE", new Color(77, 114, 156)),
            Map.entry("PEDESTAL", new Color(128, 123, 118)),
            Map.entry("POOL", new Color(64, 132, 152)),
            Map.entry("GARGOYLE", new Color(98, 101, 106)),
            Map.entry("MISSING_BRICK", new Color(74, 110, 84)),
            Map.entry("HEAL", new Color(190, 66, 58)),
            Map.entry("ENERGY", new Color(70, 128, 204)),
            Map.entry("MANA", new Color(124, 86, 188)),
            Map.entry("KEY", new Color(202, 185, 113)),
            Map.entry("WEAPON", new Color(157, 157, 178)),
            Map.entry("ARMOR", new Color(92, 124, 160)),
            Map.entry("RING", new Color(179, 70, 98)),
            Map.entry("VALUABLE", new Color(210, 210, 235)));

    private final BuildModeController controller = new BuildModeController();
    private final TeamMatchController teamMatchController = new TeamMatchController();
    private final DesignCanvas canvas;
    private final List<ToolButton> toolButtons = new ArrayList<>();
    private Map<String, List<BuildTool>> paletteGroups = Map.of();
    private JTabbedPane paletteTabs;
    private JLabel selectedLabel;
    private JButton randomItemsButton;
    private Path lastMapPath;

    public DesignWindow() {
        setTitle("Dungeon Krawler - Design Mode");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        RetroTheme.styleFrameDark(this);

        canvas = new DesignCanvas();
        List<BuildTool> paletteTools = controller.getTools();
        paletteGroups = groupTools(paletteTools);

        JPanel wrap = new JPanel(new BorderLayout());
        RetroTheme.stylePanelDark(wrap);
        wrap.add(createPalettePanel("PALETTE"), BorderLayout.NORTH);
        wrap.add(canvas, BorderLayout.CENTER);
        wrap.add(createCommandPanel(), BorderLayout.SOUTH);
        setContentPane(wrap);

        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
    }

    private JPanel createPalettePanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CONTROL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, CONTROL_BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        panel.setPreferredSize(new Dimension(WINDOW_W, 140));

        JLabel titleLabel = new JLabel(title, JLabel.LEFT);
        titleLabel.setForeground(GOLD);
        titleLabel.setFont(controlFont(11f));

        selectedLabel = new JLabel();
        selectedLabel.setForeground(TEXT);
        selectedLabel.setFont(controlFont(12f));
        selectedLabel.setHorizontalAlignment(JLabel.RIGHT);
        refreshSelectedLabel();

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 6, 0));
        header.add(titleLabel, BorderLayout.WEST);
        header.add(selectedLabel, BorderLayout.EAST);

        paletteTabs = new JTabbedPane();
        paletteTabs.setFont(controlFont(11f));
        paletteTabs.setForeground(TEXT);
        paletteTabs.setBackground(CONTROL_BACKGROUND);
        paletteTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        for (Map.Entry<String, List<BuildTool>> entry : paletteGroups.entrySet()) {
            JPanel carpet = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
            carpet.setBackground(new Color(28, 26, 32));
            for (BuildTool tool : entry.getValue()) {
                carpet.add(createToolButton(tool));
            }
            JScrollPane scroll = new JScrollPane(carpet,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroll.setBorder(BorderFactory.createLineBorder(CONTROL_BORDER, 1));
            scroll.getViewport().setBackground(new Color(28, 26, 32));
            scroll.setPreferredSize(new Dimension(WINDOW_W - 24, 72));
            styleCarpetScrollBar(scroll.getHorizontalScrollBar());
            paletteTabs.addTab(entry.getKey(), scroll);
        }
        selectPaletteTab(controller.getSelectedTool());

        panel.add(header, BorderLayout.NORTH);
        panel.add(paletteTabs, BorderLayout.CENTER);
        return panel;
    }

    private void styleCarpetScrollBar(JScrollBar scrollBar) {
        scrollBar.setUI(new CarpetScrollBarUI());
        scrollBar.setPreferredSize(new Dimension(0, 17));
        scrollBar.setUnitIncrement(52);
        scrollBar.setBlockIncrement(208);
        scrollBar.setBackground(CONTROL_BACKGROUND);
    }

    private JPanel createCommandPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 6));
        panel.setBackground(CONTROL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CONTROL_BORDER),
                new EmptyBorder(9, 10, 9, 10)));
        JPanel editRow = commandRow();
        JPanel runRow = commandRow();

        JButton save = new CommandButton("SAVE");
        save.addActionListener(e -> saveMap());

        JButton load = new CommandButton("LOAD");
        load.addActionListener(e -> loadMap());

        JButton clear = new CommandButton("CLEAR");
        clear.addActionListener(e -> {
            controller.clearMap();
            refreshRandomItemsButton();
            refreshSelectedLabel();
            canvas.repaint();
        });

        randomItemsButton = new CommandButton("ADD 5 RANDOM ITEMS");
        randomItemsButton.addActionListener(e -> {
            BuildRandomItemPlacer.Result result = controller.addFiveRandomItems();
            refreshRandomItemsButton();
            refreshSelectedLabel("Added " + result.visibleItemsPlaced()
                    + " items" + (result.hiddenItemPlaced() ? " + hidden item" : ""));
            canvas.repaint();
        });
        refreshRandomItemsButton();

        JButton run = new CommandButton("RUN IN PLAY MODE");
        run.addActionListener(e -> {
            GameEngine engine = new GameEngine(controller.getDesignMap());
            dispose();
            SwingUtilities.invokeLater(() -> new GameWindow(engine).setVisible(true));
        });

        JButton teamMatch = new CommandButton("RUN TEAM MATCH");
        teamMatch.addActionListener(e -> runTeamMatch());

        JButton menu = new CommandButton("EXIT TO MENU");
        menu.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new MainMenuWindow().setVisible(true));
        });

        editRow.add(save);
        editRow.add(load);
        editRow.add(clear);
        editRow.add(randomItemsButton);
        runRow.add(run);
        runRow.add(teamMatch);
        runRow.add(menu);
        panel.add(editRow);
        panel.add(runRow);
        return panel;
    }

    private JPanel commandRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);
        return row;
    }

    private void refreshRandomItemsButton() {
        if (randomItemsButton == null) {
            return;
        }
        int remaining = controller.getRemainingRandomItemAdds();
        randomItemsButton.setEnabled(remaining > 0);
        randomItemsButton.setText(remaining > 0
                ? "ADD 5 RANDOM ITEMS (" + remaining + " LEFT)"
                : "RANDOM ITEMS LIMIT REACHED");
    }

    private void runTeamMatch() {
        try {
            GameEngine engine = teamMatchController.startFromDesignMap(controller.getDesignMap());
            dispose();
            SwingUtilities.invokeLater(() -> new GameWindow(engine).setVisible(true));
        } catch (IllegalArgumentException ex) {
            ItemActionMenuDialog.showNotice(this, "Team Match", "Cannot Start",
                    ex.getMessage());
        }
    }

    private void saveMap() {
        BuildMapFileDialog.showSave(this, lastMapPath).ifPresent(path -> {
            try {
                controller.saveMap(path);
                lastMapPath = path;
                refreshSelectedLabel("Saved " + path.getFileName());
            } catch (IOException ex) {
                ItemActionMenuDialog.showNotice(this, "Build", "Save Failed", ex.getMessage());
            }
        });
    }

    private void loadMap() {
        BuildMapFileDialog.showLoad(this, lastMapPath).ifPresent(path -> {
            try {
                controller.loadMap(path);
                lastMapPath = path;
                refreshRandomItemsButton();
                refreshSelectedLabel("Loaded " + path.getFileName());
                canvas.repaint();
            } catch (IOException ex) {
                ItemActionMenuDialog.showNotice(this, "Build", "Load Failed", ex.getMessage());
            }
        });
    }

    private void selectTool(BuildTool tool) {
        if (tool == null) {
            return;
        }
        controller.selectTool(tool);
        selectPaletteTab(tool);
        refreshSelectedLabel();
        for (ToolButton button : toolButtons) {
            button.setSelected(button.tool.equals(controller.getSelectedTool()));
        }
    }

    private void refreshSelectedLabel() {
        refreshSelectedLabel("Selected: " + controller.getSelectedTool().label());
    }

    private void refreshSelectedLabel(String message) {
        if (selectedLabel != null) {
            selectedLabel.setText(message);
        }
    }

    private void applyToolAtPoint(Point point, BuildTool tool) {
        if (point == null || tool == null) {
            return;
        }
        int[] cell = cellAtPoint(point);
        if (cell != null && controller.placeToolAt(cell[0], cell[1], tool)) {
            canvas.repaint();
        }
    }

    private void eraseAtPoint(Point point) {
        int[] cell = cellAtPoint(point);
        if (cell != null && controller.eraseAt(cell[0], cell[1])) {
            canvas.repaint();
        }
    }

    private int[] cellAtPoint(Point point) {
        if (point == null) {
            return null;
        }
        int tile = canvas.tileSize();
        int offsetX = canvas.offsetX(tile);
        int offsetY = canvas.offsetY(tile);
        DungeonMap map = controller.getDesignMap();
        if (point.x < offsetX || point.y < offsetY
                || point.x >= offsetX + map.getWidth() * tile
                || point.y >= offsetY + map.getHeight() * tile) {
            return null;
        }
        int x = (point.x - offsetX) / tile;
        int y = (point.y - offsetY) / tile;
        return new int[] { x, y };
    }

    private boolean isEraseGesture(MouseEvent e) {
        return SwingUtilities.isRightMouseButton(e)
                || (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0
                || e.isPopupTrigger();
    }

    private boolean isPlaceGesture(MouseEvent e) {
        return SwingUtilities.isLeftMouseButton(e)
                || (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
    }

    private Color fallbackColorFor(Item item) {
        for (BuildTool tool : controller.getTools()) {
            Item sample = tool.previewItem();
            if (sample != null && sample.getClass() == item.getClass()) {
                return colorFor(tool);
            }
        }
        return new Color(210, 210, 235);
    }

    private static Color colorFor(BuildTool tool) {
        return TOOL_COLORS.getOrDefault(tool.id(), new Color(150, 140, 125));
    }

    private static Font controlFont(float size) {
        Font base = RetroTheme.UI_MONO == null
                ? new Font(Font.MONOSPACED, Font.BOLD, Math.round(size))
                : RetroTheme.UI_MONO;
        return base.deriveFont(Font.PLAIN, size);
    }

    private final class DesignCanvas extends JPanel {
        private static final int PADDING = 26;

        DesignCanvas() {
            setBackground(new Color(10, 10, 14));
            setTransferHandler(new CanvasTransferHandler());

            MouseAdapter painter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEraseGesture(e)) {
                        eraseAtPoint(e.getPoint());
                    } else if (isPlaceGesture(e)) {
                        applyToolAtPoint(e.getPoint(), controller.getSelectedTool());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isEraseGesture(e)) {
                        eraseAtPoint(e.getPoint());
                    } else if (isPlaceGesture(e)) {
                        applyToolAtPoint(e.getPoint(), controller.getSelectedTool());
                    }
                }
            };
            addMouseListener(painter);
            addMouseMotionListener(painter);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(
                    BuildModeController.DEFAULT_MAP_WIDTH * 34,
                    BuildModeController.DEFAULT_MAP_HEIGHT * 34);
        }

        int tileSize() {
            DungeonMap map = controller.getDesignMap();
            int availableW = Math.max(1, getWidth() - PADDING * 2);
            int availableH = Math.max(1, getHeight() - PADDING * 2);
            return Math.max(1, Math.min(availableW / map.getWidth(), availableH / map.getHeight()));
        }

        int offsetX(int tileSize) {
            DungeonMap map = controller.getDesignMap();
            return (getWidth() - map.getWidth() * tileSize) / 2;
        }

        int offsetY(int tileSize) {
            DungeonMap map = controller.getDesignMap();
            return (getHeight() - map.getHeight() * tileSize) / 2;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                DungeonMap map = controller.getDesignMap();
                int tile = tileSize();
                int offsetX = offsetX(tile);
                int offsetY = offsetY(tile);

                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(offsetX - 10, offsetY - 10,
                        map.getWidth() * tile + 20,
                        map.getHeight() * tile + 20);

                for (int x = 0; x < map.getWidth(); x++) {
                    for (int y = 0; y < map.getHeight(); y++) {
                        GridCell cell = map.getCell(x, y);
                        int px = offsetX + x * tile;
                        int py = offsetY + y * tile;
                        drawCell(g2, cell, px, py, tile);
                    }
                }

                g2.setFont(controlFont(12f));
                g2.setColor(new Color(190, 180, 160));
                g2.drawString("Select an asset above and paint with the mouse.",
                        offsetX, offsetY + map.getHeight() * tile + 24);
            } finally {
                g2.dispose();
            }
        }

        private void drawCell(Graphics2D g2, GridCell cell, int px, int py, int tile) {
            if (cell.isPassable()) {
                g2.setColor(new Color(34, 38, 49));
            } else {
                g2.setColor(new Color(86, 75, 67));
            }
            g2.fillRect(px, py, tile, tile);

            g2.setColor(new Color(5, 5, 9));
            g2.drawRect(px, py, tile, tile);

            if (!cell.getItemsView().isEmpty()) {
                Item item = cell.getItemsView().get(0);
                BufferedImage sprite = SpriteRegistry.spriteFor(item);
                if (sprite != null) {
                    int inset = item instanceof DecorativeObject ? 1 : Math.max(2, tile / 6);
                    g2.drawImage(sprite, px + inset, py + inset,
                            tile - inset * 2, tile - inset * 2, null);
                } else {
                    drawFallbackItem(g2, item, px, py, tile);
                }
            } else if (!cell.isPassable()) {
                g2.setColor(new Color(118, 104, 93));
                g2.fillRect(px + tile / 5, py + tile / 5, tile - tile * 2 / 5, tile - tile * 2 / 5);
            }
        }

        private void drawFallbackItem(Graphics2D g2, Item item, int px, int py, int tile) {
            int inset = Math.max(3, tile / 5);
            g2.setColor(fallbackColorFor(item));
            g2.fillRect(px + inset, py + inset, tile - inset * 2, tile - inset * 2);
            g2.setColor(TEXT);
            g2.setFont(controlFont(Math.max(9f, tile / 4f)));
            String marker = item.getName().isEmpty() ? "?" : item.getName().substring(0, 1);
            g2.drawString(marker, px + tile / 2 - 4, py + tile / 2 + 5);
        }
    }

    private final class CanvasTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                String toolId = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                BuildTool tool = controller.findTool(toolId);
                if (tool == null) {
                    return false;
                }
                selectTool(tool);
                applyToolAtPoint(support.getDropLocation().getDropPoint(), tool);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private static final class ToolTransferHandler extends TransferHandler {
        private final BuildTool tool;

        private ToolTransferHandler(BuildTool tool) {
            this.tool = tool;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(tool.id());
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }
    }

    private static final class ToolButton extends JToggleButton {
        private final BuildTool tool;

        private ToolButton(BuildTool tool) {
            super();
            this.tool = tool;
            setIcon(new ToolIcon(tool, 30));
            setToolTipText(tool.label());
            setPreferredSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
            setForeground(TEXT);
            setBackground(colorFor(tool).darker());
            setOpaque(true);
            setText(null);
            setBorder(BorderFactory.createLineBorder(GOLD, 1));
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private ToolButton createToolButton(BuildTool tool) {
        ToolButton button = new ToolButton(tool);
        button.setSelected(tool.equals(controller.getSelectedTool()));
        button.addActionListener(e -> selectTool(tool));
        button.setTransferHandler(new ToolTransferHandler(tool));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectTool(tool);
                JComponent component = (JComponent) e.getSource();
                component.getTransferHandler().exportAsDrag(component, e, TransferHandler.COPY);
            }
        });
        toolButtons.add(button);
        return button;
    }

    private void selectPaletteTab(BuildTool tool) {
        if (tool == null || paletteTabs == null) {
            return;
        }
        int index = new ArrayList<>(paletteGroups.keySet()).indexOf(categoryFor(tool));
        if (index >= 0 && index < paletteTabs.getTabCount()) {
            paletteTabs.setSelectedIndex(index);
        }
    }

    private static Map<String, List<BuildTool>> groupTools(List<BuildTool> tools) {
        Map<String, List<BuildTool>> groups = new LinkedHashMap<>();
        for (String category : List.of("Floors", "Walls & Doors", "Rugs", "Decor", "Searchable",
                "Containers", "Keys & Rings", "Valuables", "Loot")) {
            groups.put(category, new ArrayList<>());
        }
        for (BuildTool tool : tools) {
            groups.computeIfAbsent(categoryFor(tool), key -> new ArrayList<>()).add(tool);
        }
        groups.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return groups;
    }

    private static String categoryFor(BuildTool tool) {
        String id = tool == null ? "" : tool.id();
        if (id.equals("FLOOR") || id.startsWith("FLOOR_")) {
            return "Floors";
        }
        if (id.equals("WALL") || id.startsWith("WALL_") || id.startsWith("DOOR_")) {
            return "Walls & Doors";
        }
        if (id.startsWith("RUG_")) {
            return "Rugs";
        }
        if (id.startsWith("BANNER_") || id.startsWith("SIGN_") || id.startsWith("TORCH_")
                || id.startsWith("STAIRS_") || id.startsWith("TRAP_")
                || id.startsWith("SKULL_") || id.startsWith("TOMBSTONE_")) {
            return "Decor";
        }
        if (id.startsWith("MISSING_BRICK") || id.startsWith("GARGOYLE") || id.startsWith("POOL")
                || id.startsWith("WATER_PIPE") || id.startsWith("GRILL") || id.startsWith("COLUMN")
                || id.startsWith("HOLE") || id.equals("PEDESTAL") || id.equals("VASE")) {
            return "Searchable";
        }
        if (id.contains("CHEST") || id.startsWith("CRATE") || id.startsWith("BAG_")) {
            return "Containers";
        }
        if (id.equals("KEY") || id.startsWith("KEY_") || id.equals("RING") || id.startsWith("RING_")) {
            return "Keys & Rings";
        }
        if (id.equals("VALUABLE") || id.startsWith("VALUABLE_")
                || id.startsWith("TREASURE_") || id.startsWith("COIN_")) {
            return "Valuables";
        }
        return "Loot";
    }

    private static final class ToolIcon implements Icon {
        private final BuildTool tool;
        private final BufferedImage sprite;
        private final int size;
        private final Color color;

        private ToolIcon(BuildTool tool, int size) {
            this.tool = tool;
            this.size = size;
            this.color = colorFor(tool);
            Item sample = tool.previewItem();
            this.sprite = sample == null ? null : SpriteRegistry.spriteFor(sample);
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(new Color(14, 13, 18));
                g2.fillRect(x, y, size, size);

                if (tool.isFloorBrush()) {
                    paintFloor(g2, x, y);
                    return;
                }
                if (tool.isWallBrush()) {
                    paintWall(g2, x, y);
                    return;
                }
                if (sprite != null) {
                    int inset = 3;
                    g2.drawImage(sprite, x + inset, y + inset, size - inset * 2, size - inset * 2, null);
                    return;
                }
                paintFallbackObject(g2, x, y);
            } finally {
                g2.dispose();
            }
        }

        private void paintFloor(Graphics2D g2, int x, int y) {
            g2.setColor(color);
            g2.fillRect(x + 5, y + 5, size - 10, size - 10);
            g2.setColor(new Color(68, 73, 88));
            g2.drawRect(x + 7, y + 7, size - 15, size - 15);
        }

        private void paintWall(Graphics2D g2, int x, int y) {
            g2.setColor(color);
            g2.fillRect(x + 4, y + 4, size - 8, size - 8);
            g2.setColor(new Color(132, 117, 103));
            for (int yy = y + 8; yy < y + size - 4; yy += 7) {
                g2.drawLine(x + 5, yy, x + size - 6, yy);
            }
            g2.drawLine(x + size / 2, y + 5, x + size / 2, y + size - 6);
        }

        private void paintFallbackObject(Graphics2D g2, int x, int y) {
            g2.setColor(color);
            int cx = x + size / 2;
            int cy = y + size / 2;
            int[] xs = { cx, x + size - 7, cx, x + 7 };
            int[] ys = { y + 6, cy, y + size - 7, cy };
            g2.fillPolygon(xs, ys, 4);
            g2.setColor(new Color(245, 228, 188, 180));
            g2.drawPolygon(xs, ys, 4);
        }
    }

    private static final class CommandButton extends JButton {
        private CommandButton(String label) {
            super(label);
            RetroTheme.styleRetroButton(this, new Color(92, 61, 28));
            setFont(controlFont(13f));
            setFocusable(false);
        }
    }

    private static final class CarpetScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return new ScrollArrowButton(orientation);
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return new ScrollArrowButton(orientation);
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(42, 13);
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setColor(new Color(5, 5, 9));
                g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                g2.setColor(new Color(43, 38, 37));
                g2.fillRect(bounds.x + 2, bounds.y + 2,
                        Math.max(0, bounds.width - 4), Math.max(0, bounds.height - 4));
                g2.setColor(CONTROL_BORDER);
                g2.drawLine(bounds.x + 2, bounds.y + 2, bounds.x + bounds.width - 3, bounds.y + 2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            if (bounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int x = bounds.x + 2;
                int y = bounds.y + 2;
                int w = Math.max(1, bounds.width - 4);
                int h = Math.max(1, bounds.height - 4);
                g2.setColor(new Color(5, 5, 9));
                g2.fillRect(x, y, w, h);
                g2.setColor(isDragging ? new Color(126, 84, 31) : new Color(92, 61, 28));
                g2.fillRect(x + 2, y + 2, Math.max(0, w - 4), Math.max(0, h - 4));
                g2.setColor(isDragging ? new Color(244, 205, 103) : GOLD);
                g2.drawRect(x + 1, y + 1, Math.max(0, w - 3), Math.max(0, h - 3));
                if (w >= 42) {
                    int gripX = x + w / 2 - 4;
                    g2.setColor(new Color(244, 205, 103, 190));
                    g2.drawLine(gripX, y + 4, gripX, y + h - 5);
                    g2.drawLine(gripX + 4, y + 4, gripX + 4, y + h - 5);
                    g2.drawLine(gripX + 8, y + 4, gripX + 8, y + h - 5);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class ScrollArrowButton extends JButton {
        private final int orientation;

        private ScrollArrowButton(int orientation) {
            this.orientation = orientation;
            setPreferredSize(new Dimension(17, 17));
            setMinimumSize(new Dimension(17, 17));
            setMaximumSize(new Dimension(17, 17));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusable(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setColor(new Color(5, 5, 9));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(getModel().isPressed() ? new Color(126, 84, 31) : new Color(43, 38, 37));
                g2.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
                g2.setColor(getModel().isRollover() ? new Color(244, 205, 103) : GOLD);
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int[] xs;
                int[] ys;
                if (orientation == SwingConstants.WEST) {
                    xs = new int[] { cx + 3, cx - 3, cx + 3 };
                    ys = new int[] { cy - 4, cy, cy + 4 };
                } else {
                    xs = new int[] { cx - 3, cx + 3, cx - 3 };
                    ys = new int[] { cy - 4, cy, cy + 4 };
                }
                g2.fillPolygon(xs, ys, 3);
            } finally {
                g2.dispose();
            }
        }
    }
}
