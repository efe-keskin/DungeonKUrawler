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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

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
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Themed file picker for build maps.
 *
 * <p>GRASP: this is a view helper only. It chooses a path; actual save/load
 * behavior stays in {@link engine.BuildModeController}.
 */
public final class BuildMapFileDialog {

    private static final String MAP_EXTENSION = "dkmap";
    private static final String MAP_EXTENSION_SUFFIX = "." + MAP_EXTENSION;
    private static final Path MAP_DIRECTORY = Paths.get("maps");

    private static final Color OUTLINE = new Color(5, 5, 9);
    private static final Color BORDER = new Color(103, 91, 75);
    private static final Color HIGHLIGHT = new Color(156, 131, 85);
    private static final Color FILL = new Color(18, 17, 22);
    private static final Color INSET = new Color(28, 25, 27);
    private static final Color GOLD = new Color(214, 170, 70);
    private static final Color GOLD_BRIGHT = new Color(244, 205, 103);
    private static final Color TEXT = new Color(240, 222, 180);
    private static final Color MUTED = new Color(198, 190, 170);
    private static final Color FIELD = new Color(12, 12, 18);

    private BuildMapFileDialog() {
    }

    public static Optional<Path> showSave(Component parent, Path initialPath) {
        return show(parent, Mode.SAVE, initialPath);
    }

    public static Optional<Path> showLoad(Component parent, Path initialPath) {
        return show(parent, Mode.LOAD, initialPath);
    }

    private static Optional<Path> show(Component parent, Mode mode, Path initialPath) {
        Window owner = parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        Selection selection = new Selection();

        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setContentPane(new DialogContent(dialog, selection, mode, initialPath));
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return Optional.ofNullable(selection.path);
    }

    private static Path mapDirectory() {
        return MAP_DIRECTORY.toAbsolutePath().normalize();
    }

