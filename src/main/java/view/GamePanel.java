package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import engine.Direction;
import engine.GameEngine;
import engine.InventoryController;
import engine.LockController;
import engine.PlayerModeController;
import engine.GameStateListener;
import engine.InteractionController;
import model.Container;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Item;
import model.Knight;
import model.Potion;
import model.Sorcerer;
import view.assets.SpriteRegistry;
import view.render.AmbienceRenderer;

/**
 * Observer: implements {@link GameStateListener} and repaints when the engine
 * notifies — no direct
 * model mutation. Input is forwarded to {@link GameEngine#moveHero(Direction)}
 * only; movement rules
 * stay in the controller.
 */
public class GamePanel extends JPanel implements GameStateListener {

    private static final int BASE_CELL = 28;

    /** Visible grid lines (retro tile border). */
    private static final Color GRID_LINE = new Color(55, 55, 62);
    private static final Color HERO = new Color(50, 130, 255);
    private static final Color KNIGHT = new Color(220, 55, 55);
    private static final Color SORCERER = new Color(160, 70, 220);
    private static final Color ITEM = new Color(255, 200, 40);
    private static final Color HUD_HP = new Color(220, 80, 80);
    private static final Color HUD_ENERGY = new Color(230, 200, 60);
    private static final Color HUD_COINS = new Color(235, 178, 45);
    private static final Color HUD_STR = new Color(205, 90, 70);
    private static final Color HUD_DEF = new Color(90, 140, 225);
    private static final Color HUD_TEXT = new Color(240, 240, 240);

    private static final int HERO_ANIM_INTERVAL_MS = 100;
    private static final int ENERGY_REFILL_INTERVAL_MS = 300;
    private static final float HERO_ANIM_STEP = 0.20f;
    private static final float HERO_SPRITE_SCALE = 1.15f;

    private final GameEngine engine;
    private final PlayerModeController playerModeController;
    private final InteractionController interactionController;
    private final AmbienceRenderer ambienceRenderer = new AmbienceRenderer();
    private final Timer heroAnimTimer;
    private final Timer energyRefillTimer;
    private int heroFrame = 0;
    private int heroLastX = Integer.MIN_VALUE;
    private int heroLastY = Integer.MIN_VALUE;
    private int heroAnimDx = 0;
    private int heroAnimDy = 0;
    private float heroAnimProgress = 1f;
    private float heroPixelOffsetX = 0f;
    private float heroPixelOffsetY = 0f;
    private boolean heroFacingLeft = false;

    public GamePanel(GameEngine engine, PlayerModeController playerModeController,
            InteractionController interactionController) {
        this.engine = engine;
        this.playerModeController = playerModeController;
        this.interactionController = interactionController;

        Hero hero = engine.getHero();
        if (hero != null) {
            heroLastX = hero.getX();
            heroLastY = hero.getY();
        }

        engine.addGameStateListener(this);
        setBackground(Color.BLACK);
        setOpaque(true);
        setFocusable(true);

        heroAnimTimer = new Timer(HERO_ANIM_INTERVAL_MS, e -> {
            if (heroAnimProgress < 1f) {
                heroAnimProgress = Math.min(1f, heroAnimProgress + HERO_ANIM_STEP);
                heroFrame = (heroFrame + 1) % SpriteRegistry.heroFrameCount();
                if (heroAnimProgress >= 1f) {
                    heroFrame = 0;
                    heroAnimDx = 0;
                    heroAnimDy = 0;
                    heroPixelOffsetX = 0f;
                    heroPixelOffsetY = 0f;
                }
                repaint();
            }
        });
        heroAnimTimer.start();

        energyRefillTimer = new Timer(ENERGY_REFILL_INTERVAL_MS, e -> engine.tickEnergyRefill());
        energyRefillTimer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_T) {
                    handleTakeKeyPress();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_O) {
                    handleOpenKeyPress();
                    return;
                }

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
                if (interaction.getDetail() != null) {
                    message += "\n" + interaction.getDetail();
                }

