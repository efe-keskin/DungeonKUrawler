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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import engine.ShopController;
import engine.audio.AudioManager;
import model.FullGameInventory;
import model.Item;
import model.ShopOffer;
import view.assets.AssetManager;
import view.assets.SpriteRegistry;

/**
 * Shop scene assembled from the pixel-art mockup assets. SHOP mode lists the
 * vendor's {@link ShopOffer}s; INVENTORY mode lists the player's persistent
 * {@link FullGameInventory}. Both views are paginated (six slots per page, two
 * arrows). Buying and selling are delegated to {@link ShopController}; this
 * class stays presentation-only.
 */
public final class ShopWindow extends JFrame {

    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;

    private final transient Runnable onExit;
    private final transient FullGameInventory fullInventory;
    private final transient ShopController shopController;
    private boolean closing;

    public ShopWindow(FullGameInventory fullInventory, ShopController shopController, Runnable onExit) {
        this.fullInventory = fullInventory;
        this.shopController = shopController;
        this.onExit = onExit;
        setTitle("Dungeon KUrawler - Shop");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setContentPane(new ShopPanel());
        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
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

    /** One rendered slot: a sprite plus its display text and backing model object. */
    private record Entry(BufferedImage sprite, String title, String subtitle, Object source) {
    }

    private final class ShopPanel extends JPanel {

        private static final int SLOTS_PER_PAGE = 6;
        private static final double[] SLOT_CENTER_X = { 0.238, 0.499, 0.762 };
        private static final double[] SLOT_CENTER_Y = { 0.355, 0.594 };
        private static final double ITEM_FRAME_H_FRAC = 0.30;

        private final BufferedImage background = image("/shop/shop_background.png");
        private final BufferedImage vendor = image("/shop/shop_vendor.png");
        private final BufferedImage catalog = image("/shop/shop_catalog.png");
        private final BufferedImage inventoryCatalog = image("/shop/inventory_catalog.png");
        private final BufferedImage goldPanel = image("/shop/gold_value.png");
        private final BufferedImage detailsPanel = image("/shop/item_details.png");
        private final BufferedImage itemFrame = image("/shop/item_frame_selected.png");
        private final BufferedImage actions = image("/shop/shop_actions.png");

        private final Rectangle[] itemRects = new Rectangle[SLOTS_PER_PAGE];
        private Rectangle buyRect = new Rectangle();
        private Rectangle sellRect = new Rectangle();
        private Rectangle exitRect = new Rectangle();
        private Rectangle modeRect = new Rectangle();
        private Rectangle prevPageRect = new Rectangle();
        private Rectangle nextPageRect = new Rectangle();
        private Rectangle detailsRect = new Rectangle();
        private int hoveredItem = -1;
        private boolean inventoryMode;
        private int page;
        private int selectedIndex = -1;

        ShopPanel() {
            setPreferredSize(new Dimension(WINDOW_W, WINDOW_H));
            setBackground(Color.BLACK);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoveredItem = itemAt(e.getX(), e.getY());
                    boolean actionable = hoveredItem >= 0 || actionAt(e.getX(), e.getY()) >= 0
                            || modeRect.contains(e.getX(), e.getY())
                            || arrowAt(e.getX(), e.getY()) != 0;
                    setCursor(Cursor.getPredefinedCursor(actionable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredItem = -1;
                    setCursor(Cursor.getDefaultCursor());
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

        private void handleClick(int x, int y) {
            int arrow = arrowAt(x, y);
            if (arrow != 0) {
                changePage(arrow);
                return;
            }

            int slot = itemAt(x, y);
            if (slot >= 0) {
                int globalIndex = page * SLOTS_PER_PAGE + slot;
                if (globalIndex < entries().size()) {
                    AudioManager.shared().play("button_click");
                    selectedIndex = globalIndex;
                    repaint();
                }
                return;
            }

            if (modeRect.contains(x, y)) {
                AudioManager.shared().play("button_click");
                inventoryMode = !inventoryMode;
                page = 0;
                selectedIndex = -1;
                repaint();
                return;
            }

            int action = actionAt(x, y);
            if (action < 0) {
                return;
            }
            AudioManager.shared().play("button_click");
            switch (action) {
                case 0 -> doBuy();
                case 1 -> doSell();
                case 2 -> closeToPrevious();
                default -> { /* no-op */ }
            }
        }

        private void changePage(int direction) {
            int target = page + direction;
            if (target < 0 || target >= pageCount()) {
                return;
            }
            AudioManager.shared().play("button_click");
            page = target;
            repaint();
        }

        private void doBuy() {
            if (inventoryMode || shopController == null) {
                return;
            }
            Object source = selectedSource();
            if (source instanceof ShopOffer offer) {
                shopController.buy(offer);
                clampSelection();
                repaint();
            }
        }

        private void doSell() {
            if (!inventoryMode || shopController == null) {
                return;
            }
            Object source = selectedSource();
            if (source instanceof Item item) {
                shopController.sell(item);
                selectedIndex = -1;
                clampSelection();
                repaint();
            }
        }

        /** The model objects backing the active mode (offers or persistent items). */
        private List<Entry> entries() {
            List<Entry> result = new ArrayList<>();
            if (inventoryMode) {
                if (fullInventory != null) {
                    for (Item item : fullInventory.getItems()) {
                        result.add(new Entry(SpriteRegistry.spriteFor(item), item.getName(), null, item));
                    }
                }
            } else if (shopController != null) {
                for (ShopOffer offer : shopController.getCatalog().offers()) {
                    BufferedImage sprite = offer.spriteResource() == null ? null
                            : AssetManager.get().image(offer.spriteResource());
                    result.add(new Entry(sprite, offer.getName(), offer.getPrice() + "g", offer));
                }
            }
            return result;
        }

        private int pageCount() {
            int total = entries().size();
            return Math.max(1, (total + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
        }

        private Object selectedSource() {
            List<Entry> entries = entries();
            if (selectedIndex < 0 || selectedIndex >= entries.size()) {
                return null;
            }
            return entries.get(selectedIndex).source();
        }

        private void clampSelection() {
            int size = entries().size();
            if (selectedIndex >= size) {
                selectedIndex = size - 1;
            }
            if (page >= pageCount()) {
                page = pageCount() - 1;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);

            int w = getWidth();
            int h = getHeight();
            clampSelection();
            drawCover(g2, background, 0, 0, w, h);

            Rectangle gold = rectByWidth(goldPanel, pct(w, 0.02), pct(h, 0.03), pct(w, 0.18));
            draw(g2, goldPanel, gold);

            Rectangle vendorRect = rectByWidth(vendor, pct(w, 0.055), 0, pct(w, 0.40));
            vendorRect.y = pct(h, 0.74) - vendorRect.height;
            draw(g2, vendor, vendorRect);

            detailsRect = rectByWidth(detailsPanel, pct(w, 0.04), pct(h, 0.78), pct(w, 0.32));
            int detailsBottom = detailsRect.y + detailsRect.height;
            detailsRect.width = pct(w, 0.384);
            detailsRect.height = detailsPanel == null ? detailsRect.width
                    : Math.max(1, Math.round(detailsRect.width * detailsPanel.getHeight() / (float) detailsPanel.getWidth()));
            detailsRect.y = detailsBottom - detailsRect.height;
            draw(g2, detailsPanel, detailsRect);

            BufferedImage activeCatalog = inventoryMode && inventoryCatalog != null ? inventoryCatalog : catalog;
            Rectangle catalogRect = rectByHeight(activeCatalog, 0, pct(h, 0.065), pct(h, 0.65));
            catalogRect.x = w - catalogRect.width - pct(w, 0.04);
            draw(g2, activeCatalog, catalogRect);

            layoutItems(catalogRect);
            drawSlots(g2);

            layoutPager(catalogRect);
            drawPager(g2);

            layoutModeButton(catalogRect, w, h);
            drawModeButton(g2);

            Rectangle actionRect = rectByWidth(actions, 0, pct(h, 0.80), pct(w, 0.34));
            int actionsBottom = actionRect.y + actionRect.height;
            actionRect.width = pct(w, 0.408);
            actionRect.height = actions == null ? actionRect.width
                    : Math.max(1, Math.round(actionRect.width * actions.getHeight() / (float) actions.getWidth()));
            actionRect.y = actionsBottom - actionRect.height;
            actionRect.x = Math.min(w - actionRect.width - pct(w, 0.02),
                    catalogRect.x + (catalogRect.width - actionRect.width) / 2);
            draw(g2, actions, actionRect);
            layoutActions(actionRect);

            drawDetails(g2);
            drawGoldText(g2, gold);
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
                itemRects[i] = new Rectangle(
                        cx - frameW / 2,
                        cy - frameH / 2,
                        frameW,
                        frameH);
            }
        }

        private void drawSlots(Graphics2D g2) {
            List<Entry> entries = entries();
            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                Rectangle frame = itemRects[i];
                draw(g2, itemFrame, frame);

                int globalIndex = page * SLOTS_PER_PAGE + i;
                if (globalIndex >= entries.size()) {
                    continue;
                }
                Entry entry = entries.get(globalIndex);
                drawSlotSprite(g2, frame, entry.sprite());

                if (globalIndex == selectedIndex) {
                    g2.setColor(new Color(255, 224, 120));
                    g2.drawRect(frame.x + 2, frame.y + 2, frame.width - 5, frame.height - 5);
                    g2.drawRect(frame.x + 3, frame.y + 3, frame.width - 7, frame.height - 7);
                }
            }
        }

        /** Draws an item sprite centered in the frame's inner area, preserving aspect. */
        private void drawSlotSprite(Graphics2D g2, Rectangle frame, BufferedImage sprite) {
            if (sprite == null) {
                return;
            }
            int innerW = (int) Math.round(frame.width * 0.62);
            int innerH = (int) Math.round(frame.height * 0.62);
            double scale = Math.min(innerW / (double) sprite.getWidth(), innerH / (double) sprite.getHeight());
            int sw = Math.max(1, (int) Math.round(sprite.getWidth() * scale));
            int sh = Math.max(1, (int) Math.round(sprite.getHeight() * scale));
            int sx = frame.x + (frame.width - sw) / 2;
            int sy = frame.y + (frame.height - sh) / 2;
            g2.drawImage(sprite, sx, sy, sw, sh, null);
        }

        private void layoutActions(Rectangle actionRect) {
            int padX = Math.max(8, actionRect.width / 30);
            int padY = Math.max(8, actionRect.height / 5);
            int buttonW = (actionRect.width - padX * 2) / 3;
            int buttonH = actionRect.height - padY * 2;
            buyRect = new Rectangle(actionRect.x + padX, actionRect.y + padY, buttonW, buttonH);
            sellRect = new Rectangle(actionRect.x + padX + buttonW, actionRect.y + padY, buttonW, buttonH);
            exitRect = new Rectangle(actionRect.x + padX + buttonW * 2, actionRect.y + padY, buttonW, buttonH);
        }

        private void layoutModeButton(Rectangle catalogRect, int w, int h) {
            int buttonW = Math.max(150, catalogRect.width / 3);
            int buttonH = 42;
            int rightInset = pct(w, 0.025);
            int bottomLimit = pct(h, 0.80) - buttonH - 8;
            modeRect = new Rectangle(
                    Math.min(catalogRect.x + catalogRect.width - buttonW - 16,
                            w - buttonW - rightInset),
                    Math.min(catalogRect.y + catalogRect.height + 10, bottomLimit),
                    buttonW,
                    buttonH);
        }

        /** Two paging arrows in the catalog's lower decorative band, flanking a page label. */
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
            boolean hasPrev = page > 0;
            boolean hasNext = page < total - 1;
            drawArrow(g2, prevPageRect, true, hasPrev);
            drawArrow(g2, nextPageRect, false, hasNext);

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
            List<Entry> entries = entries();
            if (selectedIndex < 0 || selectedIndex >= entries.size() || detailsRect.width <= 0) {
                return;
            }
            Entry entry = entries.get(selectedIndex);

            // Content lives inside the panel's inner border (~0.12..0.88 of the art),
            // laid out on one row: icon + name at the left, price/value at the right.
            int innerLeft = detailsRect.x + (int) Math.round(detailsRect.width * 0.12);
            int innerRight = detailsRect.x + (int) Math.round(detailsRect.width * 0.88);
            int midY = detailsRect.y + detailsRect.height / 2;

            int nameX = innerLeft;
            if (entry.sprite() != null) {
                int iconBox = (int) Math.round(detailsRect.height * 0.5);
                double scale = iconBox / (double) Math.max(entry.sprite().getWidth(), entry.sprite().getHeight());
                int sw = Math.max(1, (int) Math.round(entry.sprite().getWidth() * scale));
                int sh = Math.max(1, (int) Math.round(entry.sprite().getHeight() * scale));
                g2.drawImage(entry.sprite(), innerLeft, midY - sh / 2, sw, sh, null);
                nameX = innerLeft + sw + Math.max(8, detailsRect.width / 40);
            }

            g2.setFont(bodyFont(14f));
            int baseY = midY + g2.getFontMetrics().getAscent() / 2 - 2;
            g2.setColor(new Color(255, 238, 176));
            g2.drawString(entry.title(), nameX, baseY);

            String sub = inventoryMode
                    ? (shopController == null ? null : "Sell: " + shopController.sellPriceOf((Item) entry.source()) + "g")
                    : entry.subtitle() == null ? null : "Price: " + entry.subtitle();
            if (sub != null) {
                g2.setColor(new Color(255, 218, 83));
                g2.setFont(bodyFont(13f));
                int subW = g2.getFontMetrics().stringWidth(sub);
                g2.drawString(sub, innerRight - subW, baseY);
            }
        }

        private int itemAt(int x, int y) {
            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                if (itemRects[i] != null && itemRects[i].contains(x, y)) {
                    return i;
                }
            }
            return -1;
        }

        /** -1 = previous page, +1 = next page, 0 = no arrow (only when the arrow is enabled). */
        private int arrowAt(int x, int y) {
            if (prevPageRect.contains(x, y) && page > 0) {
                return -1;
            }
            if (nextPageRect.contains(x, y) && page < pageCount() - 1) {
                return 1;
            }
            return 0;
        }

        private int actionAt(int x, int y) {
            if (buyRect.contains(x, y)) {
                return 0;
            }
            if (sellRect.contains(x, y)) {
                return 1;
            }
            if (exitRect.contains(x, y)) {
                return 2;
            }
            return -1;
        }

        private void drawGoldText(Graphics2D g2, Rectangle r) {
            String amount = Integer.toString(fullInventory == null ? 0 : fullInventory.getGold());
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

        private void drawModeButton(Graphics2D g2) {
            g2.setColor(new Color(54, 41, 30, 235));
            g2.fillRect(modeRect.x, modeRect.y, modeRect.width, modeRect.height);
            g2.setColor(new Color(151, 120, 67));
            g2.drawRect(modeRect.x, modeRect.y, modeRect.width - 1, modeRect.height - 1);

            String label = inventoryMode ? "SHOP" : "INVENTORY";
            float size = label.length() > 4 ? 11f : 15f;
            g2.setFont(bodyFont(size));
            while (size > 9f && g2.getFontMetrics().stringWidth(label) > modeRect.width - 18) {
                size -= 1f;
                g2.setFont(bodyFont(size));
            }
            g2.setColor(new Color(255, 238, 176));
            int textW = g2.getFontMetrics().stringWidth(label);
            int textY = modeRect.y + (modeRect.height + g2.getFontMetrics().getAscent()) / 2 - 3;
            g2.drawString(label, modeRect.x + Math.max(0, (modeRect.width - textW) / 2), textY);
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
        int sx = x + (w - sw) / 2;
        int sy = y + (h - sh) / 2;
        g2.drawImage(img, sx, sy, sw, sh, null);
    }

    private static Font bodyFont(float size) {
        Font base = RetroTheme.UI_MONO_SMALL != null ? RetroTheme.UI_MONO_SMALL
                : new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
        return base.deriveFont(Font.PLAIN, size);
    }
}
