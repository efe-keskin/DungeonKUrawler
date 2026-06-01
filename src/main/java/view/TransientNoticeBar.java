package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Small retro-styled banner for transient, no-input notices (e.g. "Inventory
 * Full"). Lives in the top control strip next to the PAUSE button rather than
 * over the map, so gameplay feedback never covers the dungeon. Auto-hides after
 * a short delay; messages that do not fit are ellipsized.
 */
public final class TransientNoticeBar extends JPanel implements GameNoticeSink {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 45;
    private static final int VISIBLE_MS = 2300;

    private static final Color STONE_OUTLINE = new Color(5, 5, 9);
    private static final Color BORDER_DARK = new Color(68, 45, 38);
    private static final Color BORDER_LIGHT = new Color(120, 68, 54);
    private static final Color PANEL_FILL = new Color(18, 17, 22);
    private static final Color TITLE = new Color(244, 205, 103);
    private static final Color TEXT = new Color(198, 190, 170);

    private String title;
    private String message;
    private final Timer hideTimer;

    public TransientNoticeBar() {
        setOpaque(false);
        setFocusable(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        hideTimer = new Timer(VISIBLE_MS, e -> clear());
        hideTimer.setRepeats(false);
    }

    /** Shows {@code title}/{@code message} and restarts the auto-hide timer. */
    @Override
    public void showNotice(String title, String message) {
        this.title = title == null ? "Warning" : title;
        this.message = message == null ? "" : message.replaceAll("\\s+", " ").trim();
        hideTimer.restart();
        repaint();
    }

    private void clear() {
        title = null;
        message = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (title == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(STONE_OUTLINE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(BORDER_DARK);
            g2.fillRect(3, 3, w - 6, h - 6);
            g2.setColor(BORDER_LIGHT);
            g2.fillRect(6, 6, w - 12, h - 12);
            g2.setColor(PANEL_FILL);
            g2.fillRect(9, 9, w - 18, h - 18);

            int textX = 16;
            int innerWidth = w - textX - 14;

            g2.setFont(font(12f));
            g2.setColor(TITLE);
            g2.drawString(ellipsize(g2, title, innerWidth), textX, 22);

            if (!message.isBlank()) {
                g2.setFont(font(11f));
                g2.setColor(TEXT);
                g2.drawString(ellipsize(g2, message, innerWidth), textX, 38);
            }
        } finally {
            g2.dispose();
        }
    }

    private static Font font(float size) {
        Font base = RetroTheme.UI_MONO_SMALL;
        if (base == null) {
            base = new Font(Font.MONOSPACED, Font.BOLD, Math.round(size));
        }
        return base.deriveFont(Font.PLAIN, size);
    }

    private static String ellipsize(Graphics2D g2, String text, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        StringBuilder sb = new StringBuilder();
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int cw = fm.charWidth(text.charAt(i));
            if (width + cw + ellipsisWidth > maxWidth) {
                break;
            }
            sb.append(text.charAt(i));
            width += cw;
        }
        return sb.append(ellipsis).toString();
    }
}
