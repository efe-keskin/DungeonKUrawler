package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import engine.GameEngine;
import model.Coin;
import model.Container;
import model.Item;
import view.assets.SpriteRegistry;

/**
 * Drawn 4x4 grid view for a {@link Container}. Click a slot to move the item
 * into the hero's inventory. ESC or the EXIT button closes.
 *
 * <p>High Cohesion: this dialog only renders the container UI and forwards
 * take requests to {@link GameEngine}; access rules live in the model/engine.
 */
public class ChestDialog extends JDialog {

    private static final int SLOT_W = 64;
    private static final int SLOT_H = 64;
    private static final int SLOT_START_X = 42;
    private static final int SLOT_START_Y = 86;
    private static final int SLOT_GAP_X = 12;
    private static final int SLOT_GAP_Y = 12;
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 4;
    private static final int GRID_CAPACITY = GRID_COLS * GRID_ROWS;
    private static final int CANVAS_W = SLOT_START_X * 2 + GRID_COLS * SLOT_W + (GRID_COLS - 1) * SLOT_GAP_X;
    private static final int CANVAS_H = SLOT_START_Y + GRID_ROWS * SLOT_H + (GRID_ROWS - 1) * SLOT_GAP_Y + 70;

    private static final Color FILLED_SLOT_BG = new Color(10, 12, 18, 190);
    private static final Color FILLED_SLOT_BORDER = new Color(220, 225, 245, 150);

    private final GameEngine engine;
    private final Container container;
    private final GameNoticeSink noticeSink;

    public ChestDialog(java.awt.Window owner, GameEngine engine, Container container) {
        this(owner, engine, container, null);
    }

