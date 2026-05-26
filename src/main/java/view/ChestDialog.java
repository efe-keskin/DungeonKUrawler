package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import engine.GameEngine;
import model.Coin;
import model.Container;
import model.Item;
import view.assets.AssetId;
import view.assets.AssetManager;
import view.assets.SpriteRegistry;

/**
 * Image-backed 4x4 grid view for a {@link Container}. Click a slot to move
 * the item into the hero's inventory. ESC or the EXIT button closes.
 *
 * <p>High Cohesion: this dialog only renders the container UI and forwards
 * take requests to {@link GameEngine}; access rules live in the model/engine.
 */
public class ChestDialog extends JDialog {

    /** Background image is 590x648; everything is scaled by this to fit a typical window. */
    private static final double SCALE = 0.7d;

    private static final int RAW_SLOT_W = 90;
    private static final int RAW_SLOT_H = 90;
    private static final int RAW_SLOT_START_X = 95;
    private static final int RAW_SLOT_START_Y = 75;
    private static final int RAW_SLOT_GAP_X = 13;
    private static final int RAW_SLOT_GAP_Y = 13;

    private static final int SLOT_W = scale(RAW_SLOT_W);
    private static final int SLOT_H = scale(RAW_SLOT_H);
    private static final int SLOT_START_X = scale(RAW_SLOT_START_X);
    private static final int SLOT_START_Y = scale(RAW_SLOT_START_Y);
    private static final int SLOT_GAP_X = scale(RAW_SLOT_GAP_X);
    private static final int SLOT_GAP_Y = scale(RAW_SLOT_GAP_Y);

    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 4;
    private static final int GRID_CAPACITY = GRID_COLS * GRID_ROWS;

    private static final Color FILLED_SLOT_BG = new Color(10, 12, 18, 130);
    private static final Color FILLED_SLOT_BORDER = new Color(220, 225, 245, 120);

    private final GameEngine engine;
    private final Container container;

    public ChestDialog(java.awt.Window owner, GameEngine engine, Container container) {
        super(owner, container.getName(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.engine = engine;
        this.container = container;
        buildUi();
    }

    private static int scale(int v) {
        return (int) Math.round(v * SCALE);
    }

    private void rebuildUi() {
        getContentPane().removeAll();
        buildUi();
        revalidate();
        repaint();
    }

    private void buildUi() {
        BufferedImage background = AssetManager.get().image(AssetId.CHEST_BACKGROUND);
        if (background == null) {
            buildFallbackUi();
            return;
        }

        int bgW = scale(background.getWidth());
        int bgH = scale(background.getHeight());

        if (!isDisplayable()) {
            setUndecorated(true);
        }

        ChestCanvas canvas = new ChestCanvas(background, bgW, bgH);
        canvas.setLayout(null);

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
        hint.setBounds(bgW / 2 - 24, bgH - 36, 48, 20);
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
        exitButton.setBounds(bgW - 40, 10, 28, 28);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> dispose());
        canvas.add(exitButton);

        setContentPane(canvas);
        setResizable(false);
        setPreferredSize(new Dimension(bgW, bgH));
        pack();

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setLocationRelativeTo(getOwner());
    }

    private void buildFallbackUi() {
        JPanel fallback = new JPanel();
        fallback.setBackground(RetroTheme.BG_DUNGEON);
        fallback.setBorder(new EmptyBorder(18, 22, 18, 22));
        JLabel message = new JLabel("Chest background missing", SwingConstants.CENTER);
        message.setForeground(Color.WHITE);
        message.setFont(RetroTheme.UI_MONO);
        fallback.add(message);
        setContentPane(fallback);
        setSize(320, 160);
        setResizable(false);
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
                String action = item instanceof Coin ? "Collect " : "Take ";
                int choice = JOptionPane.showConfirmDialog(
                        ChestDialog.this,
                        action + item.getName() + "?",
                        item instanceof Coin ? "Collect Coins" : "Take Item",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
                boolean taken = engine.takeFromContainer(container, item);
                if (!taken) {
                    JOptionPane.showMessageDialog(
                            ChestDialog.this,
                            item instanceof Coin ? "Coin reward is no longer available." : "Inventory is full.",
                            item instanceof Coin ? "Cannot Collect Coins" : "Cannot Take Item",
                            JOptionPane.WARNING_MESSAGE);
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

    private int slotX(int index) {
        return SLOT_START_X + (index % GRID_COLS) * (SLOT_W + SLOT_GAP_X);
    }

    private int slotY(int index) {
        return SLOT_START_Y + (index / GRID_COLS) * (SLOT_H + SLOT_GAP_Y);
    }

    private static final class ChestCanvas extends JPanel {
        private final BufferedImage background;

        ChestCanvas(BufferedImage background, int width, int height) {
            this.background = background;
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }
    }
}
