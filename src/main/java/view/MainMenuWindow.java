package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import engine.BuildModeController;
import engine.GameEngine;
import view.assets.AssetId;
import view.assets.AssetManager;

/**
 * Main menu hub with a simple dungeon-themed backdrop. UC-1 flows unchanged
 * (Start Game → engine + {@link GameWindow}, etc.).
 */
public class MainMenuWindow extends JFrame {

    private static final int PREF_W = 920;
    private static final int PREF_H = 560;

    public MainMenuWindow() {
        setTitle("Dungeon KUrawler — Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        FantasyMenuBackgroundPanel background = new FantasyMenuBackgroundPanel();
        background.setLayout(new BorderLayout());

        javax.swing.JPanel titlePanel = new javax.swing.JPanel();
        titlePanel.setLayout(new javax.swing.BoxLayout(titlePanel, javax.swing.BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(80, 0, 0, 0));

        javax.swing.JLabel titleLabel = new javax.swing.JLabel("DUNGEON KURAWLER");
        titleLabel.setFont(RetroTheme.UI_TITLE_FONT);
        titleLabel.setForeground(new Color(255, 230, 180));
        titleLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        javax.swing.JLabel subLabel = new javax.swing.JLabel("by Stack Overglow");
        subLabel.setFont(RetroTheme.UI_SUBTITLE_FONT);
        subLabel.setForeground(new Color(200, 190, 220));
        subLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        titlePanel.add(titleLabel);
        titlePanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
        titlePanel.add(subLabel);

        background.add(titlePanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        buttonPanel.setOpaque(false);
        // Lower half: bottom weight for composition.
        buttonPanel.setBorder(new EmptyBorder(0, 0, 32, 0));

        JButton start = new JButton("START GAME");
        RetroTheme.styleRetroButton(start, RetroTheme.BTN_PRIMARY);
        start.addActionListener(e -> {
            System.out.println("Game Started");
            GameEngine engine = new GameEngine();
            dispose();
            new GameWindow(engine).setVisible(true);
        });

        JButton load = new JButton("LOAD MAP");
        RetroTheme.styleRetroButton(load, RetroTheme.BTN_SECONDARY);
        load.addActionListener(e -> loadSavedMap());

        JButton build = new JButton("BUILD MAP");
        RetroTheme.styleRetroButton(build, new Color(180, 160, 40));
        build.setForeground(Color.WHITE);
        build.addActionListener(e -> {
            dispose();
            new DesignWindow().setVisible(true);
        });

        JButton help = new JButton();
        ImageIcon helpIcon = AssetManager.get().icon(AssetId.HELP_QUESTION_MARK, 40, 40);
        if (helpIcon != null) {
            help.setIcon(helpIcon);
        } else {
            help.setText("?");
            help.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        }
        RetroTheme.styleRetroButton(help, RetroTheme.BTN_ACCENT);
        help.setBorderPainted(false);
        help.setContentAreaFilled(false);
        help.setOpaque(false);
        help.addActionListener(e -> ItemActionMenuDialog.showNotice(this, "Controls", "Help",
                "Build Mode - Arrow keys or WASD to move.\n"
                        + "The engine handles all rules; UI only forwards input."));

        JButton exit = new JButton("EXIT");
        RetroTheme.styleRetroButton(exit, RetroTheme.BTN_DANGER);
        exit.addActionListener(e -> System.exit(0));

        buttonPanel.add(start);
        buttonPanel.add(load);
        buttonPanel.add(build);
        buttonPanel.add(exit);

        JPanel wrapperPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(buttonPanel);

        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.setOpaque(false);
        southContainer.add(wrapperPanel, BorderLayout.CENTER);

        JPanel leftDummy = new JPanel();
        leftDummy.setOpaque(false);
        leftDummy.setPreferredSize(new Dimension(80, 10)); // Balance center alignment
        southContainer.add(leftDummy, BorderLayout.WEST);

        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setOpaque(false);
        rightWrapper.setBorder(new EmptyBorder(0, 0, 40, 10));
        help.setPreferredSize(new Dimension(40, 40));
        rightWrapper.add(help, BorderLayout.SOUTH);
        southContainer.add(rightWrapper, BorderLayout.EAST);

        background.add(southContainer, BorderLayout.SOUTH);
        setContentPane(background);
        pack();
        setSize(PREF_W, PREF_H);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void loadSavedMap() {
        BuildMapFileDialog.showLoad(this, null).ifPresent(this::openSavedMap);
    }

    private void openSavedMap(Path path) {
        try {
            BuildModeController controller = new BuildModeController();
            controller.loadMap(path);
            GameEngine engine = new GameEngine(controller.getDesignMap());
            dispose();
            new GameWindow(engine).setVisible(true);
        } catch (IOException ex) {
            ItemActionMenuDialog.showNotice(this, "Menu", "Load Failed", ex.getMessage());
        }
    }

    /**
     * Full-bleed background with a minimal gradient and title overlay.
     */
    private static final class FantasyMenuBackgroundPanel extends JPanel {

        private final BufferedImage backgroundImage;

        FantasyMenuBackgroundPanel() {
            setOpaque(true);
            backgroundImage = AssetManager.get().image(AssetId.MAIN_MENU_BACKGROUND);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(PREF_W, PREF_H);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                if (backgroundImage != null) {
                    g2.drawImage(backgroundImage, 0, 0, w, h, null);
                } else {
                    g2.setPaint(new GradientPaint(0, 0, new Color(10, 12, 20), 0, h, new Color(24, 18, 30)));
                    g2.fillRect(0, 0, w, h);
                }

                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRect(0, 0, w, 2);
                g2.fillRect(0, h - 2, w, 2);
            } finally {
                g2.dispose();
            }
        }
    }
}