    private static Path normalizeMapPath(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.toLowerCase().endsWith(MAP_EXTENSION_SUFFIX)) {
            name = name.substring(0, name.length() - MAP_EXTENSION_SUFFIX.length());
        }
        return mapDirectory().resolve(name + MAP_EXTENSION_SUFFIX);
    }

    private static String mapName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        String fileName = path.getFileName().toString();
        if (fileName.toLowerCase().endsWith(MAP_EXTENSION_SUFFIX)) {
            return fileName.substring(0, fileName.length() - MAP_EXTENSION_SUFFIX.length());
        }
        return fileName;
    }

    private enum Mode {
        SAVE,
        LOAD
    }

    private static final class Selection {
        private Path path;
    }

    private static final class DialogContent extends CardPanel {
        private final JDialog dialog;
        private final Selection selection;
        private final Mode mode;
        private final JTextField nameField = new JTextField();
        private final JLabel message = new JLabel(" ");
        private final DefaultListModel<Path> model = new DefaultListModel<>();
        private final JList<Path> maps = new JList<>(model);

        private DialogContent(JDialog dialog, Selection selection, Mode mode, Path initialPath) {
            this.dialog = dialog;
            this.selection = selection;
            this.mode = mode;

            setLayout(new BorderLayout(0, 16));
            setBorder(new EmptyBorder(28, 30, 28, 30));
            setPreferredSize(new Dimension(470, 430));

            add(header(), BorderLayout.NORTH);
            add(center(initialPath), BorderLayout.CENTER);
            add(actions(), BorderLayout.SOUTH);

            refreshMapList();
            if (initialPath != null) {
                nameField.setText(mapName(initialPath));
                selectPath(initialPath);
            }
            if (mode == Mode.LOAD && model.getSize() > 0 && maps.getSelectedIndex() < 0) {
                maps.setSelectedIndex(0);
            }
        }

        private JComponent header() {
            JPanel header = transparentPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

            JLabel category = new JLabel("BUILD MAP");
            category.setFont(font(RetroTheme.UI_MONO_SMALL, 12f));
            category.setForeground(GOLD);
            category.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel title = new JLabel(mode == Mode.SAVE ? "Save Map" : "Load Map");
            title.setFont(font(RetroTheme.UI_MONO, 22f));
            title.setForeground(TEXT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel detail = new JLabel(mapDirectory().toString());
            detail.setFont(font(RetroTheme.UI_MONO_SMALL, 11f));
            detail.setForeground(MUTED);
            detail.setAlignmentX(Component.LEFT_ALIGNMENT);

            header.add(category);
            header.add(Box.createVerticalStrut(10));
            header.add(title);
            header.add(Box.createVerticalStrut(10));
            header.add(detail);
            return header;
        }

        private JComponent center(Path initialPath) {
            JPanel center = transparentPanel();
            center.setLayout(new BorderLayout(0, 12));

            maps.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            maps.setCellRenderer(new MapCellRenderer());
            maps.setFixedCellHeight(34);
            maps.setOpaque(true);
            maps.setBackground(FIELD);
            maps.setForeground(TEXT);
            maps.setBorder(new EmptyBorder(4, 4, 4, 4));
            maps.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && maps.getSelectedValue() != null) {
                    nameField.setText(mapName(maps.getSelectedValue()));
                    clearMessage();
                }
            });
            maps.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        confirm();
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(maps);
            scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            scroll.getViewport().setBackground(FIELD);
            scroll.setOpaque(false);

            JPanel fieldPanel = transparentPanel();
            fieldPanel.setLayout(new BorderLayout(10, 0));

            JLabel label = new JLabel("NAME");
            label.setFont(font(RetroTheme.UI_MONO_SMALL, 12f));
            label.setForeground(GOLD);

            nameField.setFont(font(RetroTheme.UI_MONO_SMALL, 13f));
            nameField.setForeground(TEXT);
            nameField.setCaretColor(GOLD_BRIGHT);
            nameField.setBackground(FIELD);
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    new EmptyBorder(8, 10, 8, 10)));
            nameField.setText(mode == Mode.SAVE && initialPath == null ? "designed-map" : mapName(initialPath));
            nameField.addActionListener(e -> confirm());

            fieldPanel.add(label, BorderLayout.WEST);
            fieldPanel.add(nameField, BorderLayout.CENTER);

            message.setFont(font(RetroTheme.UI_MONO_SMALL, 11f));
            message.setForeground(new Color(225, 106, 92));

            center.add(scroll, BorderLayout.CENTER);
            center.add(fieldPanel, BorderLayout.NORTH);
            center.add(message, BorderLayout.SOUTH);
            return center;
        }

        private JComponent actions() {
            JPanel row = transparentPanel();
            row.setLayout(new GridLayout(1, 3, 12, 0));

            JButton refresh = new ActionButton("REFRESH", false);
            refresh.addActionListener(e -> refreshMapList());

            JButton cancel = new ActionButton("CANCEL", false);
            cancel.addActionListener(e -> dialog.dispose());

            JButton confirm = new ActionButton(mode == Mode.SAVE ? "SAVE" : "LOAD", true);
            confirm.addActionListener(e -> confirm());

            row.add(refresh);
            row.add(cancel);
            row.add(confirm);
            return row;
        }

        private void refreshMapList() {
            model.clear();
            try {
                Files.createDirectories(mapDirectory());
                try (Stream<Path> paths = Files.list(mapDirectory())) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().toLowerCase().endsWith(MAP_EXTENSION_SUFFIX))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                            .forEach(model::addElement);
                }
                if (model.isEmpty()) {
                    showMessage("No saved maps yet.");
                } else {
                    clearMessage();
                }
            } catch (IOException ex) {
                showMessage("Could not read map folder.");
            }
        }

        private void selectPath(Path path) {
            Path normalized = path.toAbsolutePath().normalize();
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).toAbsolutePath().normalize().equals(normalized)) {
                    maps.setSelectedIndex(i);
                    maps.ensureIndexIsVisible(i);
                    return;
                }
            }
        }

        private void confirm() {
            String name = nameField.getText().trim();
            if (name.isBlank()) {
                showMessage("Enter a map name.");
                return;
            }

            Path path = normalizeMapPath(name);
            if (mode == Mode.LOAD && !Files.exists(path)) {
                showMessage("Choose an existing map.");
                return;
            }

            selection.path = path;
            dialog.dispose();
        }

        private void showMessage(String text) {
            message.setText(text);
        }

        private void clearMessage() {
            message.setText(" ");
        }
    }

    private static class CardPanel extends JPanel {
        CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0, 0, 0, 135));
                g2.fillRect(9, 10, w - 12, h - 13);
                g2.setColor(OUTLINE);
                g2.fillRect(1, 1, w - 8, h - 8);
                g2.setColor(BORDER);
                g2.fillRect(4, 4, w - 14, h - 14);
                g2.setColor(HIGHLIGHT);
                g2.fillRect(7, 7, w - 20, 2);
                g2.fillRect(7, 7, 2, h - 20);
                g2.setColor(new Color(55, 47, 42));
                g2.fillRect(7, h - 15, w - 20, 2);
                g2.fillRect(w - 15, 7, 2, h - 20);
                g2.setColor(FILL);
                g2.fillRect(11, 11, w - 28, h - 28);
                g2.setColor(INSET);
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

    private static final class MapCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText(value instanceof Path path ? mapName(path) : String.valueOf(value));
            label.setFont(font(RetroTheme.UI_MONO_SMALL, 13f));
            label.setForeground(isSelected ? Color.WHITE : TEXT);
            label.setBackground(isSelected ? new Color(92, 61, 28) : FIELD);
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setBorder(new EmptyBorder(8, 10, 8, 10));
            return label;
        }
    }

    private static final class ActionButton extends JButton {
        private ActionButton(String label, boolean primary) {
            super(label);
            RetroTheme.styleRetroButton(this, primary ? new Color(92, 61, 28) : RetroTheme.BTN_SECONDARY);
            setFont(font(RetroTheme.UI_MONO_SMALL, 12f));
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static JPanel transparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static Font font(Font configured, float size) {
        Font base = configured == null ? new Font(Font.MONOSPACED, Font.BOLD, Math.round(size)) : configured;
        return base.deriveFont(Font.PLAIN, size);
    }
}
