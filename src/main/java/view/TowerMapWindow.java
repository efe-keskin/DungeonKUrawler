package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntConsumer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ImageIcon;

import engine.TowerProgressController;
import engine.audio.AudioManager;
import view.assets.SpriteRegistry;

/**
 * Tower Scenario Map (UC-T1/T5): renders the tower illustration and overlays a
 * lock badge on every floor the player cannot yet enter, so locked floors show
 * the lock indicator in place of their (baked-in) number. Clicking an enterable
 * floor forwards its number to the session controller. Low Coupling: this view
 * only reads display models from {@link TowerProgressController} and emits the
 * chosen floor number; it holds no progression rules or save logic.
 */
public final class TowerMapWindow extends JFrame {

    private static final Color BACKDROP = new Color(16, 14, 20);
    private static final Color TITLE_GOLD = new Color(244, 205, 103);
    private static final Color TEXT = new Color(220, 214, 200);
    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;
    private static final int FLOOR_COUNT = 10;
    private static final int MAP_BUTTON_MARGIN = 12;
    private static final int BACK_BUTTON_W = 104;
    private static final int BACK_BUTTON_H = 46;
    private static final int SHOP_BUTTON_SIZE = 74;
    private static final int SHOP_ICON_SIZE = 62;

    // --- Tower geometry, expressed as fractions of scenario_levels.png.
    //     One entry per actual floor keeps clicks aligned with the hand-placed
    //     level indicators in the art.
    private static final double LOCK_DIAM_FRAC = 0.065; // lock badge diameter
    private static final double[] BADGE_X_FRACS = {
            0.0,
            0.095, // floor 1
            0.095,
            0.095,
            0.095,
            0.094,
            0.095,
            0.095,
            0.095,
            0.095,
            0.096  // floor 10
    };
    private static final double[] BADGE_Y_FRACS = {
            0.0,
            0.915, // floor 1
            0.837,
            0.749,
            0.663,
            0.549,
            0.456,
            0.367,
            0.286,
            0.197,
            0.114  // floor 10
    };
    private static final double[] FLOOR_TOP_FRACS = {
            0.0,
            0.872, // floor 1
            0.782,
            0.692,
            0.607,
            0.482,
            0.407,
            0.323,
            0.234,
            0.145,
            0.022  // floor 10
    };
    private static final double[] FLOOR_BOTTOM_FRACS = {
            0.0,
            0.965, // floor 1
            0.872,
            0.782,
            0.692,
            0.607,
            0.482,
            0.407,
            0.323,
            0.234,
            0.145  // floor 10
    };
    private static final double[] HERO_X_FRACS = {
            0.0,
            0.665,
            0.390,
            0.420,
            0.650,
            0.500,
            0.700,
            0.720,
            0.660,
            0.700,
            0.710
    };
    private static final double[] HERO_Y_FRACS = {
            0.0,
            0.957,
            0.868,
            0.786,
            0.696,
            0.606,
            0.486,
            0.412,
            0.326,
            0.238,
            0.151
    };
    private static final double[][] CLIMB_PATH_X_FRACS = {
            {},
            { 0.665, 0.770 },
            { 0.390, 0.410 },
            { 0.420, 0.430 },
            { 0.650, 0.705 },
            { 0.500, 0.505 },
            { 0.700, 0.655, 0.715 },
            { 0.720, 0.720 },
            { 0.660, 0.660 },
            { 0.700, 0.705 }
    };
    private static final double[][] CLIMB_PATH_Y_FRACS = {
            {},
            { 0.957, 0.868 },
            { 0.868, 0.786 },
            { 0.786, 0.696 },
            { 0.696, 0.606 },
            { 0.606, 0.486 },
            { 0.486, 0.452, 0.412 },
            { 0.412, 0.326 },
            { 0.326, 0.238 },
            { 0.238, 0.151 }
    };
    private static final int CLIMB_TICKS = 36;

    /** Fallback width before the scroll viewport has completed layout. */
    private static final int ART_FALLBACK_WIDTH = 880;

    private static final BufferedImage TOWER = loadImage("/scenario_levels.png", false);
    private static final BufferedImage LOCK = loadImage("/level_locked_indicator.png", true);
    private static final BufferedImage SHOP = loadImage("/shop/shop_item.png", false);
    private static final BufferedImage INVENTORY = loadImage("/inventorychest.png", false);

    private final transient TowerProgressController progress;
    private final transient IntConsumer onEnter;
    private final transient Runnable onBack;
    private final transient Runnable onShop;
    private final transient Runnable onInventory;
    private final int heroFloor;
    private final int climbToFloor;

