package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import engine.GameEngine;
import model.Armor;
import model.HealPotion;
import model.Inventory;
import model.Item;
import model.ManaPotion;
import model.Potion;
import model.Weapon;
import javax.swing.ImageIcon;

/**
 * Image-backed modal inventory view with fixed 2x4 slot overlays (8 total).
 */
public class InventoryDialog extends JDialog {

    private static final String BACKGROUND_ASSET = "/Inventory x4.png";

    private static final int SLOT_START_X = 36;
    private static final int SLOT_START_Y = 176;
    private static final int SLOT_W = 54;
    private static final int SLOT_H = 54;
    private static final int SLOT_GAP_X = 11;
    private static final int SLOT_GAP_Y = 12;

    private static final Color FILLED_SLOT_BG = new Color(10, 12, 18, 150);
    private static final Color FILLED_SLOT_BORDER = new Color(220, 225, 245, 120);
    private static final Color BADGE_FG = new Color(245, 245, 255);
    private static final Color NAME_FG = new Color(245, 245, 255);

    private static BufferedImage healPotionImage;
    private static BufferedImage manaPotionImage;
    private static boolean potionImagesLoaded;

    private final GameEngine engine;

    public InventoryDialog(JDialog owner, GameEngine engine) {
        super(owner, "Inventory", Dialog.ModalityType.APPLICATION_MODAL);
        this.engine = engine;
        buildUi();
    }

    public InventoryDialog(java.awt.Frame owner, GameEngine engine) {
        super(owner, "Inventory", true);
        this.engine = engine;
        buildUi();
    }

    private void rebuildUi() {
        getContentPane().removeAll();
        buildUi();
        revalidate();
        repaint();
    }

    private void buildUi() {
        Inventory inventory = engine.getHero().getInventory();
        BufferedImage background = loadBackground();
        if (background == null) {
            JPanel fallback = new JPanel();
            fallback.setBackground(RetroTheme.BG_DUNGEON);
            fallback.setBorder(new EmptyBorder(18, 22, 18, 22));
            JLabel message = new JLabel("Inventory image missing", SwingConstants.CENTER);
            message.setForeground(Color.WHITE);
            message.setFont(RetroTheme.UI_MONO);
            fallback.add(message);
            setContentPane(fallback);
            setSize(320, 160);
            setResizable(false);
            setLocationRelativeTo(getOwner());
            return;
        }

        if (!isDisplayable()) {
            setUndecorated(true);
        }

        InventoryCanvas canvas = new InventoryCanvas(background);
        canvas.setLayout(null);

        List<Item> items = inventory.getItems();
        for (int i = 0; i < 8; i++) {
            JPanel slot = createSlot(i < items.size() ? items.get(i) : null);
            slot.setBounds(slotX(i), slotY(i), SLOT_W, SLOT_H);
            canvas.add(slot);
        }

        JLabel hint = new JLabel("ESC", SwingConstants.CENTER);
        hint.setForeground(new Color(230, 230, 240, 150));
        hint.setFont(RetroTheme.UI_MONO_SMALL);
        hint.setBounds(136, 430, 48, 20);
        canvas.add(hint);

        JButton exitButton = new JButton("EXIT");
        exitButton.setFont(RetroTheme.UI_MONO_SMALL);
        exitButton.setForeground(new Color(245, 245, 255));
        exitButton.setOpaque(false);
        exitButton.setContentAreaFilled(false);
        exitButton.setBorderPainted(false);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(null);
        exitButton.setBounds(background.getWidth() - 72, 10, 56, 22);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> dispose());
        canvas.add(exitButton);

        setContentPane(canvas);
        setResizable(false);
        setPreferredSize(new Dimension(background.getWidth(), background.getHeight()));
        pack();

        // Image-only dialog: allow quick close without adding extra chrome buttons.
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
        overlay.setBorder(javax.swing.BorderFactory.createLineBorder(FILLED_SLOT_BORDER));

