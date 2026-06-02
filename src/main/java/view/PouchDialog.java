package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
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
import model.Container;
import model.Item;
import view.assets.SpriteRegistry;

/**
 * Pouch view styled to match the hero's normal inventory: the same stone panel
 * and {@link RetroSlotPanel} tiles. The top grid is the pouch's own contents
 * (click to take an item out), the bottom grid is the hero's inventory (click
 * to store an item in), realizing the pouch as expandable in/out storage.
 *
 * <p>High Cohesion: rendering only; transfer rules live in {@link GameEngine}.
 */
public class PouchDialog extends JDialog {

    private static final int CANVAS_W = 430;
    private static final int SLOT_W = 84;
    private static final int SLOT_H = 78;
    private static final int SLOT_GAP_X = 11;
    private static final int SLOT_GAP_Y = 11;
    private static final int SLOT_START_X = 32;
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 2;

    private static final int POUCH_GRID_Y = 118;
    private static final int INV_LABEL_Y = POUCH_GRID_Y + GRID_ROWS * SLOT_H + (GRID_ROWS - 1) * SLOT_GAP_Y + 12;
    private static final int INV_GRID_Y = INV_LABEL_Y + 22;
    private static final int CANVAS_H = INV_GRID_Y + GRID_ROWS * SLOT_H + (GRID_ROWS - 1) * SLOT_GAP_Y + 58;

    private static final Color STONE_OUTLINE = new Color(5, 5, 9);
    private static final Color STONE_BORDER = new Color(103, 91, 75);
    private static final Color STONE_HIGHLIGHT = new Color(156, 131, 85);
    private static final Color PANEL_FILL = new Color(18, 17, 22);
    private static final Color PANEL_INSET = new Color(28, 25, 27);
    private static final Color GOLD = new Color(214, 170, 70);
    private static final Color GOLD_BRIGHT = new Color(244, 205, 103);
    private static final Color TITLE = new Color(240, 222, 180);
    private static final Color DETAIL = new Color(198, 190, 170);

    private final GameEngine engine;
    private final Container container;
    private final GameNoticeSink noticeSink;

    public PouchDialog(java.awt.Window owner, GameEngine engine, Container container,
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
        setBackground(new Color(0, 0, 0, 0));

        PouchCanvas canvas = new PouchCanvas();
        canvas.setLayout(null);

        JLabel category = new JLabel("CONTAINER");
        category.setFont(uiFont(12f));
        category.setForeground(GOLD);
        category.setBounds(32, 32, 235, 20);
        canvas.add(category);

        JLabel title = new JLabel(container.getName().toUpperCase(java.util.Locale.ROOT));
        title.setFont(uiFont(22f));
        title.setForeground(TITLE);
        title.setBounds(32, 55, 300, 32);
        canvas.add(title);

        JLabel counter = new JLabel(container.size() + " / " + container.getCapacity() + " SLOTS",
                SwingConstants.CENTER);
        counter.setFont(uiFont(12f));
        counter.setForeground(GOLD_BRIGHT);
        counter.setBounds(CANVAS_W - 146, 57, 104, 29);
        counter.setBorder(BorderFactory.createLineBorder(STONE_HIGHLIGHT, 1));
        canvas.add(counter);

        canvas.add(sectionLabel("POUCH CONTENTS", 96));
        addGrid(canvas, container.getContents(), POUCH_GRID_Y, true);

        canvas.add(sectionLabel("YOUR ITEMS  (click to store)", INV_LABEL_Y));
        addGrid(canvas, storableInventoryItems(), INV_GRID_Y, false);

        JLabel hint = new JLabel("CLICK TO MOVE ITEMS   |   ESC TO CLOSE", SwingConstants.CENTER);
        hint.setForeground(new Color(159, 147, 125));
        hint.setFont(uiFont(10f));
        hint.setBounds(30, CANVAS_H - 32, CANVAS_W - 60, 20);
        canvas.add(hint);

        JButton exitButton = new JButton("X");
        exitButton.setFont(uiFont(13f));
        exitButton.setForeground(GOLD_BRIGHT);
        exitButton.setBackground(new Color(54, 41, 30));
        exitButton.setOpaque(true);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createLineBorder(GOLD, 1));
        exitButton.setBounds(CANVAS_W - 58, 28, 27, 27);
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

    private JLabel sectionLabel(String text, int y) {
        JLabel label = new JLabel(text);
        label.setFont(uiFont(11f));
        label.setForeground(DETAIL);
        label.setBounds(32, y, CANVAS_W - 64, 18);
        return label;
    }

