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
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import save.SaveDtos.SaveDescriptor;

/**
 * Retro-styled load prompt with continue, delete, and cancel choices.
 */
public final class LoadGameDialog {

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
    private static final int WIDTH = 590;
    private static final int HEIGHT = 372;

    private LoadGameDialog() {
    }

    public enum Action {
        CONTINUE,
        DELETE,
        CANCEL
    }

    public record Result(Action action, SaveDescriptor save) {
        public static Result cancelled() {
            return new Result(Action.CANCEL, null);
        }
    }

    public static Result show(Component parent, List<SaveDescriptor> saves) {
        Window owner = parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        Result[] result = { Result.cancelled() };

        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setContentPane(buildCard(dialog, result, saves));
        dialog.getRootPane().registerKeyboardAction(e -> {
            result[0] = Result.cancelled();
            dialog.dispose();
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result[0];
    }

    private static JComponent buildCard(JDialog dialog, Result[] result, List<SaveDescriptor> saves) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(30, 32, 28, 32));
        card.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        JPanel content = transparentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel categoryLabel = new JLabel("LOAD");
        categoryLabel.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 12f));
        categoryLabel.setForeground(GOLD);
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("Load Game");
        titleLabel.setFont(font(RetroTheme.UI_MONO, Font.PLAIN, 22f));
        titleLabel.setForeground(TITLE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel promptLabel = new JLabel("Choose a saved game");
        promptLabel.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 13f));
        promptLabel.setForeground(DETAIL);
        promptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JList<SaveDescriptor> saveList = new JList<>(listModel(saves));
        saveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        saveList.setVisibleRowCount(Math.min(8, Math.max(1, saves.size())));
        saveList.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 12f));
        saveList.setFixedCellHeight(32);
        saveList.setBackground(new Color(10, 10, 14));
        saveList.setForeground(TITLE);
        saveList.setSelectionBackground(new Color(92, 61, 28));
        saveList.setSelectionForeground(TITLE);
        saveList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        saveList.setCellRenderer(new SaveCellRenderer());
        if (!saves.isEmpty()) {
            saveList.setSelectedIndex(0);
        }

        JScrollPane scrollPane = new JScrollPane(saveList);
        scrollPane.setBorder(BorderFactory.createLineBorder(STONE_HIGHLIGHT, 2));
        scrollPane.setBackground(new Color(10, 10, 14));
        scrollPane.getViewport().setBackground(new Color(10, 10, 14));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUI(new RetroScrollBarUI());
        verticalBar.setPreferredSize(new Dimension(15, 0));
        verticalBar.setUnitIncrement(16);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 166));

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
        content.add(scrollPane);
        content.add(Box.createVerticalStrut(6));
        content.add(errorLabel);
        card.add(content, BorderLayout.CENTER);

        JPanel buttonRow = transparentPanel();
        buttonRow.setLayout(new GridLayout(1, 3, 12, 0));

        Consumer<Action> submit = action -> {
            SaveDescriptor selected = saveList.getSelectedValue();
            if (selected == null) {
                errorLabel.setText("Please choose a saved game.");
                return;
            }
            result[0] = new Result(action, selected);
            dialog.dispose();
        };

        ActionButton continueButton = new ActionButton("Continue", ButtonTone.PRIMARY);
        continueButton.addActionListener(e -> submit.accept(Action.CONTINUE));

        ActionButton deleteButton = new ActionButton("Delete", ButtonTone.DANGER);
        deleteButton.addActionListener(e -> submit.accept(Action.DELETE));

        ActionButton cancelButton = new ActionButton("Cancel", ButtonTone.SECONDARY);
        cancelButton.addActionListener(e -> {
            result[0] = Result.cancelled();
            dialog.dispose();
        });

        buttonRow.add(continueButton);
        buttonRow.add(deleteButton);
        buttonRow.add(cancelButton);
        card.add(buttonRow, BorderLayout.SOUTH);

        saveList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && saveList.getSelectedValue() != null) {
                    submit.accept(Action.CONTINUE);
                }
            }
        });
        dialog.getRootPane().setDefaultButton(continueButton);
        SwingUtilities.invokeLater(saveList::requestFocusInWindow);
        return card;
    }

    private static DefaultListModel<SaveDescriptor> listModel(List<SaveDescriptor> saves) {
        DefaultListModel<SaveDescriptor> model = new DefaultListModel<>();
        if (saves != null) {
            for (SaveDescriptor save : saves) {
                model.addElement(save);
            }
        }
        return model;
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
        DANGER,
        SECONDARY
    }

    private static final class SaveCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
            SaveDescriptor save = value instanceof SaveDescriptor descriptor ? descriptor : null;
            label.setText(save == null ? "" : save.getDisplayLabel());
            label.setFont(font(RetroTheme.UI_MONO_SMALL, Font.PLAIN, 12f));
            label.setBorder(new EmptyBorder(7, 8, 7, 8));
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(new Color(92, 61, 28));
                label.setForeground(TITLE);
            } else {
                label.setBackground(index % 2 == 0 ? new Color(18, 17, 22) : new Color(24, 21, 24));
                label.setForeground(DETAIL);
            }
            return label;
        }
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
                g2.setColor(STONE_OUTLINE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(backgroundColor());
                g2.fillRect(3, 3, getWidth() - 6, getHeight() - 6);
                g2.setColor(borderColor());
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
                case DANGER -> hovered ? new Color(128, 54, 54) : new Color(92, 43, 43);
                case SECONDARY -> hovered ? new Color(65, 55, 47) : new Color(43, 38, 37);
            };
        }

        private Color borderColor() {
            return switch (tone) {
                case PRIMARY -> hovered ? GOLD_BRIGHT : GOLD;
                case DANGER -> hovered ? new Color(240, 125, 108) : new Color(184, 83, 76);
                case SECONDARY -> hovered ? STONE_HIGHLIGHT : STONE_BORDER;
            };
        }
    }

    private static final class RetroScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return invisibleButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return invisibleButton();
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, java.awt.Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setColor(STONE_OUTLINE);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.setColor(new Color(18, 17, 22));
                g2.fillRect(trackBounds.x + 2, trackBounds.y, trackBounds.width - 4, trackBounds.height);
                g2.setColor(new Color(55, 47, 42));
                g2.drawLine(trackBounds.x + 1, trackBounds.y,
                        trackBounds.x + 1, trackBounds.y + trackBounds.height);
                g2.drawLine(trackBounds.x + trackBounds.width - 2, trackBounds.y,
                        trackBounds.x + trackBounds.width - 2, trackBounds.y + trackBounds.height);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, java.awt.Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int x = thumbBounds.x + 2;
                int y = thumbBounds.y + 2;
                int w = Math.max(1, thumbBounds.width - 4);
                int h = Math.max(1, thumbBounds.height - 4);
                g2.setColor(STONE_OUTLINE);
                g2.fillRect(x, y, w, h);
                g2.setColor(isDragging ? new Color(126, 84, 31) : new Color(92, 61, 28));
                g2.fillRect(x + 2, y + 2, w - 4, h - 4);
                g2.setColor(isDragging ? GOLD_BRIGHT : GOLD);
                g2.drawRect(x + 1, y + 1, w - 3, h - 3);
                if (h >= 22) {
                    int gripY = y + h / 2 - 4;
                    g2.setColor(new Color(244, 205, 103, 190));
                    g2.drawLine(x + 4, gripY, x + w - 5, gripY);
                    g2.drawLine(x + 4, gripY + 4, x + w - 5, gripY + 4);
                    g2.drawLine(x + 4, gripY + 8, x + w - 5, gripY + 8);
                }
            } finally {
                g2.dispose();
            }
        }

        private static JButton invisibleButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }
}