                if (interaction.isTakable()) {
                    Object[] options = { interaction.getActionLabel(), "Return to map" };
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

    private void handleOpenKeyPress() {
        Container container = engine.findContainerNearHero();
        Window parent = SwingUtilities.getWindowAncestor(this);
        if (container == null) {
            JOptionPane.showMessageDialog(parent,
                    "No container is within reach to open.",
                    "Cannot Open", JOptionPane.WARNING_MESSAGE);
            requestFocusInWindow();
            return;
        }

        if (container.isLocked()) {
            model.Key match = engine.getHero().getInventory().findKey(container.getRequiredKeyId());
            if (match == null) {
                JOptionPane.showMessageDialog(parent,
                        container.getName() + " is locked.",
                        "Locked", JOptionPane.WARNING_MESSAGE);
                requestFocusInWindow();
                return;
            }
            int choice = JOptionPane.showConfirmDialog(parent,
                    "Open " + container.getName() + " with " + match.getName() + "?",
                    "Locked Container",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                requestFocusInWindow();
                return;
            }
            LockController.UnlockResult result = engine.tryUnlock(container);
            if (result == LockController.UnlockResult.NO_MATCHING_KEY) {
                JOptionPane.showMessageDialog(parent,
                        container.getName() + " is locked.",
                        "Locked", JOptionPane.WARNING_MESSAGE);
                requestFocusInWindow();
                return;
            }
        }

        ChestDialog dialog = new ChestDialog(parent, engine, container);
        dialog.setVisible(true);
        requestFocusInWindow();
    }

    private void handleTakeKeyPress() {
        if (!engine.takeItemOnGround()) {
            Window parent = SwingUtilities.getWindowAncestor(this);
            String message = engine.getHero().getInventory().isFull()
                    ? getPickupFailureMessage(InventoryController.PickupResult.INVENTORY_FULL)
                    : "No takable item is available on this tile or an adjacent tile.";
            JOptionPane.showMessageDialog(parent, message,
                    "Cannot Take Item", JOptionPane.WARNING_MESSAGE);
        }

        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        heroAnimTimer.stop();
        energyRefillTimer.stop();
        engine.removeGameStateListener(this);
        super.removeNotify();
    }

    @Override
    public void onGameStateChanged() {
        Hero hero = engine.getHero();
        if (hero != null) {
            DungeonMap map = engine.getDungeonMap();
            int tileSize = map != null ? getTileSize(map) : BASE_CELL;
            int dx = hero.getX() - heroLastX;
            int dy = hero.getY() - heroLastY;
            if (heroLastX != Integer.MIN_VALUE
                    && (dx != 0 || dy != 0)
                    && Math.abs(dx) + Math.abs(dy) == 1) {
                if (heroAnimProgress < 1f) {
                    float remaining = 1f - heroAnimProgress;
                    heroPixelOffsetX = -heroAnimDx * remaining * tileSize;
                    heroPixelOffsetY = -heroAnimDy * remaining * tileSize;
                } else {
                    heroPixelOffsetX = 0f;
                    heroPixelOffsetY = 0f;
                }
                heroPixelOffsetX += -dx * tileSize;
                heroPixelOffsetY += -dy * tileSize;
                heroAnimDx = dx;
                heroAnimDy = dy;
                heroAnimProgress = 0f;
                heroFrame = 0;
                if (dx != 0) {
                    heroFacingLeft = dx < 0;
                }
            }
            heroLastX = hero.getX();
            heroLastY = hero.getY();
        }
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
            // Pass 1: floor sprite under every passable cell.
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    GridCell cell = map.getCell(x, y);
                    if (cell == null || !cell.isPassable()) {
                        continue;
                    }
                    int px = offsetX + x * tileSize;
                    int py = offsetY + y * tileSize;
                    ambienceRenderer.drawFloor(g2, px, py, tileSize);
                }
            }

