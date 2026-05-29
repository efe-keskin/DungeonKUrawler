package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntConsumer;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import engine.TowerProgressController;
import engine.audio.AudioManager;

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
    private static final Color MUTED = new Color(140, 134, 124);
    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;
    private static final int FLOOR_COUNT = 10;

    // --- Tower geometry, expressed as fractions of the rendered art so the
    //     overlays scale with the panel. Tuned against scenario_levels.png,
    //     whose number badges run down the left edge (floor 10 top to 1 bottom).
    private static final double BADGE_X_FRAC = 0.096;   // badge center, x
    private static final double BADGE_TOP_FRAC = 0.114; // floor 10 center, y
    private static final double BADGE_BOT_FRAC = 0.910; // floor 1 center, y
    private static final double LOCK_DIAM_FRAC = 0.065; // lock badge diameter

    /** Fallback width before the scroll viewport has completed layout. */
    private static final int ART_FALLBACK_WIDTH = 880;

    private static final BufferedImage TOWER = loadImage("/scenario_levels.png", false);
    private static final BufferedImage LOCK = loadImage("/level_locked_indicator.png", true);

    private final transient TowerProgressController progress;
    private final transient IntConsumer onEnter;
    private final transient Runnable onBack;

    /**
     * @param progress source of the scenario and per-floor statuses
     * @param onEnter  invoked with the chosen floor number when the player
     *                 enters an available floor
     */
    public TowerMapWindow(TowerProgressController progress, IntConsumer onEnter, Runnable onBack) {
        this.progress = progress;
        this.onEnter = onEnter;
        this.onBack = onBack;

        setTitle("Dungeon KUrawler - Tower Map");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKDROP);
        root.add(new HeaderPanel(), BorderLayout.NORTH);

        TowerPanel towerPanel = new TowerPanel();
        JScrollPane scroll = new JScrollPane(towerPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BACKDROP);
        scroll.setWheelScrollingEnabled(true);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        root.add(scroll, BorderLayout.CENTER);

        setContentPane(root);
        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
    }

    /** Small header block: title + a one-line legend. */
    private final class HeaderPanel extends JPanel {
        HeaderPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(18, 20, 14, 20));

            JPanel titleStack = new JPanel();
            titleStack.setOpaque(false);
            titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("THE TOWER");
            title.setFont(heading(26f));
            title.setForeground(TITLE_GOLD);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel hint = new JLabel("Clear a floor to unlock the next. Locked floors show a seal.");
            hint.setFont(body(12f));
            hint.setForeground(MUTED);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);

            titleStack.add(title);
            titleStack.add(Box.createVerticalStrut(6));
            titleStack.add(hint);
            add(titleStack, BorderLayout.WEST);

            JButton back = new JButton("BACK");
            RetroTheme.styleRetroButton(back, new Color(54, 41, 30));
            back.setFocusable(false);
            back.addActionListener(e -> {
                AudioManager.shared().play("button_click");
                if (onBack != null) {
                    onBack.run();
                }
            });
            add(back, BorderLayout.EAST);
        }
    }

    /** The interactive tower: paints the art, lock seals, and hover/click state. */
    private final class TowerPanel extends JComponent implements Scrollable {

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
                int cx = (int) Math.round(BADGE_X_FRAC * w);
                int cy = floorCenterY(floor, h);
                if (!progress.canEnter(floor)) {
                    drawLock(g2, cx, cy, w);
                } else if (TOWER == null) {
                    drawFallbackNumber(g2, floor, cx, cy, w);
                }
            }
            g2.dispose();
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
    private static int floorCenterY(int floor, int h) {
        double top = BADGE_TOP_FRAC * h;
        double bot = BADGE_BOT_FRAC * h;
        int i = FLOOR_COUNT - floor; // 0 for floor 10 (top), 9 for floor 1
        return (int) Math.round(top + i * (bot - top) / (FLOOR_COUNT - 1));
    }

    /** The floor whose band contains the given y in the rendered art. */
    private int floorAt(int y) {
        int h = Math.max(1, getContentArtHeight(getWidth()));
        double top = BADGE_TOP_FRAC * h;
        double step = (BADGE_BOT_FRAC * h - top) / (FLOOR_COUNT - 1);
        int i = (int) Math.round((y - top) / step);
        i = Math.max(0, Math.min(FLOOR_COUNT - 1, i));
        return FLOOR_COUNT - i;
    }

    private int markerFloorAt(int x, int y, int componentWidth) {
        int w = Math.max(1, componentWidth);
        int floor = floorAt(y);
        int cx = (int) Math.round(BADGE_X_FRAC * w);
        int cy = floorCenterY(floor, getContentArtHeight(w));
        int radius = Math.max(24, (int) Math.round(LOCK_DIAM_FRAC * w * 0.7));
        int dx = x - cx;
        int dy = y - cy;
        return dx * dx + dy * dy <= radius * radius ? floor : -1;
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

    private static java.awt.Font body(float size) {
        java.awt.Font base = RetroTheme.UI_MONO_SMALL != null ? RetroTheme.UI_MONO_SMALL
                : new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, Math.round(size));
        return base.deriveFont(java.awt.Font.PLAIN, size);
    }
}