    public ChestDialog(java.awt.Window owner, GameEngine engine, Container container,
            GameNoticeSink noticeSink) {
        super(owner, container.getName(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.engine = engine;
        this.container = container;
        this.noticeSink = noticeSink;
        buildUi();
    }

    private void rebuildUi() {
        getContentPane().removeAll();
        buildUi();
        revalidate();
        repaint();
    }

    private void buildUi() {
        if (!isDisplayable()) {
            setUndecorated(true);
        }

        ChestCanvas canvas = new ChestCanvas();
        canvas.setLayout(null);

        JLabel title = new JLabel(container.getName(), SwingConstants.CENTER);
        title.setForeground(new Color(255, 226, 142));
        title.setFont(RetroTheme.UI_MONO);
        title.setBounds(48, 28, CANVAS_W - 96, 28);
        canvas.add(title);

        List<Item> items = container.getContents();
        for (int i = 0; i < GRID_CAPACITY; i++) {
            Item item = i < items.size() ? items.get(i) : null;
            JPanel slot = createSlot(item);
            slot.setBounds(slotX(i), slotY(i), SLOT_W, SLOT_H);
            canvas.add(slot);
        }

        JLabel hint = new JLabel("ESC", SwingConstants.CENTER);
        hint.setForeground(new Color(230, 230, 240, 170));
        hint.setFont(RetroTheme.UI_MONO_SMALL);
        hint.setBounds(CANVAS_W / 2 - 24, CANVAS_H - 38, 48, 20);
        canvas.add(hint);

        JButton exitButton = new JButton("X");
        exitButton.setFont(RetroTheme.UI_MONO);
        exitButton.setForeground(new Color(245, 245, 255));
        exitButton.setBackground(new Color(120, 30, 30));
        exitButton.setOpaque(true);
        exitButton.setContentAreaFilled(true);
        exitButton.setBorderPainted(true);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 120), 1));
        exitButton.setBounds(CANVAS_W - 40, 10, 28, 28);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> dispose());
        canvas.add(exitButton);

        setContentPane(canvas);
        setResizable(false);
        setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));
        pack();

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setLocationRelativeTo(getOwner());
    }

    private JPanel createSlot(Item item) {
        JPanel slot = new JPanel(null);
        slot.setOpaque(false);

        if (item == null) {
            return slot;
        }

        JPanel overlay = new JPanel(null);
        overlay.setBackground(FILLED_SLOT_BG);
        overlay.setOpaque(true);
        overlay.setBounds(2, 2, SLOT_W - 4, SLOT_H - 4);
        overlay.setBorder(BorderFactory.createLineBorder(FILLED_SLOT_BORDER));

        BufferedImage sprite = SpriteRegistry.spriteFor(item);
        if (sprite != null) {
            int iconW = SLOT_W - 16;
            int iconH = SLOT_H - 16;
            JLabel icon = new JLabel(new ImageIcon(
                    sprite.getScaledInstance(iconW, iconH, java.awt.Image.SCALE_SMOOTH)));
            icon.setBounds(6, 6, iconW, iconH);
            overlay.add(icon);
        } else {
            JLabel marker = new JLabel(item.getName(), SwingConstants.CENTER);
            marker.setForeground(new Color(245, 245, 255));
            marker.setFont(RetroTheme.UI_MONO_SMALL);
            marker.setBounds(2, 2, SLOT_W - 8, SLOT_H - 8);
            overlay.add(marker);
        }

        slot.add(overlay);
        attachTakeHandler(slot, overlay, item);
        return slot;
    }

    private void attachTakeHandler(JPanel slot, JPanel overlay, Item item) {
        slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter takeOnClick = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Taking is a single click — no confirmation prompt (redundant).
                boolean taken = engine.takeFromContainer(container, item);
                if (!taken) {
                    showPassiveNotice(
                            "Warning",
                            item instanceof Coin ? "Cannot Collect Coins" : "Cannot Take Item",
                            item instanceof Coin ? "Coin reward is no longer available." : "Inventory is full.");
                    return;
                }
                rebuildUi();
            }
        };
        slot.addMouseListener(takeOnClick);
        overlay.addMouseListener(takeOnClick);
        for (java.awt.Component child : overlay.getComponents()) {
            if (child instanceof JComponent jc) {
                jc.addMouseListener(takeOnClick);
            }
        }
    }

    private void showPassiveNotice(String category, String title, String message) {
        if (noticeSink != null) {
            noticeSink.showNotice(title, message);
        } else {
            ItemActionMenuDialog.showNotice(this, category, title, message);
        }
    }

    private int slotX(int index) {
        return SLOT_START_X + (index % GRID_COLS) * (SLOT_W + SLOT_GAP_X);
    }

    private int slotY(int index) {
        return SLOT_START_Y + (index / GRID_COLS) * (SLOT_H + SLOT_GAP_Y);
    }

    private static final class ChestCanvas extends JPanel {

        ChestCanvas() {
            setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));
            setBackground(new Color(9, 8, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(new Color(9, 8, 12));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(70, 39, 22));
                g2.fillRect(18, 42, CANVAS_W - 36, CANVAS_H - 72);
                g2.setColor(new Color(125, 75, 34));
                g2.fillRect(24, 48, CANVAS_W - 48, 42);
                g2.setColor(new Color(44, 24, 18));
                g2.fillRect(24, 90, CANVAS_W - 48, CANVAS_H - 124);

                g2.setColor(new Color(211, 156, 57));
                g2.drawRect(18, 42, CANVAS_W - 37, CANVAS_H - 73);
                g2.drawRect(28, 96, CANVAS_W - 57, CANVAS_H - 137);
                g2.fillRect(CANVAS_W / 2 - 16, 78, 32, 20);
                g2.setColor(new Color(85, 53, 26));
                g2.drawRect(CANVAS_W / 2 - 12, 82, 24, 12);

                for (int i = 0; i < GRID_CAPACITY; i++) {
                    int sx = SLOT_START_X + (i % GRID_COLS) * (SLOT_W + SLOT_GAP_X);
                    int sy = SLOT_START_Y + (i / GRID_COLS) * (SLOT_H + SLOT_GAP_Y);
                    g2.setColor(new Color(15, 15, 22));
                    g2.fillRect(sx, sy, SLOT_W, SLOT_H);
                    g2.setColor(new Color(158, 111, 49));
                    g2.drawRect(sx, sy, SLOT_W, SLOT_H);
                    g2.setColor(new Color(0, 0, 0, 95));
                    g2.drawRect(sx + 3, sy + 3, SLOT_W - 6, SLOT_H - 6);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