        BufferedImage sprite = itemSprite(item);
        if (sprite != null) {
            JLabel icon = new JLabel(new ImageIcon(
                    sprite.getScaledInstance(SLOT_W - 16, SLOT_H - 16, java.awt.Image.SCALE_SMOOTH)));
            icon.setBounds(6, 6, SLOT_W - 16, SLOT_H - 16);
            overlay.add(icon);
            slot.add(overlay);

            if (item instanceof Potion potion) {
                attachDrinkHandler(slot, overlay, icon, potion);
            }
            return slot;
        }

        JLabel marker = new JLabel(typeMarker(item), SwingConstants.CENTER);
        marker.setOpaque(true);
        marker.setBackground(typeColor(item));
        marker.setForeground(BADGE_FG);
        marker.setFont(RetroTheme.UI_MONO_SMALL);
        marker.setBounds(6, 6, SLOT_W - 16, 16);

        JLabel name = new JLabel(item.getName(), SwingConstants.CENTER);
        name.setForeground(NAME_FG);
        name.setFont(RetroTheme.UI_MONO_SMALL);
        name.setBounds(3, 27, SLOT_W - 10, 20);

        overlay.add(marker);
        overlay.add(name);
        slot.add(overlay);

        if (item instanceof Potion potion) {
            attachDrinkHandler(slot, overlay, potion, marker, name);
        }

        return slot;
    }

    private void attachDrinkHandler(JPanel slot, JPanel overlay, Potion potion, JComponent... extras) {
        slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter drinkOnClick = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                        InventoryDialog.this,
                        "Consume " + potion.getName() + "?",
                        "Use Item",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
                engine.consumePotion(potion);
                rebuildUi();
            }
        };
        slot.addMouseListener(drinkOnClick);
        overlay.addMouseListener(drinkOnClick);
        for (JComponent extra : extras) {
            extra.addMouseListener(drinkOnClick);
        }
    }

    private void attachDrinkHandler(JPanel slot, JPanel overlay, JLabel icon, Potion potion) {
        attachDrinkHandler(slot, overlay, potion, icon);
    }

    private BufferedImage itemSprite(Item item) {
        if (item instanceof HealPotion) {
            return potionImage(true);
        }
        if (item instanceof ManaPotion) {
            return potionImage(false);
        }
        return null;
    }

    private static synchronized BufferedImage potionImage(boolean heal) {
        if (!potionImagesLoaded) {
            healPotionImage = loadResource("/items_objects/healpotion.png");
            manaPotionImage = loadResource("/items_objects/manapotion.png");
            potionImagesLoaded = true;
        }
        return heal ? healPotionImage : manaPotionImage;
    }

    private static BufferedImage loadResource(String path) {
        try (InputStream in = InventoryDialog.class.getResourceAsStream(path)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
            // Missing sprite falls back to text badge.
        }
        return null;
    }

    private int slotX(int index) {
        return SLOT_START_X + (index % 4) * (SLOT_W + SLOT_GAP_X);
    }

    private int slotY(int index) {
        return SLOT_START_Y + (index / 4) * (SLOT_H + SLOT_GAP_Y);
    }

    private BufferedImage loadBackground() {
        try (InputStream in = InventoryDialog.class.getResourceAsStream(BACKGROUND_ASSET)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
            // Fallback UI handles missing/unreadable assets.
        }
        return null;
    }

    private String typeMarker(Item item) {
        if (item instanceof Potion) {
            return "POT";
        }
        if (item instanceof Weapon) {
            return "WPN";
        }
        if (item instanceof Armor) {
            return "ARM";
        }
        return "ITM";
    }

    private Color typeColor(Item item) {
        if (item instanceof Potion) {
            return new Color(70, 120, 70);
        }
        if (item instanceof Weapon) {
            return new Color(120, 85, 40);
        }
        if (item instanceof Armor) {
            return new Color(60, 95, 130);
        }
        return new Color(95, 85, 120);
    }

    private static final class InventoryCanvas extends JPanel {
        private final BufferedImage background;

        InventoryCanvas(BufferedImage background) {
            this.background = background;
            setPreferredSize(new Dimension(background.getWidth(), background.getHeight()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }
    }
}
