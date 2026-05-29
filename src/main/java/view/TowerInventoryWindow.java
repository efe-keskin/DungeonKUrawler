package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import engine.PetController;
import engine.audio.AudioManager;
import model.FullGameInventory;
import model.Item;
import model.Pet;
import view.assets.AssetManager;
import view.assets.SpriteRegistry;

/**
 * View of the player's persistent {@link FullGameInventory}, opened from the
 * tower map's inventory icon. Mirrors the shop's INVENTORY catalog (six
 * paginated slots, two arrows, an item-details panel). A selected {@link Pet}
 * can be equipped here via {@link PetController}; other items are display-only.
 */
public final class TowerInventoryWindow extends JFrame {

    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;

    private final transient Runnable onExit;
    private final transient PetController petController;
    private final transient Runnable onChange;
    private boolean closing;

    public TowerInventoryWindow(FullGameInventory inventory, PetController petController,
            Runnable onChange, Runnable onExit) {
        this.onExit = onExit;
        this.petController = petController;
        this.onChange = onChange;
        setTitle("Dungeon KUrawler - Inventory");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        setContentPane(new InventoryPanel(inventory));
        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
        getRootPane().registerKeyboardAction(e -> closeToPrevious(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeToPrevious();
            }
        });
    }

    private void closeToPrevious() {
        if (closing) {
            return;
        }
        closing = true;
        dispose();
        if (onExit != null) {
            onExit.run();
        }
    }

    private final class InventoryPanel extends JPanel {

        private static final int SLOTS_PER_PAGE = 6;
        private static final double[] SLOT_CENTER_X = { 0.237, 0.498, 0.760 };
        private static final double[] SLOT_CENTER_Y = { 0.359, 0.604 };
        private static final double ITEM_FRAME_H_FRAC = 0.30;

        private final transient FullGameInventory inventory;

        private final BufferedImage background = image("/shop/shop_background.png");
        private final BufferedImage catalog = image("/shop/inventory_catalog.png");
        private final BufferedImage goldPanel = image("/shop/gold_value.png");
        private final BufferedImage detailsPanel = image("/shop/item_details.png");
        private final BufferedImage itemFrame = image("/shop/item_frame_selected.png");

        private final Rectangle[] itemRects = new Rectangle[SLOTS_PER_PAGE];
        private Rectangle prevPageRect = new Rectangle();
        private Rectangle nextPageRect = new Rectangle();
        private Rectangle detailsRect = new Rectangle();
        private Rectangle backRect = new Rectangle();
        private Rectangle equipRect = new Rectangle();
        private boolean backHover;
        private boolean equipHover;
        private int page;
        private int selectedIndex = -1;

        InventoryPanel(FullGameInventory inventory) {
            this.inventory = inventory;
            setPreferredSize(new Dimension(WINDOW_W, WINDOW_H));
            setBackground(Color.BLACK);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    backHover = backRect.contains(e.getX(), e.getY());
                    equipHover = canEquipSelected() && equipRect.contains(e.getX(), e.getY());
                    boolean actionable = backHover || equipHover || itemAt(e.getX(), e.getY()) >= 0
                            || arrowAt(e.getX(), e.getY()) != 0;
                    setCursor(Cursor.getPredefinedCursor(actionable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    handleClick(e.getX(), e.getY());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        /** The currently selected item if it is a pet that can be equipped, else null. */
        private Pet selectedPet() {
            if (petController == null || selectedIndex < 0 || selectedIndex >= items().size()) {
                return null;
            }
            return items().get(selectedIndex) instanceof Pet pet ? pet : null;
        }

        private boolean canEquipSelected() {
            Pet pet = selectedPet();
            return pet != null && !petController.isEquipped(pet);
        }

        private void handleClick(int x, int y) {
            if (backRect.contains(x, y)) {
                AudioManager.shared().play("button_click");
                closeToPrevious();
                return;
            }
            if (canEquipSelected() && equipRect.contains(x, y)) {
                AudioManager.shared().play("button_click");
                petController.equip(selectedPet());
                if (onChange != null) {
                    onChange.run();
                }
                repaint();
                return;
            }
            int arrow = arrowAt(x, y);
            if (arrow != 0) {
                page += arrow;
                AudioManager.shared().play("button_click");
                repaint();
                return;
            }
            int slot = itemAt(x, y);
            if (slot >= 0) {
                int globalIndex = page * SLOTS_PER_PAGE + slot;
                if (globalIndex < items().size()) {
                    AudioManager.shared().play("button_click");
                    selectedIndex = globalIndex;
                    repaint();
                }
            }
        }

        private List<Item> items() {
            return inventory == null ? List.of() : inventory.getItems();
        }

        private int pageCount() {
            int total = items().size();
            return Math.max(1, (total + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            int w = getWidth();
            int h = getHeight();
            if (page >= pageCount()) {
                page = pageCount() - 1;
            }
            if (selectedIndex >= items().size()) {
                selectedIndex = -1;
            }
            drawCover(g2, background, 0, 0, w, h);

            Rectangle gold = rectByWidth(goldPanel, pct(w, 0.03), pct(h, 0.035), pct(w, 0.26));
            draw(g2, goldPanel, gold);
            drawGoldText(g2, gold);

            BufferedImage activeCatalog = catalog;
            Rectangle catalogRect = rectByHeight(activeCatalog, 0, pct(h, 0.06), pct(h, 0.62));
            catalogRect.x = (w - catalogRect.width) / 2;
            draw(g2, activeCatalog, catalogRect);

            layoutItems(catalogRect);
            drawSlots(g2);
            layoutPager(catalogRect);
            drawPager(g2);

            detailsRect = rectByWidth(detailsPanel, 0, 0, pct(w, 0.42));
            detailsRect.x = (w - detailsRect.width) / 2;
            detailsRect.y = h - detailsRect.height - pct(h, 0.03);
            draw(g2, detailsPanel, detailsRect);
            drawDetails(g2);

            layoutBack(w, h);
            drawBack(g2);
            layoutEquip(w, h);
            drawEquip(g2);

            g2.dispose();
        }

        private void layoutItems(Rectangle catalogRect) {
            int frameH = Math.max(1, (int) Math.round(catalogRect.height * ITEM_FRAME_H_FRAC));
            int frameW = itemFrame == null ? frameH * 3 / 4
                    : Math.max(1, Math.round(frameH * itemFrame.getWidth() / (float) itemFrame.getHeight()));
            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                int col = i % 3;
                int row = i / 3;
                int cx = catalogRect.x + (int) Math.round(catalogRect.width * SLOT_CENTER_X[col]);
                int cy = catalogRect.y + (int) Math.round(catalogRect.height * SLOT_CENTER_Y[row]);
                itemRects[i] = new Rectangle(cx - frameW / 2, cy - frameH / 2, frameW, frameH);
            }
        }

        private void drawSlots(Graphics2D g2) {
            List<Item> items = items();
            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                Rectangle frame = itemRects[i];
                draw(g2, itemFrame, frame);
                int globalIndex = page * SLOTS_PER_PAGE + i;
                if (globalIndex >= items.size()) {
                    continue;
                }
                drawSlotSprite(g2, frame, SpriteRegistry.spriteFor(items.get(globalIndex)));
                if (globalIndex == selectedIndex) {
                    g2.setColor(new Color(255, 224, 120));
                    g2.drawRect(frame.x + 2, frame.y + 2, frame.width - 5, frame.height - 5);
                    g2.drawRect(frame.x + 3, frame.y + 3, frame.width - 7, frame.height - 7);
                }
            }
        }

        private void drawSlotSprite(Graphics2D g2, Rectangle frame, BufferedImage sprite) {
            if (sprite == null) {
                return;
            }
            int innerW = (int) Math.round(frame.width * 0.62);
            int innerH = (int) Math.round(frame.height * 0.62);
            double scale = Math.min(innerW / (double) sprite.getWidth(), innerH / (double) sprite.getHeight());
            int sw = Math.max(1, (int) Math.round(sprite.getWidth() * scale));
            int sh = Math.max(1, (int) Math.round(sprite.getHeight() * scale));
            g2.drawImage(sprite, frame.x + (frame.width - sw) / 2, frame.y + (frame.height - sh) / 2, sw, sh, null);
        }

        private void layoutPager(Rectangle catalogRect) {
            int size = Math.max(20, catalogRect.width / 14);
            int cy = catalogRect.y + (int) Math.round(catalogRect.height * 0.915) - size / 2;
            int leftX = catalogRect.x + (int) Math.round(catalogRect.width * 0.10);
            int rightX = catalogRect.x + (int) Math.round(catalogRect.width * 0.90) - size;
            prevPageRect = new Rectangle(leftX, cy, size, size);
            nextPageRect = new Rectangle(rightX, cy, size, size);
        }

        private void drawPager(Graphics2D g2) {
            int total = pageCount();
            drawArrow(g2, prevPageRect, true, page > 0);
            drawArrow(g2, nextPageRect, false, page < total - 1);
            String label = (page + 1) + " / " + total;
            g2.setFont(bodyFont(13f));
            g2.setColor(new Color(255, 238, 176));
            int textW = g2.getFontMetrics().stringWidth(label);
            int midX = (prevPageRect.x + prevPageRect.width + nextPageRect.x) / 2;
            int baseY = prevPageRect.y + (prevPageRect.height + g2.getFontMetrics().getAscent()) / 2 - 2;
            g2.drawString(label, midX - textW / 2, baseY);
        }

        private void drawArrow(Graphics2D g2, Rectangle r, boolean pointsLeft, boolean enabled) {
            int tip = pointsLeft ? r.x : r.x + r.width;
            int base = pointsLeft ? r.x + r.width : r.x;
            Polygon p = new Polygon();
            p.addPoint(tip, r.y + r.height / 2);
            p.addPoint(base, r.y);
            p.addPoint(base, r.y + r.height);
            g2.setColor(enabled ? new Color(255, 224, 120) : new Color(120, 100, 70, 130));
            g2.fillPolygon(p);
        }

        private void drawDetails(Graphics2D g2) {
            List<Item> items = items();
            if (selectedIndex < 0 || selectedIndex >= items.size() || detailsRect.width <= 0) {
                return;
            }
            Item item = items.get(selectedIndex);
            BufferedImage sprite = SpriteRegistry.spriteFor(item);
            int innerLeft = detailsRect.x + (int) Math.round(detailsRect.width * 0.12);
            int innerRight = detailsRect.x + (int) Math.round(detailsRect.width * 0.88);
            int midY = detailsRect.y + detailsRect.height / 2;

            int nameX = innerLeft;
            if (sprite != null) {
                int iconBox = (int) Math.round(detailsRect.height * 0.5);
                double scale = iconBox / (double) Math.max(sprite.getWidth(), sprite.getHeight());
                int sw = Math.max(1, (int) Math.round(sprite.getWidth() * scale));
                int sh = Math.max(1, (int) Math.round(sprite.getHeight() * scale));
                g2.drawImage(sprite, innerLeft, midY - sh / 2, sw, sh, null);
                nameX = innerLeft + sw + Math.max(8, detailsRect.width / 40);
            }
            g2.setFont(bodyFont(14f));
            int baseY = midY + g2.getFontMetrics().getAscent() / 2 - 2;
            g2.setColor(new Color(255, 238, 176));
            g2.drawString(item.getName(), nameX, baseY);

            String sub;
            if (item instanceof Pet pet) {
                sub = "HP " + pet.getHp() + "/" + pet.getMaxHp()
                        + (petController != null && petController.isEquipped(pet) ? "  Equipped" : "");
            } else {
                sub = items.size() + " item" + (items.size() == 1 ? "" : "s");
            }
            g2.setFont(bodyFont(12f));
            g2.setColor(new Color(210, 196, 150));
            int subW = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, innerRight - subW, baseY);
        }

        private void layoutBack(int w, int h) {
            int buttonW = Math.max(96, pct(w, 0.13));
            int buttonH = Math.max(34, pct(h, 0.075));
            backRect = new Rectangle(w - buttonW - pct(w, 0.03), pct(h, 0.035), buttonW, buttonH);
        }

        private void drawBack(Graphics2D g2) {
            g2.setColor(backHover ? new Color(74, 57, 40, 245) : new Color(54, 41, 30, 235));
            g2.fillRect(backRect.x, backRect.y, backRect.width, backRect.height);
            g2.setColor(new Color(151, 120, 67));
            g2.drawRect(backRect.x, backRect.y, backRect.width - 1, backRect.height - 1);

            String label = "BACK";
            g2.setFont(bodyFont(Math.max(11f, backRect.height * 0.4f)));
            g2.setColor(new Color(255, 238, 176));
            int textW = g2.getFontMetrics().stringWidth(label);
            int textY = backRect.y + (backRect.height + g2.getFontMetrics().getAscent()) / 2 - 3;
            g2.drawString(label, backRect.x + (backRect.width - textW) / 2, textY);
        }

        /** Equip button: shown beneath the details panel only when a pet is selected. */
        private void layoutEquip(int w, int h) {
            int buttonW = Math.max(120, pct(w, 0.16));
            int buttonH = Math.max(34, pct(h, 0.075));
            equipRect = new Rectangle((w - buttonW) / 2, h - buttonH - pct(h, 0.005), buttonW, buttonH);
        }

        private void drawEquip(Graphics2D g2) {
            Pet pet = selectedPet();
            if (pet == null) {
                return;
            }
            boolean equipped = petController.isEquipped(pet);
            String label = equipped ? "EQUIPPED" : "EQUIP";
            g2.setColor(equipped ? new Color(40, 60, 40, 235)
                    : equipHover ? new Color(74, 57, 40, 245) : new Color(54, 41, 30, 235));
            g2.fillRect(equipRect.x, equipRect.y, equipRect.width, equipRect.height);
            g2.setColor(equipped ? new Color(120, 180, 120) : new Color(151, 120, 67));
            g2.drawRect(equipRect.x, equipRect.y, equipRect.width - 1, equipRect.height - 1);
            g2.setFont(bodyFont(Math.max(11f, equipRect.height * 0.38f)));
            g2.setColor(new Color(255, 238, 176));
            int textW = g2.getFontMetrics().stringWidth(label);
            int textY = equipRect.y + (equipRect.height + g2.getFontMetrics().getAscent()) / 2 - 3;
            g2.drawString(label, equipRect.x + (equipRect.width - textW) / 2, textY);
        }

        private void drawGoldText(Graphics2D g2, Rectangle r) {
            String amount = Integer.toString(inventory == null ? 0 : inventory.getGold());
            int textX = r.x + r.width * 60 / 100;
            int maxTextW = r.width * 26 / 100;
            float size = 12.75f;
            g2.setFont(bodyFont(size));
            while (size > 9f && g2.getFontMetrics().stringWidth(amount) > maxTextW) {
                size -= 1f;
                g2.setFont(bodyFont(size));
            }
            g2.setColor(new Color(255, 218, 83));
            g2.drawString(amount, textX, r.y + r.height * 62 / 100);
        }

        private int itemAt(int x, int y) {
            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                if (itemRects[i] != null && itemRects[i].contains(x, y)) {
                    return i;
                }
            }
            return -1;
        }

        private int arrowAt(int x, int y) {
            if (prevPageRect.contains(x, y) && page > 0) {
                return -1;
            }
            if (nextPageRect.contains(x, y) && page < pageCount() - 1) {
                return 1;
            }
            return 0;
        }
    }

    private static BufferedImage image(String path) {
        return AssetManager.get().image(path);
    }

    private static int pct(int value, double fraction) {
        return (int) Math.round(value * fraction);
    }

    private static Rectangle rectByWidth(BufferedImage img, int x, int y, int width) {
        int height = img == null ? width : Math.max(1, Math.round(width * img.getHeight() / (float) img.getWidth()));
        return new Rectangle(x, y, width, height);
    }

    private static Rectangle rectByHeight(BufferedImage img, int x, int y, int height) {
        int width = img == null ? height : Math.max(1, Math.round(height * img.getWidth() / (float) img.getHeight()));
        return new Rectangle(x, y, width, height);
    }

    private static void draw(Graphics2D g2, BufferedImage img, Rectangle r) {
        if (img != null) {
            g2.drawImage(img, r.x, r.y, r.width, r.height, null);
        }
    }

    private static void drawCover(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        if (img == null) {
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, w, h);
            return;
        }
        double scale = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int sw = (int) Math.round(img.getWidth() * scale);
        int sh = (int) Math.round(img.getHeight() * scale);
        g2.drawImage(img, x + (w - sw) / 2, y + (h - sh) / 2, sw, sh, null);
    }

    private static Font bodyFont(float size) {
        Font base = RetroTheme.UI_MONO_SMALL != null ? RetroTheme.UI_MONO_SMALL
                : new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
        return base.deriveFont(Font.PLAIN, size);
    }
}
