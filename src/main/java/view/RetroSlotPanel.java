package view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

/**
 * Shared retro stone slot tile used by the inventory and pouch views so both
 * render item slots with the same look. Holds its own palette so it is
 * self-contained.
 */
public final class RetroSlotPanel extends JPanel {

    private static final Color STONE_OUTLINE = new Color(5, 5, 9);
    private static final Color STONE_BORDER = new Color(103, 91, 75);
    private static final Color GOLD = new Color(214, 170, 70);
    private static final Color GOLD_BRIGHT = new Color(244, 205, 103);
    private static final Color SLOT_EMPTY = new Color(15, 14, 18);
    private static final Color SLOT_FILLED = new Color(31, 28, 28);
    private static final Color SLOT_HOVER = new Color(53, 43, 31);

    private final boolean filled;
    private final boolean equipped;
    private boolean hovered;

    public RetroSlotPanel(boolean filled, boolean equipped) {
        this.filled = filled;
        this.equipped = equipped;
        setOpaque(false);
        setLayout(null);
    }

    public void setHovered(boolean hovered) {
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