    /**
     * @param progress source of the scenario and per-floor statuses
     * @param onEnter  invoked with the chosen floor number when the player
     *                 enters an available floor
     */
    public TowerMapWindow(TowerProgressController progress, IntConsumer onEnter, Runnable onBack,
            Runnable onShop, Runnable onInventory, int heroFloor, int climbToFloor) {
        this.progress = progress;
        this.onEnter = onEnter;
        this.onBack = onBack;
        this.onShop = onShop;
        this.onInventory = onInventory;
        this.heroFloor = clampFloor(heroFloor);
        this.climbToFloor = climbToFloor >= 1 && climbToFloor <= FLOOR_COUNT ? climbToFloor : -1;

        setTitle("Dungeon KUrawler - Tower Map");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKDROP);

        TowerPanel towerPanel = new TowerPanel();
        JScrollPane scroll = new JScrollPane(towerPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BACKDROP);
        scroll.setWheelScrollingEnabled(true);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        JButton back = createBackButton();
        JButton shop = createShopButton();
        JButton inventory = createInventoryButton();
        JPanel mapLayer = new JPanel(null) {
            @Override
            public void doLayout() {
                scroll.setBounds(0, 0, getWidth(), getHeight());
                back.setBounds(MAP_BUTTON_MARGIN, MAP_BUTTON_MARGIN, BACK_BUTTON_W, BACK_BUTTON_H);
                int shopX = Math.max(MAP_BUTTON_MARGIN, getWidth() - SHOP_BUTTON_SIZE - MAP_BUTTON_MARGIN);
                int shopY = Math.max(MAP_BUTTON_MARGIN, getHeight() - SHOP_BUTTON_SIZE - MAP_BUTTON_MARGIN);
                shop.setBounds(shopX, shopY, SHOP_BUTTON_SIZE, SHOP_BUTTON_SIZE);
                // Inventory icon sits just to the left of the shop icon.
                inventory.setBounds(Math.max(MAP_BUTTON_MARGIN, shopX - SHOP_BUTTON_SIZE - MAP_BUTTON_MARGIN),
                        shopY, SHOP_BUTTON_SIZE, SHOP_BUTTON_SIZE);
            }
        };
        mapLayer.setBackground(BACKDROP);
        mapLayer.add(scroll);
        mapLayer.add(back);
        mapLayer.add(shop);
        mapLayer.add(inventory);
        mapLayer.setComponentZOrder(back, 0);
        mapLayer.setComponentZOrder(shop, 0);
        mapLayer.setComponentZOrder(inventory, 0);
        root.add(mapLayer, BorderLayout.CENTER);

