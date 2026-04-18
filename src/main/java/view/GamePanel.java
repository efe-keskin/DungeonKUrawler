package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import engine.Direction;
import engine.GameEngine;
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

                int panelW = Math.max(1, GamePanel.this.getWidth());
                int panelH = Math.max(1, GamePanel.this.getHeight());

                int gridX = Math.min(map.getWidth() - 1, Math.max(0, e.getX() * map.getWidth() / panelW));
                int gridY = Math.min(map.getHeight() - 1, Math.max(0, e.getY() * map.getHeight() / panelH));

                String actionName = GamePanel.this.interactionController.getPrimaryAction(gridX, gridY);

                if (actionName != null) {
                    JPopupMenu actionMenu = new JPopupMenu();

                    JMenuItem actionBtn = new JMenuItem(actionName);
                    actionBtn.addActionListener(event -> GamePanel.this.interactionController.executeAction(actionName,
                            gridX, gridY));

                    JMenuItem returnBtn = new JMenuItem("Return to map");

                    actionMenu.add(actionBtn);
                    actionMenu.add(returnBtn);

                    actionMenu.show(GamePanel.this, e.getX(), e.getY());
                }
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
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    GridCell cell = map.getCell(x, y);
                    if (cell == null) {
                        continue;
                    }
                    int px = x * getWidth() / mapW;
                    int py = y * getHeight() / mapH;
                    int px2 = (x + 1) * getWidth() / mapW;
                    int py2 = (y + 1) * getHeight() / mapH;
                    int cellW = Math.max(1, px2 - px);
                    int cellH = Math.max(1, py2 - py);
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
