package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import engine.audio.AudioManager;
import view.assets.AssetManager;

/**
 * First-pass shop scene assembled from the provided pixel-art mockup assets.
 * It owns only presentation placeholders; buying and selling can be wired to
 * game state later without changing the layout surface.
 */
public final class ShopWindow extends JFrame {

    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;

    private final transient Runnable onExit;
    private final int gold;
    private boolean closing;

    public ShopWindow(int gold, Runnable onExit) {
        this.gold = Math.max(0, gold);
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

    private final class ShopPanel extends JPanel {

        private static final int ITEM_COUNT = 6;
        private static final double[] SLOT_CENTER_X = { 0.22, 0.50, 0.78 };
        private static final double[] SLOT_CENTER_Y = { 0.34, 0.70 };
        private static final double ITEM_FRAME_H_FRAC = 0.34;

        private final BufferedImage background = image("/shop/shop_background.png");
        private final BufferedImage vendor = image("/shop/shop_vendor.png");
        private final BufferedImage catalog = image("/shop/shop_catalog.png");
        private final BufferedImage goldPanel = image("/shop/gold_value.png");
        private final BufferedImage detailsPanel = image("/shop/item_details.png");
        private final BufferedImage itemFrame = image("/shop/item_frame_selected.png");
        private final BufferedImage actions = image("/shop/shop_actions.png");
        private final BufferedImage popup = image("/shop/shop_popup.png");

        private final Rectangle[] itemRects = new Rectangle[ITEM_COUNT];
        private Rectangle buyRect = new Rectangle();
        private Rectangle sellRect = new Rectangle();
        private Rectangle exitRect = new Rectangle();
        private int hoveredItem = -1;

        ShopPanel() {
            setPreferredSize(new Dimension(WINDOW_W, WINDOW_H));
            setBackground(Color.BLACK);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoveredItem = itemAt(e.getX(), e.getY());
                    boolean actionable = hoveredItem >= 0 || actionAt(e.getX(), e.getY()) >= 0;
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
                    int item = itemAt(e.getX(), e.getY());
                    if (item >= 0) {
                        AudioManager.shared().play("button_click");
                        repaint();
                        return;
                    }

                    int action = actionAt(e.getX(), e.getY());
                    if (action < 0) {
                        return;
                    }
                    AudioManager.shared().play("button_click");
                    if (action == 2) {
                        closeToPrevious();
                    }
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
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
            drawCover(g2, background, 0, 0, w, h);

            Rectangle gold = rectByWidth(goldPanel, pct(w, 0.02), pct(h, 0.03), pct(w, 0.18));
            draw(g2, goldPanel, gold);

            Rectangle vendorRect = rectByWidth(vendor, pct(w, 0.055), 0, pct(w, 0.40));
            vendorRect.y = pct(h, 0.74) - vendorRect.height;
            draw(g2, vendor, vendorRect);

            Rectangle details = rectByWidth(detailsPanel, pct(w, 0.03), pct(h, 0.72), pct(w, 0.41));
            draw(g2, detailsPanel, details);

            Rectangle catalogRect = rectByHeight(catalog, 0, pct(h, 0.065), pct(h, 0.65));
            catalogRect.x = w - catalogRect.width - pct(w, 0.04);
            draw(g2, catalog, catalogRect);

            layoutItems(catalogRect);
            for (int i = 0; i < ITEM_COUNT; i++) {
                draw(g2, itemFrame, itemRects[i]);
            }

            Rectangle actionRect = rectByWidth(actions, 0, pct(h, 0.73), pct(w, 0.42));
            actionRect.x = catalogRect.x + (catalogRect.width - actionRect.width) / 2;
            draw(g2, actions, actionRect);
            layoutActions(actionRect);

            Rectangle popupRect = rectByWidth(popup, 0, 0, pct(w, 0.46));
            popupRect.x = (w - popupRect.width) / 2;
            popupRect.y = h - popupRect.height - pct(h, 0.015);
            draw(g2, popup, popupRect);

            drawGoldText(g2, gold);
            g2.dispose();
        }

        private void layoutItems(Rectangle catalogRect) {
            int frameH = Math.max(1, (int) Math.round(catalogRect.height * ITEM_FRAME_H_FRAC));
            int frameW = itemFrame == null ? frameH * 3 / 4
                    : Math.max(1, Math.round(frameH * itemFrame.getWidth() / (float) itemFrame.getHeight()));
            for (int i = 0; i < ITEM_COUNT; i++) {
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

        private void layoutActions(Rectangle actionRect) {
            int padX = Math.max(8, actionRect.width / 30);
            int padY = Math.max(8, actionRect.height / 5);
            int buttonW = (actionRect.width - padX * 2) / 3;
            int buttonH = actionRect.height - padY * 2;
            buyRect = new Rectangle(actionRect.x + padX, actionRect.y + padY, buttonW, buttonH);
            sellRect = new Rectangle(actionRect.x + padX + buttonW, actionRect.y + padY, buttonW, buttonH);
            exitRect = new Rectangle(actionRect.x + padX + buttonW * 2, actionRect.y + padY, buttonW, buttonH);
        }

        private int itemAt(int x, int y) {
            for (int i = 0; i < ITEM_COUNT; i++) {
                if (itemRects[i] != null && itemRects[i].contains(x, y)) {
                    return i;
                }
            }
            return -1;
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
            String amount = Integer.toString(gold);
            int textX = r.x + r.width * 65 / 100;
            int maxTextW = r.width * 26 / 100;
            float size = 15f;
            g2.setFont(bodyFont(size));
            while (size > 9f && g2.getFontMetrics().stringWidth(amount) > maxTextW) {
                size -= 1f;
                g2.setFont(bodyFont(size));
            }
            g2.setColor(new Color(255, 218, 83));
            g2.drawString(amount, textX, r.y + r.height * 62 / 100);
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
