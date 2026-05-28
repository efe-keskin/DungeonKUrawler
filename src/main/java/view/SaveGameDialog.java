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
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Retro-styled save prompt with explicit save, save-and-exit, and cancel paths.
 */
public final class SaveGameDialog {

    private static final Color STONE_OUTLINE = new Color(5, 5, 9);
    private static final Color STONE_BORDER = new Color(103, 91, 75);
    private static final Color STONE_HIGHLIGHT = new Color(156, 131, 85);
    private static final Color CARD_FILL = new Color(18, 17, 22);
    private static final Color CARD_INSET = new Color(28, 25, 27);
    private static final Color GOLD = new Color(214, 170, 70);
    private static final Color GOLD_BRIGHT = new Color(244, 205, 103);
    private static final Color TITLE = new Color(240, 222, 180);
    private static final Color DETAIL = new Color(198, 190, 170);
    private static final Color ERROR = new Color(235, 110, 95);
    private static final int WIDTH = 560;
    private static final int HEIGHT = 286;

    private SaveGameDialog() {
    }

    public enum Action {
        SAVE_AND_EXIT,
        SAVE,
        CANCEL
    }

    public record Result(Action action, String saveName) {
        public static Result cancelled() {
            return new Result(Action.CANCEL, null);
        }
    }

    public static Result show(Component parent) {
        Window owner = parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        Result[] result = { Result.cancelled() };

        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setContentPane(buildCard(dialog, result));
        dialog.getRootPane().registerKeyboardAction(e -> {
            result[0] = Result.cancelled();
            dialog.dispose();
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result[0];
    }

    private static JComponent buildCard(JDialog dialog, Result[] result) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(30, 32, 28, 32));
        card.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        JPanel content = transparentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel categoryLabel = new JLabel("SAVE");
        categoryLabel.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 12f));
        categoryLabel.setForeground(GOLD);
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("Save Game");
        titleLabel.setFont(font(RetroTheme.UI_MONO, Font.PLAIN, 22f));
        titleLabel.setForeground(TITLE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel promptLabel = new JLabel("Name this save file");
        promptLabel.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 13f));
        promptLabel.setForeground(DETAIL);
        promptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nameField = new JTextField();
        nameField.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 14f));
        nameField.setForeground(TITLE);
        nameField.setCaretColor(GOLD_BRIGHT);
        nameField.setBackground(new Color(10, 10, 14));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STONE_HIGHLIGHT, 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 11f));
        errorLabel.setForeground(ERROR);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(categoryLabel);
        content.add(Box.createVerticalStrut(11));
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(18));
        content.add(promptLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(nameField);
        content.add(Box.createVerticalStrut(6));
        content.add(errorLabel);
        card.add(content, BorderLayout.CENTER);

        JPanel buttonRow = transparentPanel();
        buttonRow.setLayout(new GridLayout(1, 3, 12, 0));

        Consumer<Action> submit = action -> {
            String saveName = nameField.getText() == null ? "" : nameField.getText().trim();
            if (saveName.isBlank()) {
                errorLabel.setText("Please enter a save name.");
                nameField.requestFocusInWindow();
                return;
            }
            result[0] = new Result(action, saveName);
            dialog.dispose();
        };

        ActionButton saveAndExit = new ActionButton("Save and Exit", ButtonTone.ACCENT);
        saveAndExit.addActionListener(e -> submit.accept(Action.SAVE_AND_EXIT));

        ActionButton save = new ActionButton("Save", ButtonTone.PRIMARY);
        save.addActionListener(e -> submit.accept(Action.SAVE));

        ActionButton cancel = new ActionButton("Cancel", ButtonTone.SECONDARY);
        cancel.addActionListener(e -> {
            result[0] = Result.cancelled();
            dialog.dispose();
        });

        buttonRow.add(saveAndExit);
        buttonRow.add(save);
        buttonRow.add(cancel);
        card.add(buttonRow, BorderLayout.SOUTH);

        nameField.addActionListener(e -> submit.accept(Action.SAVE));
        dialog.getRootPane().setDefaultButton(save);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        return card;
    }

    private static JPanel transparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static Font font(Font configured, int style, float size) {
        Font base = configured == null ? new Font(Font.SANS_SERIF, style, Math.round(size)) : configured;
        return base.deriveFont(style, size);
    }

    private enum ButtonTone {
        PRIMARY,
        ACCENT,
        SECONDARY
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
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRect(8, 9, w - 12, h - 13);
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
                g2.fillRect(30, 24, 58, 3);
                g2.fillRect(w - 100, 24, 58, 3);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static final class ActionButton extends JButton {
        private final ButtonTone tone;
        private boolean hovered;

        ActionButton(String label, ButtonTone tone) {
            super(label);
            this.tone = tone;
            setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 11f));
            setForeground(TITLE);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(12, 8, 12, 8));
            setPreferredSize(new Dimension(150, 46));
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
                Color background = backgroundColor();
                Color border = borderColor();
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

        private Color backgroundColor() {
            return switch (tone) {
                case PRIMARY -> hovered ? new Color(126, 84, 31) : new Color(92, 61, 28);
                case ACCENT -> hovered ? new Color(93, 68, 93) : new Color(64, 49, 74);
                case SECONDARY -> hovered ? new Color(65, 55, 47) : new Color(43, 38, 37);
            };
        }

        private Color borderColor() {
            return switch (tone) {
                case PRIMARY -> hovered ? GOLD_BRIGHT : GOLD;
                case ACCENT -> hovered ? new Color(202, 160, 232) : new Color(143, 115, 178);
                case SECONDARY -> hovered ? STONE_HIGHLIGHT : STONE_BORDER;
            };
        }
    }
}
