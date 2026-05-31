package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import engine.BuildModeController;
import engine.GameEngine;
import engine.TowerProgressController;
import engine.audio.AudioManager;
import save.SaveDtos.SaveDescriptor;
import save.SaveGameController;
import save.SaveGameException;
import save.SaveGameType;
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
        setTitle("Dungeon KUrawler - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getRootPane().putClientProperty("apple.awt.fullscreenable", Boolean.TRUE);

        FantasyMenuBackgroundPanel background = new FantasyMenuBackgroundPanel();
        background.setLayout(new BorderLayout());
        AudioManager audio = AudioManager.shared();
        audio.startMenuMusic();

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

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.setOpaque(false);
        JPanel leftTitleSpacer = new JPanel();
        leftTitleSpacer.setOpaque(false);
        leftTitleSpacer.setPreferredSize(new Dimension(80, 56));
        northContainer.add(leftTitleSpacer, BorderLayout.WEST);
        northContainer.add(titlePanel, BorderLayout.CENTER);
        northContainer.add(createMusicControl(audio), BorderLayout.EAST);
        background.add(northContainer, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        buttonPanel.setOpaque(false);
        // Lower half: bottom weight for composition.
        buttonPanel.setBorder(new EmptyBorder(0, 0, 32, 0));

        JButton start = new JButton("START GAME");
        RetroTheme.styleRetroButton(start, RetroTheme.BTN_PRIMARY);
        start.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            handleStartGame();
        });

        JButton load = new JButton("LOAD CUSTOM GAME");
        RetroTheme.styleRetroButton(load, new Color(118, 72, 142));
        load.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            handleLoadCustomGame();
        });

        JButton loadMap = new JButton("LOAD MAP");
        RetroTheme.styleRetroButton(loadMap, RetroTheme.BTN_SECONDARY);
        loadMap.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            loadSavedMap();
        });

        JButton build = new JButton("BUILD MAP");
        RetroTheme.styleRetroButton(build, new Color(180, 160, 40));
        build.setForeground(Color.WHITE);
        build.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            AudioManager.shared().stopMenuMusic();
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
        help.addActionListener(e -> {
            AudioManager.shared().play("button_click");

            new HelpDialog(this).setVisible(true);


        });

        JButton exit = new JButton("EXIT");
        RetroTheme.styleRetroButton(exit, RetroTheme.BTN_DANGER);
        exit.addActionListener(e -> {
            AudioManager.shared().play("button_click");
            AudioManager.shared().stopMenuMusic();
            System.exit(0);
        });

        buttonPanel.add(start);
        buttonPanel.add(load);
        buttonPanel.add(loadMap);
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
        FullscreenSupport.install(this);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private JPanel createMusicControl(AudioManager audio) {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 0, 0, 18));
        panel.setPreferredSize(new Dimension(80, 56));
        if (!audio.isMusicAvailable()) {
            return panel;
        }

        BufferedImage onIcon = AssetManager.get().image(AssetId.AUDIO_ON);
        BufferedImage offIcon = AssetManager.get().image(AssetId.AUDIO_OFF);

        JButton muteButton = new JButton();
        muteButton.setFocusable(false);
        muteButton.setBorderPainted(false);
        muteButton.setContentAreaFilled(false);
        muteButton.setMargin(new Insets(4, 8, 4, 8));
        muteButton.setToolTipText("Toggle menu music");

        Runnable refreshIcon = () -> {
            BufferedImage src = audio.isMusicMuted() ? offIcon : onIcon;
            if (src != null) {
                muteButton.setIcon(new ImageIcon(src));
                muteButton.setText("");
            } else {
                // Graceful fallback when icons fail to load.
                muteButton.setIcon(null);
                muteButton.setText(audio.isMusicMuted() ? "MUTE" : "SND");
                muteButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
            }
        };
        refreshIcon.run();

        muteButton.addActionListener(e -> {
            audio.toggleMusicMute();
            refreshIcon.run();
        });
        panel.add(muteButton);
        return panel;
    }

    private void loadSavedMap() {
        BuildMapFileDialog.showLoad(this, null).ifPresent(this::openSavedMap);
    }

    private void openSavedMap(Path path) {
        try {
            BuildModeController controller = new BuildModeController();
            controller.loadMap(path);
            GameEngine engine = new GameEngine(controller.getDesignMap());
            AudioManager.shared().stopMenuMusic();
            dispose();
            new GameWindow(engine).setVisible(true);
        } catch (IOException ex) {
            ItemActionMenuDialog.showNotice(this, "Menu", "Load Failed", ex.getMessage());
        }
    }

    /**
     * UC-T1: Start Game owns the scenario flow. A saved scenario checkpoint
     * resumes directly in its floor; New Game opens the tower map with previous
     * unlocks preserved.
     */
    private void handleStartGame() {
        SaveGameController saveController = new SaveGameController();
        List<SaveDescriptor> checkpoints;
        List<SaveDescriptor> scenarioSaves;
        try {
            checkpoints = saveController.listSaves(SaveGameType.SCENARIO_CHECKPOINT);
            scenarioSaves = saveController.listSaves().stream()
                    .filter(save -> save.getSaveType().isScenario())
                    .toList();
        } catch (SaveGameException ex) {
            ItemActionMenuDialog.showNotice(this, "Start Game", "Load Failed",
                    "Saved games could not be read.");
            return;
        }

        TowerProgressController progress = new TowerProgressController(saveController);

        if (checkpoints.isEmpty()) {
            if (!scenarioSaves.isEmpty()) {
                try {
                    loadBestScenarioProgress(progress, scenarioSaves);
                    openTowerMap(progress);
                } catch (SaveGameException ex) {
                    ItemActionMenuDialog.showNotice(this, "Start Game", "Load Failed",
                            "This save file could not be loaded.");
                }
                return;
            }
            progress.startNewRun();
            openTowerMap(progress);
            return;
        }

        while (true) {
            LoadGameDialog.Result result = LoadGameDialog.showForStartGame(this, checkpoints);
            if (result.action() == LoadGameDialog.Action.CANCEL) {
                return;
            }
            try {
                if (result.action() == LoadGameDialog.Action.NEW_GAME) {
                    loadBestScenarioProgress(progress, scenarioSaves);
                    openTowerMap(progress);
                    return;
                }
                if (result.action() == LoadGameDialog.Action.DELETE) {
                    saveController.deleteSave(result.save());
                    checkpoints = saveController.listSaves(SaveGameType.SCENARIO_CHECKPOINT);
                    scenarioSaves = saveController.listSaves().stream()
                            .filter(save -> save.getSaveType().isScenario())
                            .toList();
                    if (checkpoints.isEmpty()) {
                        if (scenarioSaves.isEmpty()) {
                            progress.startNewRun();
                        } else {
                            loadBestScenarioProgress(progress, scenarioSaves);
                        }
                        openTowerMap(progress);
                        return;
                    }
                    continue;
                }
                progress.loadFromSave(result.save());
                openScenarioCheckpoint(progress);
                return;
            } catch (SaveGameException ex) {
                ItemActionMenuDialog.showNotice(this, "Start Game", "Load Failed",
                        "This save file could not be loaded.");
            }
        }
    }

    private void loadBestScenarioProgress(TowerProgressController progress, List<SaveDescriptor> saves)
            throws SaveGameException {
        SaveDescriptor baseline = saves.stream()
                .max(Comparator
                        .comparingInt(SaveDescriptor::getHighestUnlockedLevel)
                        .thenComparing(SaveDescriptor::getSavedAt,
                                Comparator.nullsLast(String::compareTo)))
                .orElse(null);
        if (baseline == null) {
            progress.startNewRun();
        } else {
            progress.loadFromSave(baseline);
        }
    }

    private void openTowerMap(TowerProgressController progress) {
        AudioManager.shared().stopMenuMusic();
        dispose();
        TowerSessionController.startFrom(progress);
    }

    private void openScenarioCheckpoint(TowerProgressController progress) {
        AudioManager.shared().stopMenuMusic();
        dispose();
        TowerSessionController.resumeFromCheckpoint(progress);
    }

    private void handleLoadCustomGame() {
        SaveGameController controller = new SaveGameController();
        while (true) {
            List<SaveDescriptor> saves;
            try {
                saves = controller.listSaves(SaveGameType.CUSTOM_GAME);
            } catch (SaveGameException ex) {
                ItemActionMenuDialog.showNotice(this, "Load Custom Game", "Load Failed",
                        "This save file could not be loaded.");
                return;
            }
            if (saves.isEmpty()) {
                ItemActionMenuDialog.showNotice(this, "Load Custom Game", "No Saves",
                        "No custom game saves found.");
                return;
            }

            LoadGameDialog.Result result = LoadGameDialog.show(this, saves,
                    "Load Custom Game", "Choose a saved custom game", false);
            if (result.action() == LoadGameDialog.Action.CANCEL) {
                return;
            }

            try {
                if (result.action() == LoadGameDialog.Action.DELETE) {
                    controller.deleteSave(result.save());
                    ItemActionMenuDialog.showNotice(this, "Load Custom Game", "Deleted",
                            "Saved game deleted.");
                    continue;
                }
                GameEngine engine = controller.loadGame(result.save());
                AudioManager.shared().stopMenuMusic();
                dispose();
                new GameWindow(engine).setVisible(true);
                return;
            } catch (SaveGameException ex) {
                ItemActionMenuDialog.showNotice(this, "Load Custom Game", "Load Failed",
                        "This save file could not be loaded.");
            }
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
