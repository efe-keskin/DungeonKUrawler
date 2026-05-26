package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Styled popup surface used for choices and notices throughout the UI.
 * It only returns the selected option; game behavior remains with the caller.
 */
public final class ItemActionMenuDialog {

    private static final Color STONE_OUTLINE = new Color(5, 5, 9);
    private static final Color STONE_BORDER = new Color(103, 91, 75);
    private static final Color STONE_HIGHLIGHT = new Color(156, 131, 85);
    private static final Color CARD_FILL = new Color(18, 17, 22);
    private static final Color CARD_INSET = new Color(28, 25, 27);
    private static final Color GOLD = new Color(214, 170, 70);
    private static final Color GOLD_BRIGHT = new Color(244, 205, 103);
    private static final Color TITLE = new Color(240, 222, 180);
    private static final Color DETAIL = new Color(198, 190, 170);
    private static final Color BADGE = new Color(224, 176, 68);
    private static final int WIDTH = 390;

    private ItemActionMenuDialog() {
    }

    /**
     * Displays the styled action menu and returns the index of the selected
     * label, or {@code -1} when it is closed or dismissed with ESC.
     */
    public static int show(Component parent, String category, String title, String detail, String... labels) {
        Window owner = parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        int[] selection = { -1 };

        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setContentPane(buildCard(dialog, selection, category, title, detail, labels));
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return selection[0];
    }

    public static void showNotice(Component parent, String category, String title, String detail) {
        show(parent, category, title, detail, "Close");
    }

    private static JComponent buildCard(JDialog dialog, int[] selection, String category,
            String title, String detail, String[] labels) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(30, 29, 27, 29));
        card.setPreferredSize(new Dimension(WIDTH, calculateHeight(detail, labels.length)));

        JPanel header = transparentPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel categoryLabel = new JLabel(category == null ? "OBJECT" : category.toUpperCase());
        categoryLabel.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 12f));
        categoryLabel.setForeground(BADGE);
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(font(RetroTheme.UI_MONO, Font.PLAIN, 20f));
        titleLabel.setForeground(TITLE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(categoryLabel);
        header.add(Box.createVerticalStrut(13));
        header.add(titleLabel);

        JTextArea detailText = new JTextArea(detail == null ? "" : detail);
        detailText.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 12f));
        detailText.setForeground(DETAIL);
        detailText.setOpaque(false);
        detailText.setEditable(false);
        detailText.setFocusable(false);
        detailText.setLineWrap(true);
        detailText.setWrapStyleWord(true);
        detailText.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
        detailText.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(Box.createVerticalStrut(14));
        header.add(detailText);
        card.add(header, BorderLayout.CENTER);

        JPanel buttonRow = transparentPanel();
        buttonRow.setLayout(new GridLayout(1, Math.max(1, labels.length), 12, 0));
        for (int i = 0; i < labels.length; i++) {
            final int optionIndex = i;
            ActionButton button = new ActionButton(labels[i], i == 0);
            button.addActionListener(e -> {
                selection[0] = optionIndex;
                dialog.dispose();
            });
            buttonRow.add(button);
        }
        card.add(buttonRow, BorderLayout.SOUTH);
        return card;
    }

    private static JPanel transparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static int calculateHeight(String detail, int optionCount) {
        int detailLines = 0;
        if (detail != null && !detail.isBlank()) {
            for (String line : detail.split("\n", -1)) {
                detailLines += Math.max(1, (line.length() + 43) / 44);
            }
        }
        return 224 + Math.min(3, Math.max(0, detailLines - 1)) * 16
                + (optionCount > 2 ? 4 : 0);
    }

    private static Font font(Font configured, int style, float size) {
        Font base = configured == null ? new Font(Font.SANS_SERIF, style, Math.round(size)) : configured;
        return base.deriveFont(style, size);
    }

    private static final class CardPanel extends JPanel {
        CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
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
                g2.setColor(CARD_FILL);
                g2.fillRect(11, 11, w - 28, h - 28);
                g2.setColor(CARD_INSET);
                g2.fillRect(17, 17, w - 40, h - 40);
                g2.setColor(GOLD);
                g2.fillRect(28, 23, 54, 3);
                g2.fillRect(w - 94, 23, 54, 3);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static final class ActionButton extends JButton {
        private final boolean primary;
        private boolean hovered;

        ActionButton(String label, boolean primary) {
            super(label.toUpperCase());
            this.primary = primary;
            setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 12f));
            setForeground(TITLE);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(13, 10, 13, 10));
            setPreferredSize(new Dimension(90, 45));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                Color background = primary
                        ? (hovered ? new Color(126, 84, 31) : new Color(92, 61, 28))
                        : (hovered ? new Color(65, 55, 47) : new Color(43, 38, 37));
                Color border = primary
                        ? (hovered ? GOLD_BRIGHT : GOLD)
                        : (hovered ? STONE_HIGHLIGHT : STONE_BORDER);
                g2.setColor(STONE_OUTLINE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(background);
                g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
                g2.setColor(border);
                g2.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
                g2.setColor(hovered ? GOLD_BRIGHT : new Color(155, 122, 62));
                g2.fillRect(5, 5, getWidth() - 10, 2);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
