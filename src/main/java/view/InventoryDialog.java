package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import engine.audio.AudioManager;
import engine.GameEngine;
import model.Armor;
import model.Book;
import model.Inventory;
import model.Item;
import model.ItemAction;
import model.Key;
import model.Pet;
import model.Potion;
import model.Readable;
import model.Ring;
import model.Scroll;
import model.Torch;
import model.ValuableItem;
import model.Weapon;
import view.assets.SpriteRegistry;

/**
 * Retro-styled modal inventory view with a fixed 2x4 slot grid.
 */
public class InventoryDialog extends JDialog {

    private static final int CANVAS_W = 430;
    private static final int CANVAS_H = 476;
    private static final int SLOT_START_X = 32;
    private static final int SLOT_START_Y = 139;
    private static final int SLOT_W = 84;
    private static final int SLOT_H = 78;
    private static final int SLOT_GAP_X = 11;
    private static final int SLOT_GAP_Y = 11;
    private static final int PET_SLOT_Y = 360;
    private static final int PET_SLOT_H = 70;

    private static final Color STONE_OUTLINE = new Color(5, 5, 9);
    private static final Color STONE_BORDER = new Color(103, 91, 75);
    private static final Color STONE_HIGHLIGHT = new Color(156, 131, 85);
    private static final Color PANEL_FILL = new Color(18, 17, 22);
    private static final Color PANEL_INSET = new Color(28, 25, 27);
    private static final Color GOLD = new Color(214, 170, 70);
    private static final Color GOLD_BRIGHT = new Color(244, 205, 103);
    private static final Color TITLE = new Color(240, 222, 180);
    private static final Color DETAIL = new Color(198, 190, 170);
    private static final Color SLOT_EMPTY = new Color(15, 14, 18);
    private static final Color SLOT_FILLED = new Color(31, 28, 28);
    private static final Color SLOT_HOVER = new Color(53, 43, 31);
    private static final Color BADGE_FG = new Color(245, 228, 188);

    private final GameEngine engine;
    private int torchFrame = 0;
    private Timer torchAnimationTimer;

    public InventoryDialog(JDialog owner, GameEngine engine) {
        super(owner, "Inventory", Dialog.ModalityType.APPLICATION_MODAL);
        this.engine = engine;
        buildUi();
        startTorchAnimationTimer();
    }

    public InventoryDialog(java.awt.Frame owner, GameEngine engine) {
        super(owner, "Inventory", true);
        this.engine = engine;
        buildUi();
        startTorchAnimationTimer();
    }

