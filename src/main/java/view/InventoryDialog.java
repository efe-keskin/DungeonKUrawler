package view;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import model.Armor;
import model.Inventory;
import model.Item;
import model.Potion;
import model.Weapon;

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

    public InventoryDialog(JDialog owner, Inventory inventory) {
        super(owner, "Inventory", Dialog.ModalityType.APPLICATION_MODAL);
        buildUi(inventory);
    }

    public InventoryDialog(java.awt.Frame owner, Inventory inventory) {
        super(owner, "Inventory", true);
        buildUi(inventory);
    }

    private void buildUi(Inventory inventory) {
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

        setUndecorated(true);

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
        return slot;
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
