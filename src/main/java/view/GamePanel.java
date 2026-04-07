package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import engine.Direction;
import engine.GameEngine;
import engine.GameStateListener;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Item;
import model.Knight;
import model.Sorcerer;

/**
 * Observer: implements {@link GameStateListener} and repaints when the engine notifies — no direct
 * model mutation. Input is forwarded to {@link GameEngine#moveHero(Direction)} only; movement rules
 * stay in the controller.
 */
public class GamePanel extends JPanel implements GameStateListener {

    private static final int CELL = 28;

    private static final Color FLOOR = new Color(32, 36, 48);
    private static final Color WALL = new Color(48, 48, 58);
    /** Visible grid lines (retro tile border). */
    private static final Color GRID_LINE = new Color(55, 55, 62);
    private static final Color HERO = new Color(50, 130, 255);
    private static final Color KNIGHT = new Color(220, 55, 55);
    private static final Color SORCERER = new Color(160, 70, 220);
    private static final Color ITEM = new Color(255, 200, 40);

    private final GameEngine engine;

    public GamePanel(GameEngine engine) {
        this.engine = engine;
        engine.addGameStateListener(this);
        setBackground(Color.BLACK);
        setOpaque(true);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Direction d = Direction.fromKeyCode(e.getKeyCode());
                if (d != null) {
                    engine.moveHero(d);
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

                    for (Item ignored : cell.getItemsView()) {
                        drawItemGold(g2, px, py);
                    }

                    for (Entity ent : cell.getEntitiesView()) {
                        Color c;
                        if (ent instanceof Hero) {
                            c = HERO;
                        } else if (ent instanceof Knight) {
                            c = KNIGHT;
                        } else if (ent instanceof Sorcerer) {
                            c = SORCERER;
                        } else {
                            c = Color.LIGHT_GRAY;
                        }
                        g2.setColor(c);
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