            // Pass 2: walls (renderer owns the layout — multi-cell sprites span
            // their CSV-derived number of cells).
            ambienceRenderer.drawWalls(g2, map, tileSize, offsetX, offsetY);

            // Pass 3: items, entities, and AI labels on top of the ambience.
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

                    if (!cell.getItemsView().isEmpty()) {
                        Item first = cell.getItemsView().get(0);
                        BufferedImage sprite = spriteFor(first);
                        if (sprite != null) {
                            drawItemSprite(g2, first, sprite, px, py, cellW, cellH);
                        } else {
                            Color itemColor = first instanceof Potion p ? p.getColor() : ITEM;
                            drawItemMarker(g2, px, py, cellW, cellH, itemColor);
                        }
                    }

                    for (Entity ent : cell.getEntitiesView()) {
                        if (ent instanceof Hero) {
                            continue;
                        }
                        BufferedImage enemySprite = spriteFor(ent);
                        if (enemySprite != null) {    
                            int spriteW = Math.round(enemySprite.getWidth() * HERO_SPRITE_SCALE);
                            int spriteH = Math.round(enemySprite.getHeight() * HERO_SPRITE_SCALE);    
                            int drawX = px + (cellW - spriteW) / 2;
                            int drawY = py + (cellH - spriteH) / 2;
    
                            g2.drawImage(enemySprite, drawX, drawY, spriteW, spriteH, null);
                        } else {
                            g2.setColor(ent instanceof Knight ? KNIGHT
                                    : ent instanceof Sorcerer ? SORCERER
                                            : Color.LIGHT_GRAY);
                            int inset = Math.max(1, Math.min(cellW, cellH) / 5);
                            int entityW = Math.max(1, cellW - inset * 2);
                            int entityH = Math.max(1, cellH - inset * 2);
                            g2.fillRect(px + inset, py + inset, entityW, entityH);
                        }
                        drawAiStateLabel(g2, ent, px, py, cellW);
                    }
                }
            }
            drawHero(g2, map, tileSize, offsetX, offsetY);
            drawHud(g2);
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

    private BufferedImage spriteFor(Item item) {
        return SpriteRegistry.spriteFor(item);
    }

    private BufferedImage spriteFor(Entity entity) {
        return SpriteRegistry.spriteFor(entity);
    }

    private void drawItemSprite(Graphics2D g2, Item item, BufferedImage sprite, int px, int py, int cellW, int cellH) {
        int inset = Math.max(1, Math.min(cellW, cellH) / 6);
        int boxW = Math.max(1, cellW - inset * 2);
        int boxH = Math.max(1, cellH - inset * 2);
        if (item instanceof model.Key) {
            // Keys are tiny pixel art — draw at native size, centered, never enlarged.
            int drawX = px + (cellW - sprite.getWidth()) / 2;
            int drawY = py + (cellH - sprite.getHeight()) / 2;
            g2.drawImage(sprite, drawX, drawY, sprite.getWidth(), sprite.getHeight(), null);
            return;
        }
        g2.drawImage(sprite, px + inset, py + inset, boxW, boxH, null);
    }

    private void drawItemMarker(Graphics2D g2, int px, int py, int cellW, int cellH, Color color) {
        g2.setColor(color);
        int inset = Math.max(1, Math.min(cellW, cellH) / 3);
        int itemW = Math.max(1, cellW - inset * 2);
        int itemH = Math.max(1, cellH - inset * 2);
        g2.fillRect(px + inset, py + inset, itemW, itemH);
        g2.setColor(GRID_LINE);
        g2.drawRect(px + inset, py + inset, itemW, itemH);
    }

    private void drawHero(Graphics2D g2, DungeonMap map, int tileSize, int offsetX, int offsetY) {
        Hero hero = engine.getHero();
        if (hero == null) {
            return;
        }

        BufferedImage heroSprite = SpriteRegistry.heroFrame(heroFrame);
        if (heroSprite == null) {
            GridCell cell = map.getCell(hero.getX(), hero.getY());
            if (cell == null) {
                return;
            }
            int px = offsetX + hero.getX() * tileSize;
            int py = offsetY + hero.getY() * tileSize;
            int inset = Math.max(1, tileSize / 5);
            int entityW = Math.max(1, tileSize - inset * 2);
            int entityH = Math.max(1, tileSize - inset * 2);
            g2.setColor(HERO);
            g2.fillRect(px + inset, py + inset, entityW, entityH);
            return;
        }

        int spriteW = Math.round(heroSprite.getWidth() * HERO_SPRITE_SCALE);
        int spriteH = Math.round(heroSprite.getHeight() * HERO_SPRITE_SCALE);
        float remaining = 1f - heroAnimProgress;
        float renderGridX = hero.getX() + ((heroPixelOffsetX * remaining) / tileSize);
        float renderGridY = hero.getY() + ((heroPixelOffsetY * remaining) / tileSize);
        int drawX = offsetX + Math.round(renderGridX * tileSize) + (tileSize - spriteW) / 2;
        int drawY = offsetY + Math.round(renderGridY * tileSize) + (tileSize - spriteH) / 2;

        if (heroFacingLeft) {
            g2.drawImage(heroSprite, drawX + spriteW, drawY, -spriteW, spriteH, null);
        } else {
            g2.drawImage(heroSprite, drawX, drawY, spriteW, spriteH, null);
        }
    }

    private void drawHud(Graphics2D g2) {
        Hero hero = engine.getHero();
        if (hero == null) {
            return;
        }
        int x = 10;
        int y = 10;
        int w = 150;
        int h = 102;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(90, 90, 100));
        g2.drawRect(x, y, w, h);

        g2.setColor(HUD_HP);
        g2.fillRect(x + 8, y + 8, 12, 12);
        g2.setColor(HUD_TEXT);
        g2.drawString("HP: " + hero.getHp(), x + 26, y + 19);

        g2.setColor(HUD_ENERGY);
        g2.fillRect(x + 8, y + 26, 12, 12);
        g2.setColor(HUD_TEXT);
        g2.drawString("Energy: " + hero.getEnergy(), x + 26, y + 37);

        g2.setColor(HUD_COINS);
        g2.fillRect(x + 8, y + 44, 12, 12);
        g2.setColor(HUD_TEXT);
        g2.drawString("Coins: " + hero.getCoinBalance(), x + 26, y + 55);

        g2.setColor(HUD_STR);
        g2.fillRect(x + 8, y + 62, 12, 12);
        g2.setColor(HUD_TEXT);
        g2.drawString("STR: " + hero.getStr(), x + 26, y + 73);

        g2.setColor(HUD_DEF);
        g2.fillRect(x + 8, y + 80, 12, 12);
        g2.setColor(HUD_TEXT);
        g2.drawString("DEF: " + hero.getDef(), x + 26, y + 91);
    }

    /**
     * Renders a small Chasing/Roaming tag just above the enemy sprite for debug visibility.
     * Keeps rendering work tight so it stays cheap even with 5 enemies on screen.
     */
    private void drawAiStateLabel(Graphics2D g2, Entity ent, int px, int py, int cellW) {
        String label;
        Color color;
        if (ent instanceof Knight k) {
            label = k.getAiState().name();
            color = k.getAiState() == model.AIState.CHASING ? new Color(255, 90, 90) : new Color(200, 200, 200);
        } else if (ent instanceof Sorcerer s) {
            label = s.getAiState().name();
            color = s.getAiState() == model.AIState.CHASING ? new Color(255, 90, 90) : new Color(200, 200, 200);
        } else {
            return;
        }
        g2.setFont(g2.getFont().deriveFont(10f));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textX = px + (cellW - textW) / 2;
        int textY = py - 2;
        // background for readability
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(textX - 2, textY - fm.getAscent(), textW + 4, fm.getHeight());
        g2.setColor(color);
        g2.drawString(label, textX, textY - 2);
    }

}
