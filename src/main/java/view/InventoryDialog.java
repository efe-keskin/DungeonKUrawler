package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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
import model.Book;
import model.Inventory;
import model.Item;
import model.ItemAction;
import model.Key;
import model.Potion;
import model.Ring;
import model.ValuableItem;
import model.Weapon;
import view.assets.AssetId;
import view.assets.AssetManager;
import view.assets.SpriteRegistry;
import javax.swing.ImageIcon;

/**
 * Image-backed modal inventory view with fixed 2x4 slot overlays (8 total).
 */
public class InventoryDialog extends JDialog {

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

            attachActionHandler(slot, overlay, item, icon);
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

        attachActionHandler(slot, overlay, item, marker, name);

        return slot;
    }

    private void attachActionHandler(JPanel slot, JPanel overlay, Item item, JComponent... extras) {
        slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter actionOnClick = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showItemActions(item);
            }
        };
        slot.addMouseListener(actionOnClick);
        overlay.addMouseListener(actionOnClick);
        for (JComponent extra : extras) {
            extra.addMouseListener(actionOnClick);
        }
    }

    private void showItemActions(Item item) {
        List<ItemAction> actions = new ArrayList<>(item.getInventoryActions());
        if (engine.getHero().isEquipped(item)) {
            for (int i = 0; i < actions.size(); i++) {
                ItemAction action = actions.get(i);
                if (action == ItemAction.WEAR || action == ItemAction.EQUIP) {
                    actions.set(i, ItemAction.REMOVE);
                    break;
                }
            }
        }

        Object[] options = actions.stream().map(ItemAction::getLabel).toArray();
        int choice = JOptionPane.showOptionDialog(
                this,
                itemDescription(item),
                "Use " + item.getName(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        if (choice < 0) {
            return;
        }

        ItemAction action = actions.get(choice);
        if (!engine.performInventoryAction(item, action)) {
            JOptionPane.showMessageDialog(this, "That action is no longer available.",
                    "Cannot Use Item", JOptionPane.WARNING_MESSAGE);
            rebuildUi();
            return;
        }

        if (action == ItemAction.READ && item instanceof Book book) {
            JOptionPane.showMessageDialog(this, book.read(), item.getName(), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (action == ItemAction.WEAR || action == ItemAction.EQUIP || action == ItemAction.REMOVE) {
            JOptionPane.showMessageDialog(this,
                    "STR: " + engine.getHero().getStr() + "    DEF: " + engine.getHero().getDef(),
                    "Equipment Updated",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        rebuildUi();
    }

    private String itemDescription(Item item) {
        String equipped = engine.getHero().isEquipped(item) ? " (Equipped)" : "";
        if (item instanceof Ring ring) {
            return item.getName() + equipped + "\nProtective ring: +" + ring.getDefBonus() + " DEF";
        }
        if (item instanceof Armor armor) {
            return item.getName() + equipped + "\nArmor: +" + armor.getDefModifier() + " DEF";
        }
        if (item instanceof Weapon weapon) {
            return item.getName() + equipped + "\nWeapon: +" + weapon.getAtkValue() + " ATK";
        }
        if (item instanceof Potion) {
            return item.getName() + "\nConsume this potion or discard it.";
        }
        if (item instanceof Key) {
            return item.getName() + "\nUsed automatically when opening a matching locked chest.";
        }
        if (item instanceof Book) {
            return item.getName() + "\nA readable object.";
        }
        return item.getName() + "\nA collectible valuable object.";
    }

    private BufferedImage itemSprite(Item item) {
        return SpriteRegistry.spriteFor(item);
    }

    private int slotX(int index) {
        return SLOT_START_X + (index % 4) * (SLOT_W + SLOT_GAP_X);
    }

    private int slotY(int index) {
        return SLOT_START_Y + (index / 4) * (SLOT_H + SLOT_GAP_Y);
    }

    private BufferedImage loadBackground() {
        return AssetManager.get().image(AssetId.INVENTORY_BACKGROUND);
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
        if (item instanceof Ring) {
            return "RNG";
        }
        if (item instanceof Book) {
            return "BOK";
        }
        if (item instanceof Key) {
            return "KEY";
        }
        if (item instanceof ValuableItem) {
            return "VAL";
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
        if (item instanceof Ring) {
            return new Color(130, 95, 35);
        }
        if (item instanceof Book) {
            return new Color(120, 45, 35);
        }
        if (item instanceof Key) {
            return new Color(145, 120, 40);
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