    /** Hero inventory items eligible to be stored: excludes the open pouch and other containers. */
    private List<Item> storableInventoryItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : engine.getHero().getInventory().getItems()) {
            if (item != container && !(item instanceof Container)) {
                items.add(item);
            }
        }
        return items;
    }

    private void addGrid(JPanel canvas, List<Item> items, int startY, boolean fromPouch) {
        for (int i = 0; i < GRID_COLS * GRID_ROWS; i++) {
            Item item = i < items.size() ? items.get(i) : null;
            JPanel slot = createSlot(item, fromPouch);
            slot.setBounds(SLOT_START_X + (i % GRID_COLS) * (SLOT_W + SLOT_GAP_X),
                    startY + (i / GRID_COLS) * (SLOT_H + SLOT_GAP_Y), SLOT_W, SLOT_H);
            canvas.add(slot);
        }
    }

    private JPanel createSlot(Item item, boolean fromPouch) {
        JPanel slot = new JPanel(null);
        slot.setOpaque(false);

        RetroSlotPanel overlay = new RetroSlotPanel(item != null, false);
        overlay.setBounds(0, 0, SLOT_W, SLOT_H);
        slot.add(overlay);
        if (item == null) {
            return slot;
        }

        BufferedImage sprite = SpriteRegistry.spriteFor(item);
        if (sprite != null) {
            JLabel icon = new JLabel(new ImageIcon(
                    sprite.getScaledInstance(44, 44, java.awt.Image.SCALE_REPLICATE)));
            icon.setBounds(20, 8, 44, 44);
            overlay.add(icon);
        } else {
            JLabel name = new JLabel(item.getName(), SwingConstants.CENTER);
            name.setForeground(TITLE);
            name.setFont(uiFont(9f));
            name.setBounds(6, 17, SLOT_W - 12, 26);
            overlay.add(name);
        }

        JLabel hintLabel = new JLabel(fromPouch ? "TAKE" : "STORE", SwingConstants.CENTER);
        hintLabel.setOpaque(true);
        hintLabel.setBackground(new Color(40, 32, 22));
        hintLabel.setForeground(GOLD_BRIGHT);
        hintLabel.setFont(uiFont(9f));
        hintLabel.setBounds(8, SLOT_H - 21, SLOT_W - 16, 14);
        overlay.add(hintLabel);

        attachTransferHandler(slot, overlay, item, fromPouch);
        return slot;
    }

    private void attachTransferHandler(JPanel slot, RetroSlotPanel overlay, Item item, boolean fromPouch) {
        slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        slot.setToolTipText((fromPouch ? "Take out: " : "Store: ") + item.getName());
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                transfer(item, fromPouch);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                overlay.setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                overlay.setHovered(false);
            }
        };
        slot.addMouseListener(handler);
        overlay.addMouseListener(handler);
        for (java.awt.Component child : overlay.getComponents()) {
            if (child instanceof JComponent jc) {
                jc.addMouseListener(handler);
            }
        }
    }

    private void transfer(Item item, boolean fromPouch) {
        boolean ok = fromPouch
                ? engine.takeFromContainer(container, item)
                : engine.storeInContainer(container, item);
        if (!ok) {
            notify(fromPouch ? "Cannot Take Item" : "Cannot Store Item",
                    fromPouch ? "Your inventory is full." : "The pouch is full.");
            return;
        }
        rebuildUi();
    }

    private void notify(String title, String message) {
        if (noticeSink != null) {
            noticeSink.showNotice(title, message);
        } else {
            ItemActionMenuDialog.showNotice(this, "Pouch", title, message);
        }
    }

    private static Font uiFont(float size) {
        Font base = RetroTheme.UI_MONO != null ? RetroTheme.UI_MONO : new Font(Font.MONOSPACED, Font.PLAIN, 12);
        return base.deriveFont(Font.PLAIN, size);
    }

    /** Stone panel background matching the inventory dialog's frame. */
    private static final class PouchCanvas extends JPanel {
        PouchCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(7, 8, w - 10, h - 11);
                g2.setColor(STONE_OUTLINE);
                g2.fillRect(1, 1, w - 8, h - 8);
                g2.setColor(STONE_BORDER);
                g2.fillRect(4, 4, w - 14, h - 14);
                g2.setColor(STONE_HIGHLIGHT);
                g2.fillRect(7, 7, w - 20, 2);
                g2.fillRect(7, 7, 2, h - 20);
                g2.setColor(new Color(55, 47, 42));
                g2.fillRect(7, h - 15, w - 20, 2);
                g2.fillRect(w - 15, 7, 2, h - 20);
                g2.setColor(PANEL_FILL);
                g2.fillRect(11, 11, w - 28, h - 28);
                g2.setColor(PANEL_INSET);
                g2.fillRect(17, 17, w - 40, h - 40);
                g2.setColor(GOLD);
                g2.fillRect(32, 90, w - 64, 2);
                g2.fillRect(32, INV_LABEL_Y - 8, w - 64, 2);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
