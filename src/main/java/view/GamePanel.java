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

    private static final int CELL = 28;

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
                    playerModeController.moveHero(d);
                }
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
        int gridX = e.getX() / CELL;
        int gridY = e.getY() / CELL;

        
        String actionName = interactionController.getPrimaryAction(gridX, gridY);

        // 2. If an action exists, build and show the UI menu
        if (actionName != null) {
            JPopupMenu actionMenu = new JPopupMenu();

            // --- BUTTON 1: The specific action ---
            JMenuItem actionBtn = new JMenuItem(actionName);
            actionBtn.addActionListener(event -> {
                // When clicked, tell the controller to execute it
                interactionController.executeAction(actionName, gridX, gridY);
            });

            // --- BUTTON 2: Return to map ---
            JMenuItem returnBtn = new JMenuItem("Return to map");
            // In Java Swing, clicking any JMenuItem automatically closes the menu, 
            // so we don't even need to add an ActionListener for this one!

            // Add the buttons to the menu
            actionMenu.add(actionBtn);
            actionMenu.add(returnBtn);
            
            // Show the menu at the exact X and Y coordinates of the mouse click
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
        return new Dimension(map.getWidth() * CELL + 1, map.getHeight() * CELL + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            DungeonMap map = engine.getDungeonMap();
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    GridCell cell = map.getCell(x, y);
                    if (cell == null) {
                        continue;
                    }
                    int px = x * CELL;
                    int py = y * CELL;
                    g2.setColor(cell.isPassable() ? FLOOR : WALL);
                    g2.fillRect(px, py, CELL, CELL);
                    g2.setColor(GRID_LINE);
                    g2.drawRect(px, py, CELL, CELL);

                    if (!cell.getItemsView().isEmpty()) {
                        drawItemGold(g2, px, py);
                    }

                    for (Entity ent : cell.getEntitiesView()) {
                        g2.setColor(ent instanceof Hero ? HERO
                                : ent instanceof Knight ? KNIGHT
                                        : ent instanceof Sorcerer ? SORCERER
                                                : Color.LIGHT_GRAY);
                        int inset = 5;
                        g2.fillRect(px + inset, py + inset, CELL - inset * 2, CELL - inset * 2);
                    }
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawItemGold(Graphics2D g2, int px, int py) {
        g2.setColor(ITEM);
        int inset = 9;
        int s = CELL - inset * 2;
        g2.fillRect(px + inset, py + inset, s, s);
        g2.setColor(GRID_LINE);
        g2.drawRect(px + inset, py + inset, s, s);
    }

}