    private void startTorchAnimationTimer() {
        torchAnimationTimer = new Timer(150, e -> {
            torchFrame = (torchFrame + 1) % SpriteRegistry.torchFrameCount();
            repaint();
        });
        torchAnimationTimer.start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopTorchAnimationTimer();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopTorchAnimationTimer();
            }
        });
    }

    private void stopTorchAnimationTimer() {
        if (torchAnimationTimer != null) {
            torchAnimationTimer.stop();
        }
    }

    private void rebuildUi() {
        getContentPane().removeAll();
        buildUi();
        revalidate();
        repaint();
    }

    private void buildUi() {
        Inventory inventory = engine.getHero().getInventory();
        if (!isDisplayable()) {
            setUndecorated(true);
        }
        setBackground(new Color(0, 0, 0, 0));

        InventoryCanvas canvas = new InventoryCanvas();
        canvas.setLayout(null);

        JLabel category = new JLabel("ADVENTURER'S PACK");
        category.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 12f));
        category.setForeground(GOLD);
        category.setBounds(32, 32, 235, 20);
        canvas.add(category);

        JLabel title = new JLabel("INVENTORY");
        title.setFont(uiFont(RetroTheme.UI_MONO, 26f));
        title.setForeground(TITLE);
        title.setBounds(32, 57, 235, 34);
        canvas.add(title);

        JLabel counter = new JLabel(inventory.size() + " / " + inventory.getCapacity() + " SLOTS", SwingConstants.CENTER);
        counter.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 12f));
        counter.setForeground(GOLD_BRIGHT);
        counter.setBounds(284, 59, 104, 29);
        counter.setBorder(BorderFactory.createLineBorder(STONE_HIGHLIGHT, 1));
        canvas.add(counter);

        JLabel section = new JLabel("ITEM SLOTS");
        section.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 11f));
        section.setForeground(DETAIL);
        section.setBounds(32, 108, 120, 18);
        canvas.add(section);

        List<Item> items = inventory.getItems();
        for (int i = 0; i < 8; i++) {
            JPanel slot = createSlot(i < items.size() ? items.get(i) : null);
            slot.setBounds(slotX(i), slotY(i), SLOT_W, SLOT_H);
            canvas.add(slot);
        }

        JLabel companions = new JLabel("COMPANIONS");
        companions.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 11f));
        companions.setForeground(DETAIL);
        companions.setBounds(32, 333, 200, 18);
        canvas.add(companions);

        List<Pet> pets = ownedPets();
        if (pets.isEmpty()) {
            JLabel none = new JLabel("No pets owned - buy one at the shop.");
            none.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 10f));
            none.setForeground(new Color(140, 130, 110));
            none.setBounds(32, PET_SLOT_Y + 22, CANVAS_W - 64, 18);
            canvas.add(none);
        } else {
            for (int i = 0; i < pets.size() && i < 4; i++) {
                JPanel slot = createPetSlot(pets.get(i));
                slot.setBounds(slotX(i), PET_SLOT_Y, SLOT_W, PET_SLOT_H);
                canvas.add(slot);
            }
        }

        JLabel hint = new JLabel("CLICK AN ITEM TO USE   |   ESC TO CLOSE", SwingConstants.CENTER);
        hint.setForeground(new Color(159, 147, 125));
        hint.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 10f));
        hint.setBounds(30, 444, CANVAS_W - 60, 20);
        canvas.add(hint);

        JButton exitButton = new JButton("X");
        exitButton.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 13f));
        exitButton.setForeground(GOLD_BRIGHT);
        exitButton.setBackground(new Color(54, 41, 30));
        exitButton.setOpaque(true);
        exitButton.setContentAreaFilled(true);
        exitButton.setBorderPainted(true);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createLineBorder(GOLD, 1));
        exitButton.setBounds(CANVAS_W - 58, 28, 27, 27);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            dispose();
        });
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

        RetroSlotPanel overlay = new RetroSlotPanel(item != null, item != null && engine.getHero().isEquipped(item));
        overlay.setBounds(0, 0, SLOT_W, SLOT_H);
        slot.add(overlay);

        if (item == null) {
            return slot;
        }

        boolean customWand = isMagicWand(item);
        boolean customBow = isWoodenBow(item);
        BufferedImage sprite = customWand || customBow ? null : itemSprite(item);
        JLabel marker = new JLabel(typeMarker(item), SwingConstants.CENTER);
        marker.setOpaque(true);
        marker.setBackground(typeColor(item));
        marker.setForeground(BADGE_FG);
        marker.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 9f));
        marker.setBounds(8, SLOT_H - 21, 34, 14);
        overlay.add(marker);

        JLabel equipped = null;
        if (engine.getHero().isEquipped(item)) {
            equipped = new JLabel("ON", SwingConstants.CENTER);
            equipped.setOpaque(true);
            equipped.setBackground(new Color(104, 72, 29));
            equipped.setForeground(GOLD_BRIGHT);
            equipped.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 9f));
            equipped.setBounds(SLOT_W - 35, SLOT_H - 21, 27, 14);
            overlay.add(equipped);
        }

        if (customWand) {
            JComponent icon = new WandIconPanel();
            icon.setBounds(20, 8, 44, 44);
            overlay.add(icon);
            if (equipped == null) {
                attachActionHandler(slot, overlay, item, icon, marker);
            } else {
                attachActionHandler(slot, overlay, item, icon, marker, equipped);
            }
            return slot;
        }

        if (customBow) {
            JComponent icon = new BowIconPanel();
            icon.setBounds(20, 8, 44, 44);
            overlay.add(icon);
            if (equipped == null) {
                attachActionHandler(slot, overlay, item, icon, marker);
            } else {
                attachActionHandler(slot, overlay, item, icon, marker, equipped);
            }
            return slot;
        }

        if (sprite != null) {
            JLabel icon = new JLabel(new ImageIcon(
                    sprite.getScaledInstance(44, 44, java.awt.Image.SCALE_REPLICATE)));
            icon.setBounds(20, 8, 44, 44);
            overlay.add(icon);
            if (equipped == null) {
                attachActionHandler(slot, overlay, item, icon, marker);
            } else {
                attachActionHandler(slot, overlay, item, icon, marker, equipped);
            }
            return slot;
        }

        JLabel name = new JLabel(item.getName(), SwingConstants.CENTER);
        name.setForeground(TITLE);
        name.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 9f));
        name.setBounds(6, 17, SLOT_W - 12, 26);
        overlay.add(name);

        if (equipped == null) {
            attachActionHandler(slot, overlay, item, marker, name);
        } else {
            attachActionHandler(slot, overlay, item, marker, name, equipped);
        }

        return slot;
    }

    private static boolean isMagicWand(Item item) {
        if (!(item instanceof Weapon weapon)) {
            return false;
        }
        return "B23_WAND".equals(weapon.getType().id())
                || "staves".equals(weapon.getType().category())
                || weapon.getName().toLowerCase(java.util.Locale.ROOT).contains("wand");
    }

    private static boolean isWoodenBow(Item item) {
        if (!(item instanceof Weapon weapon)) {
            return false;
        }
        return "B23_BOW".equals(weapon.getType().id())
                || "bows".equals(weapon.getType().category())
                || weapon.getName().toLowerCase(java.util.Locale.ROOT).contains("bow");
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

        if (overlay instanceof RetroSlotPanel retroSlot) {
            MouseAdapter hover = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    retroSlot.setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    retroSlot.setHovered(false);
                }
            };
            overlay.addMouseListener(hover);
            for (JComponent extra : extras) {
                extra.addMouseListener(hover);
            }
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

        String[] options = new String[actions.size() + 1];
        for (int i = 0; i < actions.size(); i++) {
            options[i] = actions.get(i).getLabel();
        }
        options[options.length - 1] = "Cancel";

        int choice = ItemActionMenuDialog.show(
                this,
                "Inventory Object",
                item.getName(),
                itemDescription(item),
                options);
        if (choice < 0 || choice == options.length - 1) {
            return;
        }

        ItemAction action = actions.get(choice);
        if (!engine.performInventoryAction(item, action)) {
            ItemActionMenuDialog.showNotice(this, "Warning", "Cannot Use Item",
                    "That action is no longer available.");
            rebuildUi();
            return;
        }

        if (action == ItemAction.READ && item instanceof Readable readable) {
            ItemActionMenuDialog.showNotice(this, "Readable Object", item.getName(), readable.read());
        }

        if (action == ItemAction.WEAR || action == ItemAction.EQUIP || action == ItemAction.REMOVE) {
            ItemActionMenuDialog.showNotice(this, "Equipment", "Equipment Updated",
                    "STR: " + engine.getHero().getStr()
                            + "    DEF: " + engine.getHero().getDef()
                            + "\nMANA: " + engine.getHero().getMana() + "/" + engine.getHero().getMaxMana()
                            + "    ENERGY: " + engine.getHero().getEnergy() + "/"
                            + engine.getHero().getMaxEnergy());
        }
        rebuildUi();
    }

    private List<Pet> ownedPets() {
        return engine.ownedPets();
    }

    private JPanel createPetSlot(Pet pet) {
        JPanel slot = new JPanel(null);
        slot.setOpaque(false);
        boolean equipped = engine.getHero().getEquippedPet() == pet;

        RetroSlotPanel overlay = new RetroSlotPanel(true, equipped);
        overlay.setBounds(0, 0, SLOT_W, PET_SLOT_H);
        slot.add(overlay);

        List<JComponent> extras = new ArrayList<>();
        BufferedImage sprite = itemSprite(pet);
        if (sprite != null) {
            JLabel icon = new JLabel(new ImageIcon(
                    sprite.getScaledInstance(40, 40, java.awt.Image.SCALE_REPLICATE)));
            icon.setBounds(22, 4, 40, 40);
            overlay.add(icon);
            extras.add(icon);
        }

        JLabel hp = new JLabel("HP " + pet.getHp() + "/" + pet.getMaxHp(), SwingConstants.CENTER);
        hp.setForeground(BADGE_FG);
        hp.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 9f));
        hp.setBounds(4, PET_SLOT_H - 18, SLOT_W - 8, 14);
        overlay.add(hp);
        extras.add(hp);

        if (equipped) {
            JLabel badge = new JLabel("ON", SwingConstants.CENTER);
            badge.setOpaque(true);
            badge.setBackground(new Color(104, 72, 29));
            badge.setForeground(GOLD_BRIGHT);
            badge.setFont(uiFont(RetroTheme.UI_MONO_SMALL, 9f));
            badge.setBounds(SLOT_W - 32, 4, 26, 14);
            overlay.add(badge);
            extras.add(badge);
        }

        attachPetHandler(slot, overlay, pet, extras.toArray(new JComponent[0]));
        return slot;
    }

    private void attachPetHandler(JPanel slot, RetroSlotPanel overlay, Pet pet, JComponent... extras) {
        slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        overlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter click = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPetActions(pet);
            }
        };
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                overlay.setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                overlay.setHovered(false);
            }
        };
        slot.addMouseListener(click);
        overlay.addMouseListener(click);
        overlay.addMouseListener(hover);
        for (JComponent extra : extras) {
            extra.addMouseListener(click);
            extra.addMouseListener(hover);
        }
    }

    private void showPetActions(Pet pet) {
        boolean equipped = engine.getHero().getEquippedPet() == pet;
        String primary = equipped ? "Unequip" : "Equip";
        String detail = "HP " + pet.getHp() + "/" + pet.getMaxHp()
                + (equipped ? "\nActive companion." : "\nMake this your active companion.");
        int choice = ItemActionMenuDialog.show(this, "Companion", pet.getName(), detail, primary, "Cancel");
        if (choice != 0) {
            return;
        }
        if (equipped) {
            engine.unequipPet();
        } else {
            engine.equipPet(pet);
        }
        rebuildUi();
    }

    private String itemDescription(Item item) {
        String equipped = engine.getHero().isEquipped(item) ? "Equipped\n" : "";
        if (item instanceof Ring ring) {
            return equipped + ring.effectDescription();
        }
        if (item instanceof Armor armor) {
            return equipped + "Armor: +" + armor.getDefModifier() + " DEF";
        }
        if (item instanceof Weapon weapon) {
            return equipped + "Weapon: +" + weapon.getAtkValue() + " ATK";
        }
        if (item instanceof Potion) {
            return "Consume this potion or discard it.";
        }
        if (item instanceof Key) {
            return "Used automatically when opening a matching locked chest.";
        }
        if (item instanceof Readable) {
            return "A readable object.";
        }
        return "A collectible valuable object.";
    }

    private BufferedImage itemSprite(Item item) {
        if (item instanceof Torch) {
            return SpriteRegistry.torchFrame(torchFrame);
        }
        return SpriteRegistry.spriteFor(item);
    }

    private int slotX(int index) {
        return SLOT_START_X + (index % 4) * (SLOT_W + SLOT_GAP_X);
    }

    private int slotY(int index) {
        return SLOT_START_Y + (index / 4) * (SLOT_H + SLOT_GAP_Y);
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
        if (item instanceof Scroll) {
            return "SCR";
        }
        if (item instanceof Book) {
            return "BK";
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
        if (item instanceof Scroll) {
            return new Color(150, 120, 70);
        }
        if (item instanceof Book) {
            return new Color(120, 45, 35);
        }
        if (item instanceof Key) {
            return new Color(145, 120, 40);
        }
        return new Color(95, 85, 120);
    }

    private static Font uiFont(Font configured, float size) {
        Font base = configured == null ? new Font(Font.MONOSPACED, Font.BOLD, Math.round(size)) : configured;
        return base.deriveFont(Font.PLAIN, size);
    }

    private static final class InventoryCanvas extends JPanel {
        InventoryCanvas() {
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
                g2.fillRect(32, 96, w - 64, 2);
                g2.fillRect(32, 320, w - 64, 2);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class WandIconPanel extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int pixel = Math.max(2, getWidth() / 18);
                int baseX = getWidth() / 2 - pixel;
                int baseY = getHeight() - pixel * 6;
                int tipX = getWidth() / 2 + pixel * 3;
                int tipY = pixel * 5;

                g2.setColor(new Color(45, 27, 18));
                drawPixelLine(g2, baseX - pixel, baseY + pixel, tipX - pixel, tipY + pixel, pixel);
                g2.setColor(new Color(118, 73, 43));
                drawPixelLine(g2, baseX, baseY, tipX, tipY, pixel);
                g2.setColor(new Color(195, 151, 90));
                g2.fillRect(baseX - pixel, baseY + pixel * 2, pixel * 4, pixel);

                g2.setColor(new Color(170, 230, 255));
                g2.fillRect(tipX + pixel, tipY - pixel, pixel, pixel);
                g2.fillRect(tipX + pixel * 3, tipY - pixel * 2, pixel, pixel);
                g2.setColor(Color.WHITE);
                g2.fillRect(tipX + pixel * 2, tipY - pixel, pixel, pixel);
                g2.fillRect(tipX + pixel, tipY - pixel * 3, pixel, pixel);
            } finally {
                g2.dispose();
            }
        }

        private static void drawPixelLine(Graphics2D g2, int x1, int y1, int x2, int y2, int pixel) {
            int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) / pixel;
            steps = Math.max(1, steps);
            for (int i = 0; i <= steps; i++) {
                int x = x1 + Math.round((x2 - x1) * (i / (float) steps));
                int y = y1 + Math.round((y2 - y1) * (i / (float) steps));
                g2.fillRect(x, y, pixel, pixel);
            }
        }
    }

    private static final class BowIconPanel extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int pixel = Math.max(2, getWidth() / 18);
                int centerY = getHeight() / 2;
                int arrowLeft = pixel * 4;
                int arrowRight = getWidth() - pixel * 3;
                int bowTopX = getWidth() - pixel * 8;
                int bowMidX = getWidth() - pixel * 5;
                int bowBotX = getWidth() - pixel * 8;
                int topY = pixel * 4;
                int bottomY = getHeight() - pixel * 4;

                g2.setColor(new Color(222, 216, 196));
                drawPixelLine(g2, arrowLeft, centerY, bowTopX, topY, pixel);
                drawPixelLine(g2, arrowLeft, centerY, bowBotX, bottomY, pixel);

                g2.setColor(new Color(74, 38, 18));
                drawPixelLine(g2, bowTopX + pixel, topY, bowMidX + pixel, centerY - pixel * 2, pixel);
                drawPixelLine(g2, bowMidX + pixel, centerY - pixel * 2, bowMidX + pixel, centerY + pixel * 2, pixel);
                drawPixelLine(g2, bowMidX + pixel, centerY + pixel * 2, bowBotX + pixel, bottomY, pixel);
                g2.setColor(new Color(205, 132, 62));
                drawPixelLine(g2, bowTopX, topY, bowMidX, centerY - pixel * 2, pixel);
                drawPixelLine(g2, bowMidX, centerY - pixel * 2, bowMidX, centerY + pixel * 2, pixel);
                drawPixelLine(g2, bowMidX, centerY + pixel * 2, bowBotX, bottomY, pixel);

                g2.setColor(new Color(92, 52, 24));
                g2.fillRect(arrowLeft, centerY - pixel, arrowRight - arrowLeft - pixel * 3, pixel * 2);
                g2.setColor(new Color(201, 130, 62));
                g2.fillRect(arrowLeft, centerY - pixel / 2, arrowRight - arrowLeft - pixel * 3, pixel);

                g2.setColor(new Color(220, 220, 220));
                g2.fillRect(arrowRight - pixel * 3, centerY - pixel * 2, pixel * 2, pixel);
                g2.fillRect(arrowRight - pixel * 2, centerY - pixel, pixel * 2, pixel);
                g2.fillRect(arrowRight - pixel, centerY, pixel, pixel);
                g2.fillRect(arrowRight - pixel * 2, centerY + pixel, pixel * 2, pixel);
                g2.fillRect(arrowRight - pixel * 3, centerY + pixel * 2, pixel * 2, pixel);

                g2.setColor(new Color(236, 214, 152));
                g2.fillRect(arrowLeft - pixel * 2, centerY - pixel * 2, pixel * 2, pixel);
                g2.fillRect(arrowLeft - pixel * 2, centerY + pixel, pixel * 2, pixel);
            } finally {
                g2.dispose();
            }
        }

        private static void drawPixelLine(Graphics2D g2, int x1, int y1, int x2, int y2, int pixel) {
            int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) / pixel;
            steps = Math.max(1, steps);
            for (int i = 0; i <= steps; i++) {
                int x = x1 + Math.round((x2 - x1) * (i / (float) steps));
                int y = y1 + Math.round((y2 - y1) * (i / (float) steps));
                g2.fillRect(x, y, pixel, pixel);
            }
        }
    }

    private static final class ArmorIconPanel extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                paintArmorPixelArt(g2, 0, 0, Math.min(getWidth(), getHeight()));
            } finally {
                g2.dispose();
            }
        }
    }

    private static void paintArmorPixelArt(Graphics2D g2, int x, int y, int size) {
        if (HeroArmorPixelArt.armorImage == null) {
            return;
        }
        int bodyH = size;
        int bodyW = Math.max(1, Math.round(size * 16f / 32f));
        int originX = x + (size - bodyW) / 2;
        g2.drawImage(HeroArmorPixelArt.armorImage, originX, y, bodyW, bodyH, null);
    }

    private static final class RetroSlotPanel extends JPanel {
        private final boolean filled;
        private final boolean equipped;
        private boolean hovered;

        RetroSlotPanel(boolean filled, boolean equipped) {
            this.filled = filled;
            this.equipped = equipped;
            setOpaque(false);
            setLayout(null);
        }

        void setHovered(boolean hovered) {
            this.hovered = hovered;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Color fill = hovered ? SLOT_HOVER : filled ? SLOT_FILLED : SLOT_EMPTY;
                Color border = equipped ? GOLD_BRIGHT : hovered ? GOLD : STONE_BORDER;
                g2.setColor(STONE_OUTLINE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(fill);
                g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
                g2.setColor(border);
                g2.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
                if (filled) {
                    g2.setColor(equipped ? GOLD_BRIGHT : new Color(122, 103, 69));
                    g2.fillRect(6, 6, getWidth() - 12, 2);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