        setContentPane(root);
        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> {
            int targetY = towerPanel.heroYForScroll();
            int viewH = scroll.getViewport().getExtentSize().height;
            int max = scroll.getVerticalScrollBar().getMaximum();
            int y = Math.max(0, Math.min(max, targetY - viewH / 2));
            scroll.getVerticalScrollBar().setValue(y);
            towerPanel.startClimbIfNeeded();
        });
    }

    private JButton createBackButton() {
        JButton back = new JButton("BACK");
        RetroTheme.styleRetroButton(back, new Color(54, 41, 30));
        back.setFocusable(false);
        back.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            if (onBack != null) {
                onBack.run();
            }
        });
        return back;
    }

    private JButton createShopButton() {
        JButton shop = new JButton();
        shop.setBorder(BorderFactory.createEmptyBorder());
        shop.setContentAreaFilled(false);
        shop.setBorderPainted(false);
        shop.setOpaque(false);
        shop.setFocusable(false);
        shop.setToolTipText("Shop");
        if (SHOP != null) {
            Image scaled = SHOP.getScaledInstance(SHOP_ICON_SIZE, SHOP_ICON_SIZE, Image.SCALE_SMOOTH);
            shop.setIcon(new ImageIcon(scaled));
        } else {
            shop.setText("$");
        }
        shop.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            if (onShop != null) {
                onShop.run();
            }
        });
        return shop;
    }

    private JButton createInventoryButton() {
        JButton inventory = new JButton();
        inventory.setBorder(BorderFactory.createEmptyBorder());
        inventory.setContentAreaFilled(false);
        inventory.setBorderPainted(false);
        inventory.setOpaque(false);
        inventory.setFocusable(false);
        inventory.setToolTipText("Inventory");
        if (INVENTORY != null) {
            Image scaled = INVENTORY.getScaledInstance(SHOP_ICON_SIZE, SHOP_ICON_SIZE, Image.SCALE_SMOOTH);
            inventory.setIcon(new ImageIcon(scaled));
        } else {
            inventory.setText("INV");
        }
        inventory.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            if (onInventory != null) {
                onInventory.run();
            }
        });
        return inventory;
    }

    /** The interactive tower: paints the art, lock seals, and hover/click state. */
    private final class TowerPanel extends JComponent implements Scrollable {

        private Timer climbTimer;
        private int climbTick;
        private int heroFrame;

        TowerPanel() {
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int floor = markerFloorAt(e.getX(), e.getY(), getWidth());
                    if (floor < 1) {
                        return;
                    }
                    if (progress.canEnter(floor)) {
                        AudioManager.shared().play("button_click");
                        onEnter.accept(floor);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int floor = markerFloorAt(e.getX(), e.getY(), getWidth());
                    setCursor(Cursor.getPredefinedCursor(progress.canEnter(floor)
                            ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setCursor(Cursor.getDefaultCursor());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getLayoutWidth();
            return new Dimension(w, getContentArtHeight(w));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return Math.max(60, visibleRect.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            if (TOWER != null) {
                g2.drawImage(TOWER, 0, 0, w, h, null);
            } else {
                g2.setColor(BACKDROP);
                g2.fillRect(0, 0, w, h);
            }

            for (int floor = 1; floor <= FLOOR_COUNT; floor++) {
                int cx = floorCenterX(floor, w);
                int cy = floorCenterY(floor, h);
                if (!progress.canEnter(floor)) {
                    drawLock(g2, cx, cy, w);
                } else if (TOWER == null) {
                    drawFallbackNumber(g2, floor, cx, cy, w);
                }
            }
            drawHero(g2, w, h);
            g2.dispose();
        }

        private void startClimbIfNeeded() {
            if (climbToFloor < 1 || climbToFloor == heroFloor) {
                return;
            }
            climbTimer = new Timer(75, e -> {
                climbTick++;
                heroFrame = (heroFrame + 1) % Math.max(1, SpriteRegistry.heroFrameCount());
                if (climbTick >= CLIMB_TICKS) {
                    climbTick = CLIMB_TICKS;
                    climbTimer.stop();
                }
                repaint();
            });
            climbTimer.start();
        }

        private int heroYForScroll() {
            int w = Math.max(1, getLayoutWidth());
            int h = getContentArtHeight(w);
            return Math.round((float) heroYFrac(heroFloor) * h);
        }

        private void drawHero(Graphics2D g2, int w, int h) {
            BufferedImage hero = SpriteRegistry.heroFrame(heroFrame);
            if (hero == null) {
                return;
            }
            double[] pos = currentHeroPosition();
            int spriteH = Math.max(34, (int) Math.round(w * 0.055));
            int spriteW = Math.max(24, (int) Math.round(spriteH * hero.getWidth() / (double) hero.getHeight()));
            int x = (int) Math.round(pos[0] * w) - spriteW / 2;
            int y = (int) Math.round(pos[1] * h) - spriteH + Math.max(2, spriteH / 10);
            g2.drawImage(hero, x, y, spriteW, spriteH, null);
        }

        private double[] currentHeroPosition() {
            if (climbToFloor < 1 || climbToFloor == heroFloor) {
                return new double[] { heroXFrac(heroFloor), heroYFrac(heroFloor) };
            }
            double t = Math.min(1.0, climbTick / (double) CLIMB_TICKS);
            return interpolatePath(climbPathX(heroFloor), climbPathY(heroFloor), t);
        }
    }

    private void drawLock(Graphics2D g2, int cx, int cy, int w) {
        int d = (int) Math.round(LOCK_DIAM_FRAC * w);
        if (LOCK != null) {
            g2.drawImage(LOCK, cx - d / 2, cy - d / 2, d, d, null);
        } else {
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillOval(cx - d / 2, cy - d / 2, d, d);
            g2.setColor(TITLE_GOLD);
            g2.fillRect(cx - d / 6, cy - d / 8, d / 3, d / 3);
        }
    }

    private void drawFallbackNumber(Graphics2D g2, int floor, int cx, int cy, int w) {
        g2.setColor(TEXT);
        g2.setFont(heading((float) (LOCK_DIAM_FRAC * w * 0.7)));
        String s = String.valueOf(floor);
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, cx - sw / 2, cy + g2.getFontMetrics().getAscent() / 2 - 2);
    }

    /** Vertical center of a floor's number badge in the rendered art. */
    private static int floorCenterX(int floor, int w) {
        if (floor < 1 || floor >= BADGE_X_FRACS.length) {
            return -1;
        }
        return (int) Math.round(BADGE_X_FRACS[floor] * w);
    }

    private static int floorCenterY(int floor, int h) {
        if (floor < 1 || floor >= BADGE_Y_FRACS.length) {
            return -1;
        }
        return (int) Math.round(BADGE_Y_FRACS[floor] * h);
    }

    private static int clampFloor(int floor) {
        return Math.max(1, Math.min(FLOOR_COUNT, floor));
    }

    private static double heroXFrac(int floor) {
        return HERO_X_FRACS[clampFloor(floor)];
    }

    private static double heroYFrac(int floor) {
        return HERO_Y_FRACS[clampFloor(floor)];
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
    }

    private static double[] climbPathX(int floor) {
        if (floor < 1 || floor >= CLIMB_PATH_X_FRACS.length) {
            return new double[] { heroXFrac(floor) };
        }
        return CLIMB_PATH_X_FRACS[floor];
    }

    private static double[] climbPathY(int floor) {
        if (floor < 1 || floor >= CLIMB_PATH_Y_FRACS.length) {
            return new double[] { heroYFrac(floor) };
        }
        return CLIMB_PATH_Y_FRACS[floor];
    }

    private static double[] interpolatePath(double[] xs, double[] ys, double t) {
        if (xs.length == 0 || ys.length == 0) {
            return new double[] { heroXFrac(1), heroYFrac(1) };
        }
        if (xs.length == 1 || ys.length == 1) {
            return new double[] { xs[0], ys[0] };
        }
        double total = 0.0;
        double[] lengths = new double[xs.length - 1];
        for (int i = 0; i < lengths.length; i++) {
            double dx = xs[i + 1] - xs[i];
            double dy = ys[i + 1] - ys[i];
            lengths[i] = Math.sqrt(dx * dx + dy * dy);
            total += lengths[i];
        }
        double target = total * Math.max(0.0, Math.min(1.0, t));
        for (int i = 0; i < lengths.length; i++) {
            if (target <= lengths[i] || i == lengths.length - 1) {
                double local = lengths[i] == 0.0 ? 1.0 : target / lengths[i];
                return new double[] { lerp(xs[i], xs[i + 1], local), lerp(ys[i], ys[i + 1], local) };
            }
            target -= lengths[i];
        }
        int last = xs.length - 1;
        return new double[] { xs[last], ys[last] };
    }

    private int markerFloorAt(int x, int y, int componentWidth) {
        int w = Math.max(1, componentWidth);
        int h = getContentArtHeight(w);
        int artLeft = (int) Math.round(0.035 * w);
        int artRight = (int) Math.round(0.870 * w);
        if (x < artLeft || x > artRight) {
            return -1;
        }
        for (int floor = 1; floor <= FLOOR_COUNT; floor++) {
            int top = (int) Math.round(FLOOR_TOP_FRACS[floor] * h);
            int bottom = (int) Math.round(FLOOR_BOTTOM_FRACS[floor] * h);
            if (y >= top && y <= bottom) {
                return floor;
            }
        }
        return -1;
    }

    private int getLayoutWidth() {
        int parentWidth = getParent() == null ? 0 : getParent().getWidth();
        return Math.max(ART_FALLBACK_WIDTH, parentWidth);
    }

    private int getContentArtHeight(int width) {
        if (TOWER == null) {
            return 1000;
        }
        return Math.round(width * (float) TOWER.getHeight() / TOWER.getWidth());
    }

    private static BufferedImage loadImage(String resource, boolean chromaKeyWhite) {
        try (InputStream in = TowerMapWindow.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            BufferedImage raw = ImageIO.read(in);
            if (raw == null || !chromaKeyWhite) {
                return raw;
            }
            return makeWhiteTransparent(raw);
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * The lock badge ships as opaque RGB on a white field; key out the near-white
     * corners so only the circular seal paints over the tower art.
     */
    private static BufferedImage makeWhiteTransparent(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        double cx = (src.getWidth() - 1) / 2.0;
        double cy = (src.getHeight() - 1) / 2.0;
        double radius = Math.min(src.getWidth(), src.getHeight()) * 0.485;
        double radiusSq = radius * radius;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                double dx = x - cx;
                double dy = y - cy;
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int gg = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int max = Math.max(r, Math.max(gg, b));
                int min = Math.min(r, Math.min(gg, b));
                if (dx * dx + dy * dy > radiusSq
                        || (r > 210 && gg > 210 && b > 210)
                        || (max > 175 && max - min < 32)) {
                    out.setRGB(x, y, 0x00000000);
                } else {
                    out.setRGB(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
                }
            }
        }
        return out;
    }

    private static java.awt.Font heading(float size) {
        java.awt.Font base = RetroTheme.UI_TITLE_FONT != null ? RetroTheme.UI_TITLE_FONT
                : new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, Math.round(size));
        return base.deriveFont(java.awt.Font.BOLD, size);
    }

}
