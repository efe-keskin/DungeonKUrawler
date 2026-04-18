package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import engine.Direction;
import engine.GameEngine;
import engine.InventoryController;
import engine.PlayerModeController;
import engine.GameStateListener;
import engine.InteractionController;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Knight;
import model.Sorcerer;

/**
 * Observer: implements {@link GameStateListener} and repaints when the engine
 * notifies — no direct
 * model mutation. Input is forwarded to {@link GameEngine#moveHero(Direction)}
 * only; movement rules
 * stay in the controller.
 */
public class GamePanel extends JPanel implements GameStateListener {

    private static final int BASE_CELL = 28;

    private static final Color FLOOR = new Color(32, 36, 48);
    private static final Color WALL = new Color(200, 60, 60);
    /** Visible grid lines (retro tile border). */
    private static final Color GRID_LINE = new Color(55, 55, 62);
    private static final Color HERO = new Color(50, 130, 255);
    private static final Color KNIGHT = new Color(220, 55, 55);
    private static final Color SORCERER = new Color(160, 70, 220);
    private static final Color ITEM = new Color(255, 200, 40);

    private final GameEngine engine;
    private final PlayerModeController playerModeController;
    private final InteractionController interactionController;

    public GamePanel(GameEngine engine, PlayerModeController playerModeController,
            InteractionController interactionController) {
        this.engine = engine;
        this.playerModeController = playerModeController;
        this.interactionController = interactionController;

        engine.addGameStateListener(this);
        setBackground(Color.BLACK);
        setOpaque(true);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Direction d = Direction.fromKeyCode(e.getKeyCode());
                if (d != null) {
                    GamePanel.this.playerModeController.moveHero(d);
                }
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                DungeonMap map = GamePanel.this.engine.getDungeonMap();
                if (map == null || map.getWidth() <= 0 || map.getHeight() <= 0) {
                    return;
                }

                int tileSize = getTileSize(map);
                int mapPixelW = map.getWidth() * tileSize;
                int mapPixelH = map.getHeight() * tileSize;
                int offsetX = (Math.max(1, GamePanel.this.getWidth()) - mapPixelW) / 2;
                int offsetY = (Math.max(1, GamePanel.this.getHeight()) - mapPixelH) / 2;

                // Ignore clicks outside the centered square map.
                if (e.getX() < offsetX || e.getX() >= offsetX + mapPixelW
                        || e.getY() < offsetY || e.getY() >= offsetY + mapPixelH) {
                    return;
                }

                int gridX = (e.getX() - offsetX) / tileSize;
                int gridY = (e.getY() - offsetY) / tileSize;

                InteractionController.ItemInteraction interaction = GamePanel.this.interactionController
                        .getItemInteraction(gridX, gridY);
                if (interaction == null) {
                    return;
                }

                Window parent = SwingUtilities.getWindowAncestor(GamePanel.this);
                String message = "Item: " + interaction.getItemName() + "\n"
                        + "Takable: " + (interaction.isTakable() ? "Yes" : "No");

                if (interaction.isTakable()) {
                    Object[] options = { "Take", "Return to map" };
                    int choice = JOptionPane.showOptionDialog(
                            parent,
                            message,
                            "Item Interaction",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (choice == 0) {
                        InventoryController.PickupResult result = GamePanel.this.interactionController
                                .takeItemAt(interaction.getX(), interaction.getY());
                        if (result != InventoryController.PickupResult.SUCCESS) {
                            JOptionPane.showMessageDialog(parent, getPickupFailureMessage(result),
                                    "Cannot Take Item", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(parent, message, "Item Interaction", JOptionPane.INFORMATION_MESSAGE);
                }

                GamePanel.this.requestFocusInWindow();
            }
        });



    }

    @Override
    public void removeNotify() {
        engine.removeGameStateListener(this);
        super.removeNotify();
    }

    @Override
    public void onGameStateChanged() {
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public Dimension getPreferredSize() {
        DungeonMap map = engine.getDungeonMap();
        return new Dimension(map.getWidth() * BASE_CELL + 1, map.getHeight() * BASE_CELL + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            DungeonMap map = engine.getDungeonMap();
            int mapW = Math.max(1, map.getWidth());
            int mapH = Math.max(1, map.getHeight());
            int tileSize = getTileSize(map);
            // Center the map and leave black background in the spare area.
            int offsetX = (Math.max(1, getWidth()) - mapW * tileSize) / 2;
            int offsetY = (Math.max(1, getHeight()) - mapH * tileSize) / 2;
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    GridCell cell = map.getCell(x, y);
                    if (cell == null) {
                        continue;
                    }
                    int px = offsetX + x * tileSize;
                    int py = offsetY + y * tileSize;
                    int cellW = tileSize;
                    int cellH = tileSize;
                    g2.setColor(cell.isPassable() ? FLOOR : WALL);
                    g2.fillRect(px, py, cellW, cellH);
                    g2.setColor(GRID_LINE);
                    g2.drawRect(px, py, cellW, cellH);

                    if (!cell.getItemsView().isEmpty()) {
                        drawItemGold(g2, px, py, cellW, cellH);
                    }

                    for (Entity ent : cell.getEntitiesView()) {
                        g2.setColor(ent instanceof Hero ? HERO
                                : ent instanceof Knight ? KNIGHT
                                        : ent instanceof Sorcerer ? SORCERER
                                                : Color.LIGHT_GRAY);
                        int inset = Math.max(1, Math.min(cellW, cellH) / 5);
                        int entityW = Math.max(1, cellW - inset * 2);
                        int entityH = Math.max(1, cellH - inset * 2);
                        g2.fillRect(px + inset, py + inset, entityW, entityH);
                    }
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private int getTileSize(DungeonMap map) {
        int mapW = Math.max(1, map.getWidth());
        int mapH = Math.max(1, map.getHeight());
        int availableW = Math.max(1, getWidth());
        int availableH = Math.max(1, getHeight());
        // Keep tile aspect ratio fixed by using the limiting dimension.
        return Math.max(1, Math.min(availableW / mapW, availableH / mapH));
    }

    private String getPickupFailureMessage(InventoryController.PickupResult result) {
        return switch (result) {
            case NO_ITEM -> "No item is available in this tile.";
            case NOT_ADJACENT -> "You can only take items from adjacent tiles.";
            case NOT_TAKABLE -> "This item cannot be taken.";
            case INVENTORY_FULL -> "Inventory is full.";
            case SUCCESS -> "";
        };
    }

    private void drawItemGold(Graphics2D g2, int px, int py, int cellW, int cellH) {
        g2.setColor(ITEM);
        int inset = Math.max(1, Math.min(cellW, cellH) / 3);
        int itemW = Math.max(1, cellW - inset * 2);
        int itemH = Math.max(1, cellH - inset * 2);
        g2.fillRect(px + inset, py + inset, itemW, itemH);
        g2.setColor(GRID_LINE);
        g2.drawRect(px + inset, py + inset, itemW, itemH);
    }

}
